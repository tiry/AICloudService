package org.nuxeo.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ai.model.deploy.localdocker.DockerLocalDeployer;
import org.nuxeo.ai.service.AICloudService;
import org.nuxeo.ai.service.AICloudServiceImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.PartialDeploy;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.test.runner.TargetExtensions;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.github.dockerjava.api.model.ExposedPort;
import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy({ "org.nuxeo.ai.ai-cloud-core", "org.nuxeo.ai.ai-cloud-jaxrs" })
@PartialDeploy(bundle = "studio.extensions.nuxeo-ai-online-services", extensions = {
		TargetExtensions.ContentTemplate.class })
public class TestFakePredic extends BaseTest {

	// define custom Docker deployer
	protected class HttpEchoDockerLocalDeployer extends DockerLocalDeployer {

		protected String getImageName() {
			return "solsson/http-echo";
		}

		protected ExposedPort getExposedPort() {
			ExposedPort tcp80 = ExposedPort.tcp(80);
			return tcp80;
		}

		protected String getCmd() {
			return null;
		}

		public String getName() {
			return "http-echo";
		}

		protected URI buildEndPointURI(int port) {
			try {
				return new URI("http", "//127.0.0.1:" + port + "/echo", null);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	@Inject
	CoreSession session;

	String projectId = "013";

	protected DocumentModel container;

	protected DocumentModel model;

	protected DocumentModel publishedModel = null;

	@Before
	public void doBefore() throws Exception {
		super.doBefore();

		// register the test fake Trainer
		AICloudServiceImpl component = (AICloudServiceImpl) Framework.getRuntime()
				.getComponent("org.nuxeo.ai.AICloudService");
		component.registerDeployer(new HttpEchoDockerLocalDeployer());

		// create a container
		container = session.createDocumentModel("/", "myCustomerB", "AIResourcesContainer");
		container.setPropertyValue("airc:projectid", projectId);
		container = session.createDocument(container);

		// create a model
		model = session.createDocumentModel("/myCustomerB/models", "model1", "AI_Model");

		// force association to the fake published
		Map<String, String> info = new HashMap<>();
		info.put("deployer", "http-echo");
		model.setPropertyValue("ai_model:deployment_information", (Serializable) info);

		model.setPropertyValue("file:content", new StringBlob("whatever"));
		model = session.createDocument(model);

		session.save();

		// publish the model
		AICloudService service = Framework.getService(AICloudService.class);
		publishedModel = service.publishModel(model);

		// check that publishing is done
		assertTrue(publishedModel.isVersion());
		assertEquals("1.0", publishedModel.getVersionLabel());

		info = (Map<String, String>) publishedModel.getPropertyValue("ai_model:deployment_information");
		assertNotNull(info.get("endpoint"));

		TransactionHelper.commitOrRollbackTransaction();
		TransactionHelper.startTransaction();
	}

	@After
	public void doAfter() {
		// stop the Docker Echo server!
		AICloudService service = Framework.getService(AICloudService.class);
		if (publishedModel != null) {
			service.unpublishModel(publishedModel);
		}
	}

	@Test
	public void shouldCallDeployedEchoEndPoint() throws Exception {

		String payload = "I call a fake TF enpoint";

		try (CloseableClientResponse response = getResponse(RequestType.POST,
				"ai/" + projectId + "/model/" + publishedModel.getId() + "/predict", payload)) {

			Writer w = new StringWriter();
			IOUtils.copy(response.getEntityInputStream(), w, "UTF-8");

			// check that we received an echo
			assertTrue(w.toString().contains("\"body\": \"I call a fake TF enpoint\""));
		}
	}

}
