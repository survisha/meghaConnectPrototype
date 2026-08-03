package com.survisha.meghaconnect.util;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DeleteRequestJson {

	@SerializedName("apiKey")
	@Expose
	private String apiKey;
	@SerializedName("clientId")
	@Expose
	private String clientId;
	
	@SerializedName("appId")
	@Expose
	private String appId;

	@SerializedName("id")
	@Expose
	private String id;

	public DeleteRequestJson()
	{
		
	}
	
	public DeleteRequestJson(String apiKey, String clientId, String appId, String id) {
		super();
		this.apiKey = apiKey;
		this.clientId = clientId;
		this.appId = appId;
		this.id = id;
	}

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

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}