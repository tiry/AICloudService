package org.nuxeo.ai.model.deploy;

import java.net.URI;

public interface ModelDeployer {
	
	URI deployModel(String modelUUID, URI blobModel);
	
	URI getModelEndPoint(String modelUUID);

	void undeployModel(String modelUUID);

}
