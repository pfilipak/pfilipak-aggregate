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
package org.opendatakit.aggregate.task;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.constants.BeanDefs;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.constants.TaskLockType;
import org.opendatakit.aggregate.exception.ODKExternalServiceDependencyException;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.exception.ODKTaskLockException;
import org.opendatakit.aggregate.externalservice.ExternalService;
import org.opendatakit.aggregate.externalservice.FormServiceCursor;
import org.opendatakit.aggregate.form.Form;
import org.opendatakit.aggregate.process.DeleteSubmissions;
import org.opendatakit.aggregate.submission.SubmissionKey;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.Query;
import org.opendatakit.common.persistence.TaskLock;
import org.opendatakit.common.persistence.TopLevelDynamicBase;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.User;

/**
 * Common worker implementation for the deletion of a form.
 * 
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 * 
 */
public class FormDeleteWorkerImpl {

	private final Form form;
	private final User user;

	public FormDeleteWorkerImpl(Form form, User user) {
		this.form = form;
		this.user = user;
	}

	public final void deleteForm() throws ODKDatastoreException,
			ODKFormNotFoundException, ODKExternalServiceDependencyException {
		Datastore ds = (Datastore) ContextFactory.get().getBean(
				BeanDefs.DATASTORE_BEAN);

		List<ExternalService> services = FormServiceCursor
				.getExternalServicesForForm(form, null, ds, user);
		for (ExternalService service : services) {
			String uriExternalService = service.getFormServiceCursor()
					.getUri();
			TaskLock taskLock = ds.createTaskLock();
			String pLockId = UUID.randomUUID().toString();
			boolean deleted = false;
			try {
				if (taskLock.obtainLock(pLockId, uriExternalService,
						TaskLockType.UPLOAD_SUBMISSION)) {
					taskLock = null;
					service.delete();
					deleted = true;
				}
			} catch (ODKTaskLockException e1) {
				e1.printStackTrace();
			} finally {
				if ( !deleted ) {
					// TODO: repost...
					return;
				}
			}
			taskLock = ds.createTaskLock();
			try {
				for (int i = 0; i < 10; i++) {
					if (taskLock.releaseLock(pLockId, uriExternalService,
							TaskLockType.UPLOAD_SUBMISSION))
						break;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// just move on, this retry mechanism is to only
						// make things
						// nice
					}
				}
			} catch (ODKTaskLockException e) {
				e.printStackTrace();
			}
		}

		CommonFieldsBase relation = null;
		relation = form.getTopLevelGroupElement().getFormDataModel()
				.getBackingObjectPrototype();

		if (relation != null) {
			// retrieve submissions
			Query surveyQuery = ds.createQuery(relation, user);
			surveyQuery.addSort(relation.lastUpdateDate,
					Query.Direction.DESCENDING);
			surveyQuery
					.addSort(relation.primaryKey, Query.Direction.DESCENDING);

			List<? extends CommonFieldsBase> submissionEntities = surveyQuery
					.executeQuery(ServletConsts.FORM_DELETE_RECORD_QUERY_LIMIT);

			if (submissionEntities.size() > 0) {
				List<SubmissionKey> keys = new ArrayList<SubmissionKey>();
				String topLevelGroupName = form.getTopLevelGroupElement()
						.getElementName();
				for (CommonFieldsBase en : submissionEntities) {
					TopLevelDynamicBase tl = (TopLevelDynamicBase) en;
					keys.add(new SubmissionKey(form.getFormId(), tl
							.getModelVersion(), tl.getUiVersion(),
							topLevelGroupName, tl.getUri()));
				}
				DeleteSubmissions delete;
				try {
					delete = new DeleteSubmissions(keys, ds, user);
				} catch (Exception e) {
					throw new ODKDatastoreException(e);
				}
				delete.deleteSubmissions();
				FormDelete fd = (FormDelete) ContextFactory.get().getBean(
						BeanDefs.FORM_DELETE_BEAN);

				fd.createFormDeleteTask(form, user);
				return;
			}
		}
		// no submissions or no for definition available -- delete the form...
		form.deleteForm(ds, user);
	}
}
