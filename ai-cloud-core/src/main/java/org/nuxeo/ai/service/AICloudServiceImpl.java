package org.nuxeo.ai.service;

import java.io.Serializable;
import java.net.URI;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ai.blob.AIBlobHelper;
import org.nuxeo.ai.model.deploy.ModelDeployer;
import org.nuxeo.ai.model.deploy.dummy.DummyModelDeployer;
import org.nuxeo.ai.model.deploy.localdocker.DockerLocalDeployer;
import org.nuxeo.ai.model.train.ModelTrainer;
import org.nuxeo.ai.model.train.sagemaker.SageMakerTrainer;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

public class AICloudServiceImpl extends DefaultComponent implements AICloudService {

	protected static Log log = LogFactory.getLog(AICloudServiceImpl.class);
	
    protected Map<String, ModelTrainer> trainers = new HashMap<>();	
	
	protected Map<String, ModelDeployer> deployers = new HashMap<>();	
		
	protected static final String DEFAULT_ENGINE = "sagemaker";

	protected static final String DEFAULT_DEPLOYER = "dummy";

	public void registerTrainer(ModelTrainer trainer) {
		trainers.put(trainer.getName(), trainer);
	}

	public void registerDeployer(ModelDeployer deployer) {
		deployers.put(deployer.getName(), deployer);
	}

	@Override
	public void activate(ComponentContext context) {
		registerTrainer(new SageMakerTrainer());
		registerDeployer(new DummyModelDeployer());
		registerDeployer(new DockerLocalDeployer());		
	}

	public ModelTrainer getTrainer(String engine) {
		return trainers.get(engine);
	}
	
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
		
		Map<String, String> info = (Map<String, String>) version.getPropertyValue("ai_model:deployment_information");
		info.put("endpoint", endpoint.toString());
		version.setPropertyValue("ai_model:deployment_information", (Serializable) info);
		
		return model.getCoreSession().saveDocument(version);		
	}
	
	
	protected URI deployModel(DocumentModel model) {
		
		URI modelURI = AIBlobHelper.getBlobURI(model, "file:content");
			
		URI endpointURI = getDeployerForModel(model).deployModel(model.getId(), modelURI);

		// XXX store endpoint
		
		return endpointURI;
	}

	@Override
	public DocumentModel unpublishModel(DocumentModel model) {
		
		getDeployerForModel(model).undeployModel(model.getId());
		
		DocumentModel version = model.getCoreSession().getDocument(model.getRef());
		// remove endpoint info inside the version
		version.putContextData("allowVersionWrite", true);	
		
		version.setPropertyValue("dc:source", "");
		
		return model.getCoreSession().saveDocument(version);		

	}

	@Override
	public String predict(DocumentModel doc, String modelId) {
		
		DocumentModel model = doc.getCoreSession().getDocument(new IdRef(modelId));

		String endpoint = (String) model.getPropertyValue("dc:source");
		
		// XXX
		// check alive
		// redeploy endpoint if needed		
		
		// XXX call endpoint
		// see https://github.com/nuxeo/ai-core/blob/master/nuxeo-ai-model/src/main/java/org/nuxeo/ai/model/serving/TFRuntimeModel.java
		
		// return JSON string
		
		return null;
	}

	@Override
	public String trainModel(DocumentModel model) {		
		
		String key= getTrainerForModel(model).train(model);

		// mark Model Training as scheduled
		Map<String, Serializable> info = (Map<String, Serializable>) model.getPropertyValue("ai_model:training_information");
		info.clear();
		info.put("start", GregorianCalendar.getInstance());
		info.put("jobId", key);				
		model.setPropertyValue("ai_model:training_information", (Serializable) info);
		model.getCoreSession().saveDocument(model);
		
		return key;				
	}
		
	protected ModelTrainer getTrainerForModel(DocumentModel model) {
		String engine = (String) model.getPropertyValue("ai_model:training_engine");
		if (engine==null || engine.equals("")) {
			engine = DEFAULT_ENGINE;
		}
		if (trainers.containsKey(engine)) {
			return trainers.get(engine);
		} else {
			return trainers.get(DEFAULT_ENGINE);
		}
	}

	protected ModelDeployer getDeployerForModel(DocumentModel model) {
		Map<String, String> info = (Map<String, String>) model.getPropertyValue("ai_model:deployment_information");
		
		String deployer = info.get("deployer");
		
		if (deployer==null || deployer.equals("")) {
			deployer = DEFAULT_DEPLOYER;
		}
		if (deployers.containsKey(deployer)) {
			return deployers.get(deployer);
		} else {
			return deployers.get(DEFAULT_DEPLOYER);
		}
	}

}
