package com.survisha.meghaconnect.util;

import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeleteTest {

	public static void main(String[] args) throws Exception
	{
		DeleteRequestJson reqObj = new DeleteRequestJson();
		reqObj.setApiKey("b1dc3d57-1976-4799-8e22-cb6d71905dc0");
		reqObj.setClientId("MEGHALAYA");
		reqObj.setAppId("CMD");
		
		reqObj.setId("GBZ8946857");

		String jsonReq  = new Gson().toJson(reqObj);
		
		System.out.println(jsonReq);
		System.out.println("SENDING *****");
		OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(60, TimeUnit.SECONDS)
				  .build();
				MediaType mediaType = MediaType.parse("application/json");
				RequestBody body = RequestBody.create(mediaType, jsonReq);
				Request request = new Request.Builder()
//				  .url("http://127.0.0.1:8080/DeepFaceService/verify")
//				  .url("https://staging.aadhaarkyc.com/DeepFaceService/delete")
				
				  .url("https://prdev.onlineipv.com/DeepFaceService/delete")
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
		
		DeleteResponseJson  resObj = new Gson().fromJson(resJson, DeleteResponseJson.class);

		if(resObj.isError())
		{
			System.out.println("Error Code :" + resObj.getErrorCode() + " , " + resObj.getErrorDesc());
			return;
		}
		else
		{
			System.out.println("Deleted Successfully");
		}
	}
}
