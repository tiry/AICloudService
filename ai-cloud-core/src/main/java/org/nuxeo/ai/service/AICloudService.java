package org.nuxeo.ai.service;

import java.io.InputStream;

import org.nuxeo.ai.model.train.ModelTrainer;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface AICloudService {

	public Object getCorpusStatistics(DocumentModel corpus);

	public DocumentModel publishModel(DocumentModel model);

	public DocumentModel unpublishModel(DocumentModel model);

	public PredictionResult predict(DocumentModel model, InputStream payload);
	
	public String trainModel(DocumentModel model);
		
	public ModelTrainer getTrainer(String engine);
}
