package org.nuxeo.ai.service;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class AICloudServiceImpl extends DefaultComponent implements AICloudService {

	protected static Log log = LogFactory.getLog(AICloudServiceImpl.class);
	
	@Override
	public Object getCorpusStatistics(DocumentModel corpus) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentModel publishModel(DocumentModel model) {
		if (model.isCheckedOut()) {
			
		}
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentModel unpublishModel(DocumentModel model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentModel predict(DocumentModel model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object trainModel(DocumentModel model, DocumentModel corpus) {

		BlobManager bm = Framework.getService(BlobManager.class);

		Blob modelBlob = (Blob) model.getPropertyValue("file:content");		
		Blob corpusBlob = (Blob) corpus.getPropertyValue("file:content");

		URI modelURI = null;
		URI corpusURI = null;

		try {
			modelURI = bm.getURI(modelBlob, null, null);
			corpusURI = bm.getURI(corpusBlob, null, null);
		} catch (IOException e) {
			log.error("Unable to generate url for blob", e);			
		}
		
		// Call SageMaker
		
		
		return null;
	}
}
