package org.nuxeo.ai.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ai.service.runmodel.DummyModelRunner;
import org.nuxeo.ai.service.runmodel.ModelRunner;
import org.nuxeo.ai.service.trainmodel.ModelTrainer;
import org.nuxeo.ai.service.trainmodel.stream.StreamModelTrainer;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class AICloudServiceImpl extends DefaultComponent implements AICloudService {

	protected static Log log = LogFactory.getLog(AICloudServiceImpl.class);
	
	protected ModelRunner runner = new DummyModelRunner();
	
	protected ModelTrainer trainer = new StreamModelTrainer();
	
	@Override
	public Object getCorpusStatistics(DocumentModel corpus) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentModel publishModel(DocumentModel model) {
						
		DocumentModel version=null;
		
		if (!model.isCheckedOut()) {
			version = model.getCoreSession().getLastDocumentVersion(model.getRef());
		} else {
			DocumentRef versionRef = model.getCoreSession().checkIn(model.getRef(), VersioningOption.MAJOR, "Publish");
			version = model.getCoreSession().getDocument(versionRef);							
		}
					
		// create the new endpoint
		URI endpoint = deployModel(version);
	
		// store endpoint info inside the doc
		version.putContextData("allowVersionWrite", true);	
		version.setPropertyValue("dc:source", endpoint.toString());
		
		return model.getCoreSession().saveDocument(version);		
	}
	
	
	protected URI deployModel(DocumentModel model) {
		
		URI modelURI = AIBlobHelper.getBlobURI(model, "file:content");
		
		URI endpointURI = runner.deployModel(model.getId(), modelURI);

		return endpointURI;
	}

	@Override
	public DocumentModel unpublishModel(DocumentModel model) {
		runner.undeployModel(model.getId());
		
		DocumentModel version = model.getCoreSession().getDocument(model.getRef());
		// remove endpoint info inside the version
		version.putContextData("allowVersionWrite", true);	
		version.setPropertyValue("dc:source", "");
		
		return model.getCoreSession().saveDocument(version);		

	}

	@Override
	public DocumentModel predict(DocumentModel doc, String modelId) {
		
		DocumentModel model = doc.getCoreSession().getDocument(new IdRef(modelId));

		String endpoint = (String) model.getPropertyValue("dc:source");
		
		// XXX call endpoint

		// TODO Auto-generated method stubs
		return null;
	}

	@Override
	public String trainModel(DocumentModel training) {		
		return trainer.scheduleTraining(training);
	}
	
}
