package org.nuxeo.ai.operations;

import org.nuxeo.ai.service.AICloudService;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 *
 */
@Operation(id=Publish.ID, category=Constants.CAT_DOCUMENT, label="Publish", description="Describe here what your operation does.")
public class Publish {

    public static final String ID = "AI.Publish";

    @Context
    protected CoreSession session;

    @Context
    protected AICloudService service;    
    
    @OperationMethod
    public DocumentModel run(DocumentModel model) {    	
    	return service.publishModel(model);
    }
}
