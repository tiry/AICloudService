package org.nuxeo.ai.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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

		DocumentModel version = null;

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
		return endpointURI;
	}

	@Override
	public DocumentModel unpublishModel(DocumentModel model) {

		getDeployerForModel(model).undeployModel(model.getId());

		DocumentModel version = model.getCoreSession().getDocument(model.getRef());
		// remove endpoint info inside the version
		version.putContextData("allowVersionWrite", true);

		Map<String, String> info = (Map<String, String>) version.getPropertyValue("ai_model:deployment_information");
		info.put("endpoint", "");
		version.setPropertyValue("ai_model:deployment_information", (Serializable) info);

		return model.getCoreSession().saveDocument(version);

	}

	@Override
	public PredictionResult predict(DocumentModel model, InputStream payload) {

		Map<String, String> info = (Map<String, String>) model.getPropertyValue("ai_model:deployment_information");
		String endpoint = info.get("endpoint");

		if (endpoint == null) {
			return new PredictionResult.NoEndPointResult();
		}

		URL url;
		try {
			url = new URL(endpoint);
		} catch (MalformedURLException e) {
			log.error("Bad endpoint url", e);
			return new PredictionResult.NoEndPointResult();
		}

		// Check that the endpoint is running
		HttpURLConnection conn;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);

			IOUtils.copy(payload, conn.getOutputStream());
			int responseCode = conn.getResponseCode();
			if (responseCode == 200) {
				Writer writer = new StringWriter();
				IOUtils.copy(conn.getInputStream(), writer, "UTF-8");
				return new PredictionResult(responseCode, writer.toString());
			} else {
				Writer writer = new StringWriter();
				IOUtils.copy(conn.getErrorStream(), writer, "UTF-8");
				String message = writer.toString();
				log.error("call to model endpoint returned " + responseCode + " - " + message);
				return new PredictionResult(responseCode, message);
			}
		} catch (IOException e) {
			log.error("Unable to call model", e);
			return new PredictionResult(500, e.getMessage());
		}
	}

	@Override
	public String trainModel(DocumentModel model) {

		String key = getTrainerForModel(model).train(model);

		// mark Model Training as scheduled
		Map<String, Serializable> info = (Map<String, Serializable>) model
				.getPropertyValue("ai_model:training_information");
		info.clear();
		info.put("start", GregorianCalendar.getInstance());
		info.put("jobId", key);
		model.setPropertyValue("ai_model:training_information", (Serializable) info);
		model.getCoreSession().saveDocument(model);

		return key;
	}

	protected ModelTrainer getTrainerForModel(DocumentModel model) {
		String engine = (String) model.getPropertyValue("ai_model:training_engine");
		if (engine == null || engine.equals("")) {
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

		if (deployer == null || deployer.equals("")) {
			deployer = DEFAULT_DEPLOYER;
		}
		if (deployers.containsKey(deployer)) {
			return deployers.get(deployer);
		} else {
			return deployers.get(DEFAULT_DEPLOYER);
		}
	}

}
