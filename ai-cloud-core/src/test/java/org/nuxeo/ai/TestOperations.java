package org.nuxeo.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ai.operations.GetDataSetStats;
import org.nuxeo.ai.operations.Publish;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.PartialDeploy;
import org.nuxeo.runtime.test.runner.TargetExtensions;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ai.ai-cloud-core")
@PartialDeploy(bundle = "studio.extensions.nuxeo-ai-online-services", extensions = { TargetExtensions.ContentModel.class })
public class TestOperations {

    @Inject
    protected CoreSession session;
    
    protected DocumentModel model;
    
	@Before
	public void doBefore() throws Exception {
		
		DocumentModel containerA = session.createDocumentModel("/", "myCustomerA", "AIResourcesContainer");
		containerA.setPropertyValue("airc:projectid", "001");
		containerA = session.createDocument(containerA);

		model = session.createDocumentModel("/myCustomerA", "model1", "AI_Model");
		model.setPropertyValue("file:content", new StringBlob("XXX","tensorflow/model","UTF-8","model.dat"));
		model = session.createDocument(model);
	}

    @Inject
    protected AutomationService automationService;

    @Test
    public void shouldCallPublishOperation() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(model);
        DocumentModel publishedModel = (DocumentModel) automationService.run(ctx, Publish.ID);
        
        assertTrue(publishedModel.isVersion());        
        assertEquals("1.0",publishedModel.getVersionLabel());
        assertNotEquals("", publishedModel.getPropertyValue("dc:source"));      
    }
}
