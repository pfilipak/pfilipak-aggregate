/*
 * Copyright (C) 2012-2013 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.aggregate.odktables.impl.api;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.resteasy.annotations.GZIP;
import org.opendatakit.aggregate.constants.ErrorConsts;
import org.opendatakit.aggregate.odktables.api.InstanceFileService;
import org.opendatakit.aggregate.odktables.api.TableService;
import org.opendatakit.aggregate.odktables.exception.PermissionDeniedException;
import org.opendatakit.aggregate.odktables.relation.DbTableInstanceFiles;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifest;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifestEntry;
import org.opendatakit.aggregate.odktables.rest.entity.TableRole.TablePermission;
import org.opendatakit.aggregate.odktables.security.TablesUserPermissions;
import org.opendatakit.common.datamodel.BinaryContentManipulator.BlobSubmissionOutcome;
import org.opendatakit.common.ermodel.BlobEntitySet;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;
import org.opendatakit.common.web.constants.HtmlConsts;

public class InstanceFileServiceImpl implements InstanceFileService {

  private static final Log LOGGER = LogFactory.getLog(InstanceFileServiceImpl.class);

  /**
   * String to stand in for those things in the app's root directory.
   *
   * NOTE: This cannot be null -- GAE doesn't like that!
   */
  public static final String NO_TABLE_ID = "";

  private static final String ERROR_FILE_VERSION_DIFFERS = "File on server does not match file being uploaded. Aborting upload. ";

  /**
   * The name of the folder that contains the files associated with a table in
   * an app.
   *
   * @see #getTableIdFromPathSegments(List)
   */
  private final CallingContext cc;
  private final TablesUserPermissions userPermissions;
  private final UriInfo info;
  private final String appId;
  private final String tableId;
  private final String schemaETag;

  public InstanceFileServiceImpl(String appId, String tableId, String schemaETag, UriInfo info,
      TablesUserPermissions userPermissions, CallingContext cc) throws ODKEntityNotFoundException,
      ODKDatastoreException {
    this.cc = cc;
    this.appId = appId;
    this.tableId = tableId;
    this.schemaETag = schemaETag;
    this.info = info;
    this.userPermissions = userPermissions;
  }

  @Override
  @GET
  @Path("{rowId}/manifest")
  @Produces({ MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8 })
  @GZIP
  public Response getManifest(@PathParam("rowId") String rowId,
      @QueryParam(PARAM_AS_ATTACHMENT) String asAttachment) throws IOException {

    UriBuilder ub = info.getBaseUriBuilder();
    ub.path(TableService.class);
    UriBuilder full = ub.clone().path(TableService.class, "getInstanceFiles").path(InstanceFileService.class, "getManifest");
    URI self = full.build(appId, tableId, schemaETag, rowId);
    String manifestUrl = self.toURL().toExternalForm();

    try {
      userPermissions.checkPermission(appId, tableId, TablePermission.READ_ROW);

      ArrayList<OdkTablesFileManifestEntry> manifestEntries = new ArrayList<OdkTablesFileManifestEntry>();
      DbTableInstanceFiles blobStore = new DbTableInstanceFiles(tableId, cc);
      BlobEntitySet instance = blobStore.getBlobEntitySet(rowId, cc);

      int count = instance.getAttachmentCount(cc);
      for ( int i = 1 ; i <= count ; ++i ) {
        OdkTablesFileManifestEntry entry = new OdkTablesFileManifestEntry();
        entry.filename = instance.getUnrootedFilename(i, cc);
        entry.contentLength = instance.getContentLength(i, cc);
        entry.contentType = instance.getContentType(i, cc);
        entry.md5hash = instance.getContentHash(i, cc);

        String[] pathSegments = entry.filename.split(BasicConsts.FORWARDSLASH);
        String[] fullArgs = new String[5];
        fullArgs[0] = appId;
        fullArgs[1] = tableId;
        fullArgs[2] = schemaETag;
        fullArgs[3] = rowId;
        StringBuilder b = new StringBuilder();
        for ( int j = 0 ; j < pathSegments.length ; ++j ) {
          if ( j != 0 ) {
            b.append(BasicConsts.FORWARDSLASH);
          }
          b.append(pathSegments[j]);
        }
        fullArgs[4] = b.toString();

        URI getFile = ub.clone().path(TableService.class, "getInstanceFiles").path(InstanceFileService.class, "getFile").build(fullArgs, false);
        String locationUrl = getFile.toURL().toExternalForm();
        entry.downloadUrl = locationUrl;

        manifestEntries.add(entry);
      }
      OdkTablesFileManifest manifest = new OdkTablesFileManifest(manifestEntries);

      ResponseBuilder rBuild = Response.ok(manifest);
      if (asAttachment != null && !"".equals(asAttachment)) {
        // Set the filename we're downloading to the disk.
        rBuild.header(HtmlConsts.CONTENT_DISPOSITION, "attachment; " + "filename=\""
            + "manifest.json" + "\"");
      }
      return rBuild.status(Status.OK).build();
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity("Unable to retrieve manifest of attachments for: " + manifestUrl).build();
    } catch (PermissionDeniedException e) {
      LOGGER.error(("ODKTables file upload permissions error: " + e.getMessage()));
      return Response.status(Status.UNAUTHORIZED).entity("Permission denied").build();
    }
  }


  @GET
  @Path("{rowId}/file/{filePath:.*}")
  @GZIP
  // because we want to get the whole path
  public Response getFile(@PathParam("rowId") String rowId,
      @PathParam("filePath") List<PathSegment> segments,
      @QueryParam(PARAM_AS_ATTACHMENT) String asAttachment) throws IOException {
    // The appId and tableId are from the surrounding TableService.
    // The rowId is already pulled out.
    // The segments are just rest/of/path in the full app-centric
    // path of:
    // appid/data/attachments/tableid/instances/instanceId/rest/of/path
    if (rowId == null || rowId.length() == 0) {
      return Response.status(Status.BAD_REQUEST).entity(InstanceFileService.ERROR_MSG_INVALID_ROW_ID)
          .build();
    }
    if (segments.size() < 1) {
      return Response.status(Status.BAD_REQUEST).entity(InstanceFileService.ERROR_MSG_INSUFFICIENT_PATH)
          .build();
    }
    // Now construct the whole path.
    String partialPath = constructPathFromSegments(segments);

    UriBuilder ub = info.getBaseUriBuilder();
    ub.path(TableService.class);

    String[] pathSegments = partialPath.split(BasicConsts.FORWARDSLASH);
    String[] fullArgs = new String[5];
    fullArgs[0] = appId;
    fullArgs[1] = tableId;
    fullArgs[2] = schemaETag;
    fullArgs[3] = rowId;
    StringBuilder b = new StringBuilder();
    for ( int i = 0 ; i < pathSegments.length ; ++i ) {
      if ( i != 0 ) {
        b.append(BasicConsts.FORWARDSLASH);
      }
      b.append(pathSegments[i]);
    }
    fullArgs[4] = b.toString();

    UriBuilder tmp = ub.clone().path(TableService.class, "getInstanceFiles").path(InstanceFileService.class, "getFile");
    URI getFile = tmp.build(fullArgs, false);
    String locationUrl = getFile.toURL().toExternalForm();

    try {
      userPermissions.checkPermission(appId, tableId, TablePermission.READ_ROW);

      DbTableInstanceFiles blobStore = new DbTableInstanceFiles(tableId, cc);
      BlobEntitySet instance = blobStore.getBlobEntitySet(rowId, cc);

      int count = instance.getAttachmentCount(cc);
      for (int i = 1; i <= count; ++i) {
        String path = instance.getUnrootedFilename(i, cc);
        if (path != null && path.equals(partialPath)) {
          byte[] fileBlob = instance.getBlob(i, cc);
          String contentType = instance.getContentType(i, cc);
          Long contentLength = instance.getContentLength(i, cc);

          // And now prepare everything to be returned to the caller.
          if (fileBlob != null && contentType != null && contentLength != null
              && contentLength != 0L) {
            ResponseBuilder rBuild = Response.ok(fileBlob, contentType);
            if (asAttachment != null && !"".equals(asAttachment)) {
              // Set the filename we're downloading to the disk.
              rBuild.header(HtmlConsts.CONTENT_DISPOSITION, "attachment; " + "filename=\""
                  + partialPath + "\"");
            }
            return rBuild.status(Status.OK).build();
          } else {
            return Response.status(Status.NOT_FOUND)
                .entity("File content not yet available for: " + locationUrl).build();
          }

        }
      }
      return Response.status(Status.NOT_FOUND).entity("No file found for: " + locationUrl).build();
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity("Unable to retrieve attachment and access attributes for: " + locationUrl)
          .build();
    } catch (PermissionDeniedException e) {
      LOGGER.error(("ODKTables file upload permissions error: " + e.getMessage()));
      return Response.status(Status.UNAUTHORIZED).entity("Permission denied").build();
    }
  }

  @Override
  @POST
  @Path("{rowId}/file/{filePath:.*}")
  @Consumes({ MediaType.MEDIA_TYPE_WILDCARD })
  // because we want to get the whole path
  public Response putFile(@Context HttpServletRequest req, @PathParam("rowId") String rowId,
      @PathParam("filePath") List<PathSegment> segments, @GZIP byte[] content) throws IOException,
      ODKTaskLockException {

    if (segments.size() < 1) {
      return Response.status(Status.BAD_REQUEST).entity(InstanceFileService.ERROR_MSG_INSUFFICIENT_PATH)
          .build();
    }
    // The appId and tableId are from the surrounding TableService.
    // The rowId is already pulled out.
    // The segments are just rest/of/path in the full app-centric
    // path of:
    // appid/data/attachments/tableid/instances/instanceId/rest/of/path
    String partialPath = constructPathFromSegments(segments);
    String contentType = req.getContentType();
    String md5Hash = CommonFieldsBase.newMD5HashUri(content);
    try {
      userPermissions.checkPermission(appId, tableId, TablePermission.WRITE_ROW);

      UriBuilder ub = info.getBaseUriBuilder();
      ub.path(TableService.class);

      String[] pathSegments = partialPath.split(BasicConsts.FORWARDSLASH);
      String[] fullArgs = new String[5];
      fullArgs[0] = appId;
      fullArgs[1] = tableId;
      fullArgs[2] = schemaETag;
      fullArgs[3] = rowId;
      StringBuilder b = new StringBuilder();
      for ( int j = 0 ; j < pathSegments.length ; ++j ) {
        if ( j != 0 ) {
          b.append(BasicConsts.FORWARDSLASH);
        }
        b.append(pathSegments[j]);
      }
      fullArgs[4] = b.toString();

      UriBuilder tmp = ub.clone().path(TableService.class, "getInstanceFiles").path(InstanceFileService.class, "getFile");
      URI getFile = tmp.build(fullArgs, false);
      String locationUrl = getFile.toURL().toExternalForm();

      DbTableInstanceFiles blobStore = new DbTableInstanceFiles(tableId, cc);
      BlobEntitySet instance = blobStore.newBlobEntitySet(rowId, cc);
      int count = instance.getAttachmentCount(cc);
      for (int i = 1; i <= count; ++i) {
        String path = instance.getUnrootedFilename(i, cc);
        if (path != null && path.equals(partialPath)) {
          // we already have this in our store -- check that it is identical.
          // if not, we have a problem!!!
          if (md5Hash.equals(instance.getContentHash(i, cc))) {
            return Response.status(Status.CREATED).header("Location", locationUrl).build();
          } else {
            return Response.status(Status.BAD_REQUEST)
                .entity(ERROR_FILE_VERSION_DIFFERS + "\n" + partialPath).build();
          }
        }
      }
      BlobSubmissionOutcome outcome = instance
          .addBlob(content, contentType, partialPath, false, cc);
      if (outcome == BlobSubmissionOutcome.NEW_FILE_VERSION) {
        return Response.status(Status.BAD_REQUEST)
            .entity(ERROR_FILE_VERSION_DIFFERS + "\n" + partialPath).build();
      }
      return Response.status(Status.CREATED).header("Location", locationUrl).build();
    } catch (ODKDatastoreException e) {
      LOGGER.error(("ODKTables file upload persistence error: " + e.getMessage()));
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.getMessage()).build();
    } catch (PermissionDeniedException e) {
      LOGGER.error(("ODKTables file upload permissions error: " + e.getMessage()));
      return Response.status(Status.UNAUTHORIZED).entity("Permission denied").build();
    }
  }

  /**
   * Construct the path for the file. This is the entire path excluding the app
   * id.
   *
   * @param segments
   * @return
   */
  private String constructPathFromSegments(List<PathSegment> segments) {
    // Now construct up the path from the segments.
    // We are NOT going to include the app id. Therefore if you upload a file
    // with a path of appid/myDir/myFile.html, the path will be stored as
    // myDir/myFile.html. This is so that when you get the filename on the
    // manifest, it won't matter what is the root directory of your app on your
    // device. Otherwise you might have to strip the first path segment or do
    // something similar.
    StringBuilder sb = new StringBuilder();
    int i = 0;
    for (PathSegment segment : segments) {
      sb.append(segment.getPath());
      if (i < segments.size() - 1) {
        sb.append(BasicConsts.FORWARDSLASH);
      }
      i++;
    }
    String wholePath = sb.toString();
    return wholePath;
  }

}
