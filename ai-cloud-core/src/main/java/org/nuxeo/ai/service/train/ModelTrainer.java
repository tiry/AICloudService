package org.nuxeo.ai.service.train;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface ModelTrainer {

	String scheduleTraining(DocumentModel trainingConfig);	

}
