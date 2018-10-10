package org.nuxeo.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ai.blob.AIBlobHelper;
import org.nuxeo.ai.model.train.stream.StreamModelTrainer;
import org.nuxeo.ai.service.AICloudService;
import org.nuxeo.ai.service.AICloudServiceImpl;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.PartialDeploy;
import org.nuxeo.runtime.test.runner.TargetExtensions;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("org.nuxeo.ai.ai-cloud-core")
@PartialDeploy(bundle = "studio.extensions.nuxeo-ai-online-services", extensions = {
		TargetExtensions.ContentModel.class })
public class TestTrainFlow {

	@Inject
	protected AICloudService aicloudservice;
	
	@Inject
	protected StreamService streamService;
	
	@Inject
	protected CoreSession session;

	protected DocumentModel model;

	protected DocumentModel dataset;

	@Before
	public void doBefore() throws Exception {

		// register the test fake Trainer
		AICloudServiceImpl component= (AICloudServiceImpl) Framework.getRuntime().getComponent("org.nuxeo.ai.AICloudService");
		component.registerTrainer(new StreamModelTrainer());
		
		DocumentModel containerA = session.createDocumentModel("/", "myCustomerA", "AIResourcesContainer");
		containerA.setPropertyValue("airc:projectid", "001");
		containerA = session.createDocument(containerA);

		
		// create a dummy dataset
		dataset = session.createDocumentModel("/myCustomerA", "dataset1", "AI_Corpus");
		dataset.setPropertyValue("ai_corpus:training_data", new StringBlob("XXX", "tensorflow/data", "UTF-8", "data.tf"));
		dataset.setPropertyValue("ai_corpus:evaluation_data", new StringBlob("YYY", "tensorflow/data", "UTF-8", "data.tf"));		
		dataset = session.createDocument(dataset);

		// create  Model that will use Stream to simulate training
		model = session.createDocumentModel("/myCustomerA", "model1", "AI_Model");
		model.setPropertyValue("file:content", new StringBlob("XXX", "tensorflow/model", "UTF-8", "model.dat"));
		model.setPropertyValue("ai_model:training_engine", "stream");
		model.setPropertyValue("ai_model:corpus", new String[]{dataset.getId()});
		model = session.createDocument(model);
		
		session.save();
		TransactionHelper.commitOrRollbackTransaction();
		TransactionHelper.startTransaction();
	}

	@Test
	public void testTrain() throws Exception {
		assertNotNull(aicloudservice);

		String key = aicloudservice.trainModel(model);
		assertNotNull(key);

		model = session.getDocument(model.getRef());
				
		// check that model was updated
		Map<String, Serializable> info = (Map<String, Serializable>) model.getPropertyValue("ai_model:training_information");
		assertNotNull(info.get("start"));
		assertEquals(key, info.get("jobId"));
		// XXX check lifecycle

		// check that we did send a message		
		String message = readLogEntry("aiTrainRequests", "aiTrainRequests");
		assertNotNull(message);		
		assertTrue(message.contains(model.getId()));
		
		TransactionHelper.commitOrRollbackTransaction();
		
		// simulate the training and the message sent
		simulateTrainingFinished();
		
		// check that the message was sent
		message = readLogEntry("aiTrainResults", "aiTrainResults");
		assertNotNull(message);		
		assertTrue(message.contains(model.getId()));
		
		// avoid Deadlock in H2!
		Thread.sleep(2000);					
		TransactionHelper.startTransaction();
		
		Calendar endDate=null;		
		do {			
			Thread.sleep(500);			
			model = session.getDocument(model.getRef());
			info = (Map<String, Serializable>) model.getPropertyValue("ai_model:training_information");
			endDate = (Calendar) info.get("end");			
		} while (endDate == null);

		
		model = session.getDocument(model.getRef());
		Blob modelBlob = (Blob) model.getPropertyValue("file:content");
		
		assertEquals("someTFModel", modelBlob.getString());
	}

	protected String readLogEntry(String config, String name) throws Exception {
        LogManager manager = streamService.getLogManager(config);
        try (LogTailer<Record> tailer = manager.createTailer("testGroup", name)) {
            LogRecord<Record> logRecord = null;
            do {
                logRecord = tailer.read(Duration.ofSeconds(1));
                if (logRecord != null) {
                	return new String(logRecord.message().getData(), "UTF-8");
                }
            } while (logRecord != null);
        }
        return null;

	}
	
	protected void simulateTrainingFinished() throws Exception {
		Properties properties = new Properties();

		properties.put("modelUUID", model.getId());
		Blob modelBlob = AIBlobHelper.insertBlob(session, "someTFModel");		
		properties.put("modelURI", AIBlobHelper.getBlobURI(modelBlob).toString());		

		StringWriter writer = new StringWriter();
		properties.store(writer, null);		
		writer.flush();
		
		String key = UUID.randomUUID().toString();

		Record record = null;
		try {
			record = new Record(key, writer.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		LogManager logManager = streamService.getLogManager("aiTrainResults");
		logManager.createIfNotExists("aiTrainResults", 1);
		LogAppender<Record> appender = logManager.getAppender("aiTrainResults");

		appender.append(key, record);		
	}
}
