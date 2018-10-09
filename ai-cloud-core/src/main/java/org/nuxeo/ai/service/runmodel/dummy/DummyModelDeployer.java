package org.nuxeo.ai.service.runmodel.dummy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ai.service.runmodel.ModelDeployer;

public class DummyModelDeployer implements ModelDeployer {

	protected Map<String, URI> deployedModels= new HashMap<>();
	
	@Override
	public URI deployModel(String modelUUID, URI blobModel) {		
		deployedModels.put(modelUUID, blobModel);		
		return buildEndPointURL(modelUUID);
	}

	@Override
	public URI getModelEndPoint(String modelUUID) {
		if (deployedModels.containsKey(modelUUID)) {
			return buildEndPointURL(modelUUID);
		}
		return null;
	}
	
	@Override	
	public void undeployModel(String modelUUID) {
		deployedModels.remove(modelUUID);
	}
	
	protected URI buildEndPointURL(String modelUUID) {
		
		try {
			return new URI("http", "//127.0.0.1:8080/fakeModelServer/" + modelUUID, null);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

}
