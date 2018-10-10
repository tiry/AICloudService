package org.nuxeo.ai.model.train;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ai.blob.AIBlobHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

public abstract class AbstractModelTrainer implements ModelTrainer {

	protected static Log log = LogFactory.getLog(AbstractModelTrainer.class);
	
	public String train(DocumentModel model) {

		String datasetsIds[] = (String[]) model.getPropertyValue("ai_model:corpus");
		
		List<URI> trainingURIs = new ArrayList<>();
		List<URI> evaluationURIs = new ArrayList<>();
		
		for (String id: datasetsIds) {
			DocumentModel dataset = model.getCoreSession().getDocument(new IdRef(id));
			trainingURIs.add(AIBlobHelper.getBlobURI(dataset, "ai_corpus:training_data"));
			evaluationURIs.add(AIBlobHelper.getBlobURI(dataset, "ai_corpus:evaluation_data"));			
		}				
		URI modelURI = AIBlobHelper.getBlobURI(model, "file:content");
		
		return startTraining(model, modelURI, trainingURIs, evaluationURIs);
	}
	
	protected abstract String startTraining(DocumentModel model, URI modelURI, List<URI> trainingURIs, List<URI> evaluationURIs);


}
