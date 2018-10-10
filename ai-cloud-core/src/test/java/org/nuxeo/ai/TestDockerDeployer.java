package org.nuxeo.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

import org.junit.Test;
import org.nuxeo.ai.service.runmodel.localdocker.DockerLocalDeployer;

import com.github.dockerjava.api.model.ExposedPort;

public class TestDockerDeployer {

	// override to run httpd
	protected class HttpdDockerLocalDeployer extends DockerLocalDeployer {

		protected String getImageName() {
			return "httpd";
		}

		protected ExposedPort getExposedPort() {
			ExposedPort tcp80 = ExposedPort.tcp(80);
			return tcp80;
		}
		
		protected String getCmd() {
			return null;
		}
		
	}
	
	@Test
	public void testDeployHttpViaDocker() throws Exception {
		
		DockerLocalDeployer deployer = new HttpdDockerLocalDeployer();		
		String modelUUID = UUID.randomUUID().toString();

		// deploy a Docker instance
		URI endpoint = deployer.deployModel(modelUUID, new URI("blob","01", null));		
		assertNotNull(endpoint);		
		URL url = endpoint.toURL();

		// Check that the endpoint is running
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setRequestMethod("GET");		
	    assertEquals(200, conn.getResponseCode());

	    // Check that the endpoint is idempotent
		URI endpoint2 = deployer.getModelEndPoint(modelUUID);
		assertNotNull(endpoint2);
		assertTrue( endpoint2.equals(endpoint));
		
		// Shutdown the endpoint
		deployer.undeployModel(modelUUID);
		
		// check that the endpoint is no longer there
		endpoint = deployer.getModelEndPoint(modelUUID);
		assertNull(endpoint);
				
	}
}
