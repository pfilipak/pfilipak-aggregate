/*
 * Copyright (C) 2010 University of Washington
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
package org.opendatakit.aggregate.query;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.datamodel.FormDefinition;
import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData;
import org.opendatakit.aggregate.form.Form;
import org.opendatakit.aggregate.form.FormInfo;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.EntityKey;
import org.opendatakit.common.persistence.InstanceDataBase;
import org.opendatakit.common.persistence.Query;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.User;

public class QueryFormList {
  private final Datastore ds;
  private final User user;

  private List<Form> forms;
  
  /**
   * Private constructor for common work to both constructors
   * 
   * @param requestingUser user that is requesting the forms to be queried
   * @param datastore   datastore reference
   */
  
  private QueryFormList(Datastore datastore, User user) {
    ds = datastore;
    this.user = user;
    forms = new ArrayList<Form>();    
  }

  /**
   * Constructor that queries the database for available forms
   * 
   * @param requestingUser user that is requesting the forms to be queried
   * @param checkAuthorization true if authorization rules should be used to filter form list, false otherwise
   * @param datastore  datastore reference

   * @throws ODKDatastoreException
 * @throws ODKIncompleteSubmissionData 
   */
  public QueryFormList(boolean checkAuthorization, Datastore datastore, User user) throws ODKDatastoreException, ODKIncompleteSubmissionData{
    this(datastore, user);
    
    FormDefinition fd = FormInfo.getFormDefinition(datastore);
    Query formQuery = ds.createQuery(fd.getTopLevelGroup().getBackingObjectPrototype(), user);
    List<? extends CommonFieldsBase> formEntities = formQuery.executeQuery(ServletConsts.FETCH_LIMIT);
    for (CommonFieldsBase formEntity : formEntities) {
      Form form = new Form((InstanceDataBase) formEntity, ds, user);
      addIfAuthorized(form, checkAuthorization);
    }
  }
  
  /**
   * Constructor that queries the database for the forms referenced by the formkeys. List will only contain forms that are specified in arguments
   * 
   * @param formKeys
   * @param requestingUser user that is requesting the forms to be queried
   * @param checkAuthorization true if authorization rules should be used to filter form list, false otherwise
   * @param datastore  datastore reference
   */
  public QueryFormList(List<EntityKey> formKeys, boolean checkAuthorization, Datastore datastore, User user) {
    this(datastore, user);
    
    for (EntityKey formKey : formKeys) {
      try {
    	CommonFieldsBase rel = ds.getEntity(formKey.getRelation(), formKey.getKey(), user);
        Form form = new Form((InstanceDataBase) rel, ds, user);
        addIfAuthorized(form, checkAuthorization);
      } catch (Exception e) {
        // TODO: determine how to better handle error
      }
    }
  }

  /**
   * Get the resulting Forms from the query
   * @return list of Forms
   */
  public List<Form> getForms() {
    return forms;
  }
  
  /**
   * Apply's authorization logic to determine whether form should be included.
   * 
   * @param form possible form to be included in result list
   * @param checkAuthorization true if authorization rules should be used to filter form list, false otherwise
   */
  private void addIfAuthorized(Form form, boolean checkAuthorization) {
    // TODO: improve with groups management, etc
    if(form.getCreationUser().equals(user)) {
      forms.add(form);
    } else {
      forms.add(form);
    }
  }
}
