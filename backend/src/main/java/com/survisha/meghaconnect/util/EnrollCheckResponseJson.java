package com.survisha.meghaconnect.util;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EnrollCheckResponseJson {

	@SerializedName("error")
	@Expose
	private boolean error;
	@SerializedName("errorCode")
	@Expose
	private String errorCode;
	@SerializedName("errorDesc")
	@Expose
	private String errorDesc;
	@SerializedName("enrolled")
	@Expose
	private boolean enrolled;

	public EnrollCheckResponseJson()
	{
		
	}
	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorDesc() {
		return errorDesc;
	}

	public void setErrorDesc(String errorDesc) {
		this.errorDesc = errorDesc;
	}

	public boolean isEnrolled() {
		return enrolled;
	}
	
	public void setEnrolled(boolean enrolled) {
		this.enrolled = enrolled;
	}
}