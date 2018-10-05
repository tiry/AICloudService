package org.nuxeo.ai.operations;

import org.nuxeo.ai.service.AICloudService;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

/**
 *
 */
@Operation(id=Train.ID, category=Constants.CAT_DOCUMENT, label="Train", description="Describe here what your operation does.")
public class Train {

    public static final String ID = "AI.Train";

    @Context
    protected CoreSession session;

    @Context
    protected AICloudService service;    
    
    @Param(name = "model")
    protected DocumentRef model;

    @Param(name = "dataset")
    protected DocumentRef dataset;

    @Param(name = "trainingConfig")
    protected DocumentRef trainingConfig;

    @OperationMethod
    public String run() {    	    	    	
    	String key = service.trainModel(session.getDocument(model), session.getDocument(dataset), session.getDocument(trainingConfig));
    	return key;
    }
}
