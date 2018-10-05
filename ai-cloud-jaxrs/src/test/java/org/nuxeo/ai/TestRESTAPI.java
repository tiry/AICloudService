package org.nuxeo.ai;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.PartialDeploy;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.test.runner.TargetExtensions;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy({ "org.nuxeo.ai.ai-cloud-core", "org.nuxeo.ai.ai-cloud-jaxrs" })
@PartialDeploy(bundle = "studio.extensions.nuxeo-ai-online-services", extensions = {
		TargetExtensions.ContentTemplate.class })
// manual deploy
public class TestRESTAPI extends BaseTest {

	@Inject
	CoreSession session;

	String projectId = "007";
	
	protected DocumentModel containerA;
	
	protected DocumentModel datasetB;
	
	@Before
	public void doBefore() throws Exception {
		super.doBefore();
		
		containerA = session.createDocumentModel("/", "myCustomerA", "AIResourcesContainer");
		containerA.setPropertyValue("airc:projectid", projectId);
		containerA = session.createDocument(containerA);


		DocumentModel containerB = session.createDocumentModel("/", "myCustomerB", "AIResourcesContainer");
		containerB.setPropertyValue("airc:projectid", "009");
		containerB = session.createDocument(containerB);

		datasetB = session.createDocumentModel("/myCustomerB/datasets", "ds1", "AI_Corpus");
		datasetB = session.createDocument(datasetB);
		
		session.save();
		DocumentModelList docs = session.query("select * from AIResourcesContainer where airc:projectid='" + projectId + "'");                
		assertEquals(1, docs.size());
		TransactionHelper.commitOrRollbackTransaction();
		TransactionHelper.startTransaction();
	}

	
    protected String createBatchAndUpload() throws IOException {

        // Get batch id, used as a session id
        String batchId;
        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload")) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            batchId = node.get("batchId").asText();
            assertNotNull(batchId);
        }

        String data = "SomeDataExtractedFromNuxeoDBToFeedTensorFlow";
        String fileSize = String.valueOf(data.getBytes(UTF_8).length);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("X-Upload-Type", "normal");
        headers.put("X-File-Name", "aidata.bin");
        headers.put("X-File-Size", fileSize);
        headers.put("X-File-Type", "application/octet-stream");

        try (CloseableClientResponse response = getResponse(RequestType.POST, "upload/" + batchId + "/0", data,
                headers)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("true", node.get("uploaded").asText());
            assertEquals(batchId, node.get("batchId").asText());
            assertEquals("0", node.get("fileIdx").asText());
            assertEquals("normal", node.get("uploadType").asText());
        }        
        return batchId;
    }

    
	@Test
	public void shouldCreateDataSet() throws Exception {
		
		String batchId = createBatchAndUpload();
		
		String payload = "{  " 
				+ "         \"entity-type\": \"document\"," 
				+ "         \"name\": \"myDataSet\","
				+ "         \"type\": \"AI_Corpus\"," 
				+ "         \"properties\": {"
				+ "             \"dc:title\":\"My AI DataSet\"," 
				+ "             \"ai_corpus:documents_count\":1000," 
				+ "             \"ai_corpus:query\":\"Select * from Whatever\","
		        + "             \"file:content\" : { \"upload-batch\": \"" + batchId + "\", \"upload-fileId\": \"0\" } "

		        + "         }" + "     }";
		
        try (CloseableClientResponse response = getResponse(RequestType.POST, "ai/" + projectId, payload)) {
            fetchInvalidations();

            DocumentModelList docs = session.query("select * from AI_Corpus where ecm:path startswith '" + containerA.getPathAsString()+ "/datasets/'");            
            assertEquals(1,docs.size());
                       
            DocumentModel doc = docs.get(0);            
            assertEquals("My AI DataSet", doc.getTitle());
            assertEquals(1000L, doc.getPropertyValue("ai_corpus:documents_count"));  
            
            Blob blob = (Blob) doc.getPropertyValue("file:content");
            assertNotNull(blob);
            assertEquals("aidata.bin", blob.getFilename());           
        }
	}

	@Test
	public void shouldRetrieveDataSet() throws Exception {		
        try (CloseableClientResponse response = getResponse(RequestType.GET, "ai/009/dataset/" + datasetB.getId())) {
    	 assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
         assertEntityEqualsDoc(response.getEntityInputStream(), datasetB);
        }
	}
	
	@Test
	public void shouldUpdateDataSet() throws Exception {		

		String payload = "{  " 
				+ "         \"entity-type\": \"document\"," 
				+ "         \"properties\": {"
				+ "             \"dc:title\":\"My AI DataSet\"," 
				+ "             \"ai_corpus:documents_count\":2000," 
				+ "             \"ai_corpus:query\":\"Select * from Whatever\""
				+ "         }" + "     }";

		try (CloseableClientResponse response = getResponse(RequestType.PUT, "ai/009/dataset/" + datasetB.getId(), payload)) {
    	 assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    	 
    	 datasetB = session.getDocument(datasetB.getRef());
         assertEquals(2000L, datasetB.getPropertyValue("ai_corpus:documents_count"));            
        }
	}
	
}
