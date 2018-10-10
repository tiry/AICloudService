package org.nuxeo.ai.model.train;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * 
 * @author PedroCardoso 
 *
 */
public interface ModelTrainer {
	
	public String getName();
	
    public String train(DocumentModel model);

    public List<JobStatus> listJobStatus();
}
