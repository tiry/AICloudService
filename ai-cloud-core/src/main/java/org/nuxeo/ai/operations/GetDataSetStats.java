package org.nuxeo.ai.operations;

import org.nuxeo.ai.service.AICloudService;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

/**
 *
 */
@Operation(id=GetDataSetStats.ID, category=Constants.CAT_DOCUMENT, label="GetDataSetStats", description="Describe here what your operation does.")
public class GetDataSetStats {

    public static final String ID = "AI.GetDataSetStats";

    @Context
    protected CoreSession session;

    @Context
    protected AICloudService service;    
    
    @OperationMethod
    public Blob run(DocumentModel corpus) {
    	return new StringBlob(corpus.getPathAsString());
    	//return new StringBlob(service.getCorpusStatistics(corpus).toString());
    }
}
