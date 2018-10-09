package org.nuxeo.ai.sagemaker;

import java.util.Calendar;
import java.util.Date;

public class JobStatus {
	
	protected final String urn;
	
	public JobStatus(String urn) {
		this.urn=urn;
	}
	protected String status;
	
	protected Date startDate;
	
	protected Date endDate;

	public String getUrn() {
		return urn;
	}

	public String getStatus() {
		return status;
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	
}
