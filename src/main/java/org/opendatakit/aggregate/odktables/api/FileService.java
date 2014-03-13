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
package org.opendatakit.aggregate.odktables.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.GZIP;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;

/**
 * Servlet for handling the uploading and downloading of files from the phone.
 * <p>
 * The general idea is that the interaction with the actual files will occur at
 * /odktables/files/unrootedPathToFile. Files will thus be referred to by their
 * unrooted path relative to the /odk/tables/ directory on the device.
 * <p>
 * A GET request to that url will download the file. A POST request to that url
 * must contain an entity that is the file, as well as a table id parameter on
 * the POST itself.
 * <p>
 * These urls should be generated by a file manifest servlet on a table id
 * basis.
 *
 * @author sudar.sam@gmail.com
 *
 */
@Path("/files")
public interface FileService {

  /**
   * The url of the servlet that for downloading and uploading files. This must
   * be appended to the odk table service.
   */
  public static final String SERVLET_PATH = "files";

  public static final String PARAM_AS_ATTACHMENT = "as_attachment";
  public static final String ERROR_MSG_INSUFFICIENT_PATH = "Not Enough Path Segments: must be at least 2.";
  public static final String ERROR_MSG_UNRECOGNIZED_APP_ID = "Unrecognized app id: ";
  public static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";

  @GET
  @Path("{filePath:.*}")
  @GZIP
  public Response getFile(@PathParam("filePath") List<PathSegment> segments, @QueryParam(PARAM_AS_ATTACHMENT) String asAttachment) throws IOException;

  @POST
  @Path("{filePath:.*}")
  @Consumes({MediaType.MEDIA_TYPE_WILDCARD})
  public Response putFile(@Context HttpServletRequest req, @PathParam("filePath") List<PathSegment> segments, @GZIP byte[] content) throws IOException, ODKTaskLockException;

}
