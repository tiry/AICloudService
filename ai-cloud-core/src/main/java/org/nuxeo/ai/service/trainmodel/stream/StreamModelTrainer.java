package org.nuxeo.ai.service.trainmodel.stream;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ai.service.AIBlobHelper;
import org.nuxeo.ai.service.trainmodel.ModelTrainer;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
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
					LogManager logManager = service.getLogManager(getLoggerStreamConfig());
					logManager.createIfNotExists(DEFAULT_TRAIN_SCHEDULE_NAME, 1);
					appender = logManager.getAppender(DEFAULT_TRAIN_SCHEDULE_NAME);
				}
			}
		}
		return appender;
	}


	public String scheduleTraining(DocumentModel training) {

		String modelUUID = (String) training.getPropertyValue("ait:srcModel");
		String datasetUUID = (String) training.getPropertyValue("ait:srcDataset");

		DocumentModel model = training.getCoreSession().getDocument(new IdRef(modelUUID));
		DocumentModel dataset = training.getCoreSession().getDocument(new IdRef(datasetUUID));

		URI modelURI = AIBlobHelper.getBlobURI(model, "file:content");
		URI corpusURI = AIBlobHelper.getBlobURI(dataset, "file:content");

		String key = UUID.randomUUID().toString();

		Properties properties = new Properties();

		properties.put("modelUUID", model.getId());
		properties.put("modelURI", modelURI.toString());
		properties.put("corpusURI", corpusURI.toString());

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

		// mark Model Training as scheduled
		training.setPropertyValue("ait:trainingState", "scheduled");
		training.getCoreSession().saveDocument(training);

		return key;
	}
}
