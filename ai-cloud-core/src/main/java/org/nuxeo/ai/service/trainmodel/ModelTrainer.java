package org.nuxeo.ai.service.trainmodel;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface ModelTrainer {

	String scheduleTraining(DocumentModel trainingConfig);	

}
