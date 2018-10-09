package org.nuxeo.ai.service.runmodel;

import java.net.URI;

public interface ModelRunner {
	
	URI deployModel(String modelUUID, URI blobModel);
	
	URI getModelEndPoint(String modelUUID);

	void undeployModel(String modelUUID);

}