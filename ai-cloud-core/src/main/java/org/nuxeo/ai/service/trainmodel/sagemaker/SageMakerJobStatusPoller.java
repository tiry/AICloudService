package org.nuxeo.ai.service.trainmodel.sagemaker;

import java.util.List;

import org.nuxeo.ai.sagemaker.JobStatus;
import org.nuxeo.ai.service.ModelTrainerService;
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
			 ModelTrainerService trainer = Framework.getService(ModelTrainerService.class);
			 final List<JobStatus> jobs = trainer.listJobStatus();
			 String repositoryName = Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
			 			 
			 UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(repositoryName) {
				
				@Override
				public void run() {
					
					for (JobStatus job : jobs) {
						
						List<DocumentModel> docs = session.query("select * from AI_Model where ai_model:job_name='" + job.getUrn() + "'");
						if (docs.size()>0) {
						
							DocumentModel  model = docs.get(0);
							
							// set the status : where ?
							
							// if finished
							
							session.saveDocument(model);							
						}
					}					
				}
			};
			
			runner.runUnrestricted();
			 
		 }

	}

}
