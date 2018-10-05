package org.nuxeo.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ai.service.AICloudService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.PartialDeploy;
import org.nuxeo.runtime.test.runner.TargetExtensions;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("org.nuxeo.ai.ai-cloud-core")
@PartialDeploy(bundle = "studio.extensions.nuxeo-ai-online-services", extensions = { TargetExtensions.ContentModel.class })
public class TestTrainFlow {

    @Inject
    protected AICloudService aicloudservice;

    @Inject
    protected CoreSession session;
    
    protected DocumentModel model;

    protected DocumentModel dataset;
    
    protected DocumentModel notebook;
    
	@Before
	public void doBefore() throws Exception {
		
		DocumentModel containerA = session.createDocumentModel("/", "myCustomerA", "AIResourcesContainer");
		containerA.setPropertyValue("airc:projectid", "001");
		containerA = session.createDocument(containerA);

		model = session.createDocumentModel("/myCustomerA", "model1", "AI_Model");
		model.setPropertyValue("file:content", new StringBlob("XXX","tensorflow/model","UTF-8","model.dat"));
		model = session.createDocument(model);
		
		dataset = session.createDocumentModel("/myCustomerA", "dataset1", "AI_Corpus");
		dataset.setPropertyValue("file:content", new StringBlob("XXX","tensorflow/data","UTF-8","data.tf"));
		dataset = session.createDocument(dataset);
		
		notebook = session.createDocumentModel("/myCustomerA", "notebook1", "Note");
		notebook.setPropertyValue("note:note", "tensorflow/notebook");
		notebook = session.createDocument(notebook);
		
	}

		
    @Test
    public void testTraih() {
        assertNotNull(aicloudservice);
        
        String key = aicloudservice.trainModel(model, dataset, notebook);
        
        assertNotNull(key);
    }
}
