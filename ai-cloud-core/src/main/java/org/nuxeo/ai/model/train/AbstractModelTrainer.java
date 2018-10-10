package org.nuxeo.ai.model.train;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ai.blob.AIBlobHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.versioning.VersioningService;

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

	public void updateModelAfterTraining(DocumentModel model, URI newModelURI) throws Exception {

		CoreSession session = model.getCoreSession();		
		Blob blob = AIBlobHelper.resolveBlobFromURI(session, newModelURI);
		model.setPropertyValue("file:content", (Serializable) blob);

		Map<String, Serializable> info = (Map<String, Serializable>) model.getPropertyValue("ai_model:training_information");
		info.put("jobId", "");
		info.put("end", GregorianCalendar.getInstance());					
		model.setPropertyValue("ai_model:training_information", (Serializable) info);
							
		model.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);

		session.saveDocument(model);
	}

}
