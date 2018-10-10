package org.nuxeo.ai.model.train.stream;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ai.blob.AIBlobHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class AITrainedModelComputation extends AbstractComputation {

	protected static final Log log = LogFactory.getLog(AITrainedModelComputation.class);

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
		final String blobURI = properties.getProperty("modelBlob");
		final String trainingUUID = properties.getProperty("trainingUUID");
		

		RepositoryManager rm = Framework.getService(RepositoryManager.class);
		UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(rm.getDefaultRepositoryName()) {

			@Override
			public void run() {

				DocumentModel model = session.getDocument(new IdRef(uuid));
				DocumentModel training = session.getDocument(new IdRef(trainingUUID));
				try {

					Blob blob = AIBlobHelper.resolveBlobFromURI(session, new URI(blobURI));
					model.setPropertyValue("file:content", (Serializable) blob);
					model.setPropertyValue("dc:nature", "trained");
					
					model.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);

					session.saveDocument(model);
					
					training.setPropertyValue("ait:trainingState", "completed");
					session.saveDocument(training);
					
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
