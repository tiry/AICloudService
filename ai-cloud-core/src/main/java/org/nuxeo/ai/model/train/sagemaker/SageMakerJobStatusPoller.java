package org.nuxeo.ai.model.train.sagemaker;

import java.util.List;

import org.nuxeo.ai.sagemaker.JobStatus;
import org.nuxeo.ai.service.ModelTrainerService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.runtime.api.Framework;

public class SageMakerJobStatusPoller implements EventListener {

	@Override
	public void handleEvent(Event event) {		
		 if (event.getName().equals("sagemakerpoll")) {			 
			 // XXX do Critical section using KVStore
			 synchronized (this) {
				 pollSageMaker();	
			}			 
		 }
	}
	
	
	protected void pollSageMaker() {
		 ModelTrainerService trainer = Framework.getService(ModelTrainerService.class);
		 
		 final List<JobStatus> jobs = trainer.listJobStatus();
		 
		 String repositoryName = Framework.getService(RepositoryManager.class).getDefaultRepositoryName();		 			 

		 new UnrestrictedSessionRunner(repositoryName) {
			
			@Override
			public void run() {
				
				for (JobStatus job : jobs) {				
					DocumentModel  model = findModelByJob(session, job);
					if (model!=null) {
						updateModelStatus(session, model, job);
					}
				}
			}
		}.runUnrestricted();		
	}

	protected void updateModelStatus(CoreSession session, DocumentModel model, JobStatus job) {
		// set the status : where ?
		
		// if finished
		
		session.saveDocument(model);
	}
	
	protected DocumentModel findModelByJob(CoreSession session, JobStatus job) {
		List<DocumentModel> docs = session.query("select * from AI_Model where ai_model:job_name='" + job.getUrn() + "'");
		if (docs.size()>0) {
			return docs.get(0);					
		}
		return null;
	}

}
