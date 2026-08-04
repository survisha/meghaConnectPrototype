package com.survisha.meghaconnect.util;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EnrollCheckRequestJson {

	@SerializedName("apiKey")
	@Expose
	private String apiKey;
	@SerializedName("clientId")
	@Expose
	private String clientId;
	@SerializedName("enrollmentId")
	@Expose
	private String enrollmentId;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getEnrollmentId() {
		return enrollmentId;
	}

	public void setEnrollmentId(String enrollmentId) {
		this.enrollmentId = enrollmentId;
	}
}