package org.nuxeo.ai.service;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface AICloudService {

	public Object getCorpusStatistics(DocumentModel corpus);

	public DocumentModel publishModel(DocumentModel model);

	public DocumentModel unpublishModel(DocumentModel model);

	public DocumentModel predict(DocumentModel document, String modelId);

	public String trainModel(DocumentModel trainingConfig);

}
