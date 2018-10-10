package org.nuxeo.ai.model.train;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface ModelTrainer {

	String scheduleTraining(DocumentModel trainingConfig);	

}
