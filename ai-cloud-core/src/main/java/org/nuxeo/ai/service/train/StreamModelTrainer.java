package org.nuxeo.ai.service.train;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ai.service.AICloudServiceImpl;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

public class StreamModelTrainer implements ModelTrainer {

	protected static Log log = LogFactory.getLog(StreamModelTrainer.class);

	protected LogManager oplogManager = null;

	protected LogAppender<Record> appender = null;

	protected final String TRAIN_LOG_SCHEDULE_PROP = "nuxeo.stream.ai.train.schedule.log.config";

	protected final String TRAIN_LOG_RESULT_PROP = "nuxeo.stream.ai.train.result.log.config";

	protected final String DEFAULT_TRAIN_SCHEDULE_NAME = "aiTrainRequests";
	
	protected final String DEFAULT_TRAIN_RESULTS_NAME = "aiTrainResults";
	
	
	protected String getLoggerStreamConfig() {
		return Framework.getProperty(TRAIN_LOG_SCHEDULE_PROP, DEFAULT_TRAIN_SCHEDULE_NAME);
	}
	
	protected String getConsumerStreamConfig() {
		return Framework.getProperty(TRAIN_LOG_RESULT_PROP, DEFAULT_TRAIN_RESULTS_NAME);
	}
	
	protected LogAppender<Record> getLogAppender() {
		if (appender == null) {
			synchronized (this) {
				if (appender == null) {
					StreamService service = Framework.getService(StreamService.class);
					LogManager oplogManager = service.getLogManager(getLoggerStreamConfig());
					oplogManager.createIfNotExists(DEFAULT_TRAIN_SCHEDULE_NAME, 1);
					appender = oplogManager.getAppender(DEFAULT_TRAIN_SCHEDULE_NAME);
					
				}
			}
		}
		return appender;
	}
	
	protected URI getBlobURI(DocumentModel doc, String xpath) {

		BlobManager bm = Framework.getService(BlobManager.class);
		Blob blob = (Blob) doc.getPropertyValue(xpath);
		URI uri = null;
		try {
			uri =bm.getURI(blob, null, null);
		} catch (IOException e) {			
		}
		if (uri==null) {
			try {
				uri= new URI("blob", blob.getDigest(), null);
			} catch (URISyntaxException e1) {				
				e1.printStackTrace();
			}			
		}
		return uri;
	}
	
	public String scheduleTraining(DocumentModel model, DocumentModel dataset, DocumentModel trainingConfig) {
		

		URI modelURI = getBlobURI(model, "file:content");
		URI corpusURI = getBlobURI(dataset, "file:content");

		String key = UUID.randomUUID().toString();
		
		Properties properties = new Properties();
			
		properties.put("modelUUID", model.getId());
		properties.put("modelURI", modelURI.toString());
		properties.put("corpusURI", corpusURI.toString());
		
				
		Record record;
		try {
			record = new Record(key, properties.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			log.error("Unable to create record", e);
			return null;
		}
				
		getLogAppender().append(key, record);
		
		// XXX: mark Model Training as scheduled
		
		return key;
	}
}
