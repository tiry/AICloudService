package org.nuxeo.ai.service;

public class PredictionResult {

	protected int responseCode;
	
	protected String payload;
	
	protected boolean noEndPoint=false;

	public PredictionResult(int responseCode,String payload) {
		this.responseCode=responseCode;
		this.payload=payload;			
	}
	
	protected PredictionResult(boolean noEndPoint) {
		this.noEndPoint=noEndPoint;
	}	
	
	public int getResponseCode() {
		return responseCode;
	}

	public String getPayload() {
		return payload;
	}

	public boolean hasNoEndPoint() {
		return noEndPoint;
	}
	
	public static class NoEndPointResult extends PredictionResult {
		public NoEndPointResult() {
			super(false);
			responseCode=-500;
		}
	}
	
}
