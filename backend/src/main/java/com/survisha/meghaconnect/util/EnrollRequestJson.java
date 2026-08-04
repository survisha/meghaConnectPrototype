package com.survisha.meghaconnect.util;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EnrollRequestJson {

	@SerializedName("apiKey")
	@Expose
	private String apiKey;
	@SerializedName("clientId")
	@Expose
	private String clientId;
	@SerializedName("appId")
	@Expose
	private String appId;
	@SerializedName("enrollmentId")
	@Expose
	private String enrollmentId;
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("photo")
	@Expose
	private String photo;
	
	@SerializedName("lat")
	private double lat;

	@SerializedName("lon")
	private double lon;

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhoto() {
		return photo;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}
}