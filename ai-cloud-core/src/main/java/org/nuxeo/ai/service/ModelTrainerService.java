package org.nuxeo.ai.service;

import java.util.List;

import org.nuxeo.ai.sagemaker.JobStatus;

/**
 * 
 * @author PedroCardoso 
 *
 */
public interface ModelTrainerService {
	
    public String train(String ModelId, String[] aiCorpusIds, String[] aiCorpusEvIds);

    public List<JobStatus> listJobStatus();
}
