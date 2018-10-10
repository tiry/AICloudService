package org.nuxeo.ai.model.train.stream;

import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.nuxeo.ai.model.train.AbstractModelTrainer;
import org.nuxeo.ai.model.train.JobStatus;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

public class StreamModelTrainer extends AbstractModelTrainer {


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
					LogManager logManager = service.getLogManager(getLoggerStreamConfig());
					logManager.createIfNotExists(DEFAULT_TRAIN_SCHEDULE_NAME, 1);
					appender = logManager.getAppender(DEFAULT_TRAIN_SCHEDULE_NAME);
				}
			}
		}
		return appender;
	}

	
	protected String encodeURIs(List<URI> URIs) {		
		StringBuffer sb = new StringBuffer();		
		for (URI uri: URIs) {
			sb.append(uri.toString());
			sb.append(",");
		}
		return sb.toString();		
	}

	
	protected String startTraining(DocumentModel model, URI modelURI, List<URI> trainingURIs, List<URI> evaluationURIs) {
		String key = UUID.randomUUID().toString();

		Properties properties = new Properties();

		properties.put("modelUUID", model.getId());
		properties.put("modelURI", modelURI.toString());
		properties.put("trainURIs", encodeURIs(trainingURIs));
		properties.put("evaluationURIs", encodeURIs(evaluationURIs));
		
		Record record;
		try {
			StringWriter writer = new StringWriter();
			properties.store(writer, null);
			writer.flush();
			record = new Record(key, writer.toString().getBytes("UTF-8"));
		} catch (Exception e) {
			log.error("Unable to create record", e);
			return null;
		}

		getLogAppender().append(key, record);

		return key;
		
	}
	
	
	@Override
	public List<JobStatus> listJobStatus() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getName() {		
		return "stream";
	}

}
