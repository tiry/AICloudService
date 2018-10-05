package org.nuxeo.ai.service.train;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;

public class AITrainedModelComputation extends AbstractComputation{

	public AITrainedModelComputation(String name, int nbInputStreams, int nbOutputStreams) {
		super(name, nbInputStreams, nbOutputStreams);
	}

	@Override
	public void processRecord(ComputationContext ctx, String key, Record record) {
		
		String data = new String(record.getData());
		
		Properties properties = new Properties();
		
		try {
			properties.load(new StringReader(data));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		final String uuid = properties.getProperty("modelUUID");
		final String modelURI = properties.getProperty("modelURI");
		
		RepositoryManager rm = Framework.getService(RepositoryManager.class);
		UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(rm.getDefaultRepositoryName()) {
			
			@Override
			public void run() {
				
				DocumentModel model = session.getDocument(new IdRef(uuid));
				
				try {
					Blob blob = new URLBlob(new URL(modelURI));
					model.setPropertyValue("file:content", (Serializable)blob);
					model.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);

					session.saveDocument(model);
					   
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		};
		
	}

	
}
