package org.nuxeo.ai.model.train.stream;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ai.model.train.ModelTrainer;
import org.nuxeo.ai.service.AICloudService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class AITrainedModelComputation extends AbstractComputation {

	protected static final Log log = LogFactory.getLog(AITrainedModelComputation.class);

	protected ModelTrainer getTrainer() {
		return Framework.getService(AICloudService.class).getTrainer(StreamModelTrainer.NAME);
	}

	public AITrainedModelComputation(String name, Map<String, String> options) {
		super(name, 1, 0);
	}

	@Override
	public void processRecord(ComputationContext ctx, String key, Record record) {

		String data = new String(record.getData());

		Properties properties = new Properties();

		try {
			properties.load(new StringReader(data));
		} catch (IOException e) {
			log.error("Unable to read training completed message", e);
			return;
		}

		final String uuid = properties.getProperty("modelUUID");
		final String blobURI = properties.getProperty("modelURI");

		RepositoryManager rm = Framework.getService(RepositoryManager.class);
		UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(rm.getDefaultRepositoryName()) {

			@Override
			public void run() {

				DocumentModel model = session.getDocument(new IdRef(uuid));
				try {
					getTrainer().updateModelAfterTraining(model, new URI(blobURI));
				} catch (Exception e) {
					log.error("Unable to process training completed message", e);
				}
			}
		};

		TransactionHelper.startTransaction();
		try {
			runner.runUnrestricted();
		} finally {
			TransactionHelper.commitOrRollbackTransaction();
		}
		ctx.askForCheckpoint();
	}

}
