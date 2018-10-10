package org.nuxeo.ai.model.train.sagemaker;

import java.util.List;

import org.nuxeo.ai.model.train.JobStatus;
import org.nuxeo.ai.model.train.ModelTrainer;
import org.nuxeo.ai.service.AICloudService;
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
		 		 
		 ModelTrainer trainer = Framework.getService(AICloudService.class).getTrainer(SageMakerTrainer.NAME);
		 
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
		List<DocumentModel> docs = session.query("select * from AI_Model where ai_model:traininginfo/jobId='" + job.getUrn() + "'");
		if (docs.size()>0) {
			return docs.get(0);					
		}
		return null;
	}

}
