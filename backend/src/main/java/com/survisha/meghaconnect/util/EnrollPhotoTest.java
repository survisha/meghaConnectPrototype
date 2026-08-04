package com.survisha.meghaconnect.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EnrollPhotoTest {

	public static void main(String[] args) throws Exception
	{
		new EnrollPhotoTest().startTest();
	}
	
	public void startTest() throws Exception
	{

		EnrollRequestJson reqObj = new EnrollRequestJson();
		reqObj.setApiKey("787f049b-199d-4a99-9656-4656c84386a8");
		reqObj.setClientId("MEGHALAYA_CMD");
		reqObj.setAppId("CMD");
		reqObj.setEnrollmentId("1232");
		reqObj.setName("Narsingh");
		reqObj.setLat(0.01);
		reqObj.setLon(0.01);
		reqObj.setPhoto(Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get("D:/FamilyDetails/Voter/Harika/Harika.jpg"))));
		sendRequest(reqObj);
	}
	
	public void sendRequest(EnrollRequestJson reqObj) throws Exception
	{
		
		String jsonReq  = new Gson().toJson(reqObj);
		
		//System.out.println("SENDING *****");
		OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(60, TimeUnit.SECONDS)
				  .build();
				MediaType mediaType = MediaType.parse("application/json");
				RequestBody body = RequestBody.create(mediaType, jsonReq);
				Request request = new Request.Builder()
//				  .url("http://127.0.0.1:8080/DeepFaceService/enroll")
				  .url("https://prdev.onlineipv.com/DeepFaceService/enroll")
//				  .url("https://staging.aadhaarkyc.com/DeepFaceService/enroll")
				  .method("POST", body)
				  .addHeader("Content-Type", "application/json")
				  .build();
				Response response = client.newCall(request).execute();
				
		String resJson = response.body().string();
		
		if(resJson.contains("error") == false)
		{
			System.out.println("*** INVALID RESPONSE ***");
			System.out.println(resJson);
			return;
		}
		
		EnrollResponseJson  resObj = new Gson().fromJson(resJson, EnrollResponseJson.class);

		if(resObj.isError())
		{
			//System.out.println(resJson);
			System.out.println("Error Code :" + resObj.getErrorCode() + " , " + resObj.getErrorDesc());
			return;
		}
		else
		{
			System.out.println("Enrolled Successfully!");
		}
	}
}
