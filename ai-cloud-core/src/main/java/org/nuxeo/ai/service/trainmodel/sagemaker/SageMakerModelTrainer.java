package org.nuxeo.ai.service.trainmodel.sagemaker;

import org.nuxeo.ai.service.ModelTrainerService;
import org.nuxeo.ai.service.trainmodel.ModelTrainer;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

public class SageMakerModelTrainer implements ModelTrainer {

	@Override
	public String scheduleTraining(DocumentModel training) {
		
		ModelTrainerService trainer = Framework.getService(ModelTrainerService.class);
		
		String modelUUID = (String) training.getPropertyValue("ait:srcModel");
		String datasetUUID = (String) training.getPropertyValue("ait:srcDataset");

		String jobId = trainer.train(modelUUID, null, null);
		
		// save the job id the model		
				
		return jobId;
	}
	
	

}
