package com.survisha.meghaconnect.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.survisha.meghaconnect.epic.face.dto.provider.FaceSearch1NProviderRequest;
import com.survisha.meghaconnect.epic.face.dto.provider.FaceSearch1NProviderResponse;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Verify1NTest {

	public static void main(String[] args) throws IOException {
	
		FaceSearch1NProviderRequest req = new FaceSearch1NProviderRequest();
		req.setApiKey("83ayvsexMCtchWGR1SwAT9D77Ps2TCPC");
		req.setPhoto(Base64.getEncoder().encodeToString( Files.readAllBytes(Paths.get("D:/Harika/1.jpeg"))));
		
		
		String jsonReq  = new Gson().toJson(req);

		OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(600000, TimeUnit.SECONDS)
				  .build();
				MediaType mediaType = MediaType.parse("application/json");
				RequestBody body = RequestBody.create(mediaType, jsonReq);
				Request faceRequest = new Request.Builder()
				  .url("https://prdev.onlineipv.com/MeghalayaEPICFaceMW/FaceService1N")
				  .method("POST", body)
				  .addHeader("Content-Type", "application/json")
				  .build();

		String resJson = null;
		int httpStatus = 0;

		// try-with-resources: the okhttp Response must be closed or the connection leaks.
		try (Response resp = client.newCall(faceRequest).execute()) {
			httpStatus = resp.code();
			resJson = resp.body() == null ? null : resp.body().string();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// A genuine engine response always carries the "error" flag. Anything else
		// (empty body, HTML error page, gateway response) is not something we can read.
		JsonObject resTree = null;
		try
		{
			resTree = JsonParser.parseString(resJson).getAsJsonObject();
		}catch(Exception ex)
		{
			resTree = null;
		}

		if(resTree == null || resTree.has("error") == false)
		{
			System.out.println("*** INVALID RESPONSE *** (HTTP " + httpStatus + ")");
			System.out.println(resJson);
			return;
		}

		FaceSearch1NProviderResponse resObj = new Gson().fromJson(resTree, FaceSearch1NProviderResponse.class);

		if(resObj.isError())
		{
			System.out.println("Error Code :" + resObj.getErrorCode() + " , " + resObj.getErrorDesc());
			return;
		}
		else
		{
		
			if(resObj.isMatched())
			{
				System.out.println("FACE MATCHED\n\n");
				
				System.out.println("EPIC Number: " + resObj.getEpicNumber());
				System.out.println("Name: " + resObj.getName());
				System.out.println("Address: " + resObj.getAddress());

				
				System.out.println("Serial  Number: " + resObj.getSerialNumber());
				System.out.println("Part Number: " + resObj.getPartNumber());
				System.out.println("Part Name: " + resObj.getPartName());
				System.out.println("AC PC Name: " + resObj.getAcpcName());
				System.out.println("District: " + resObj.getDistrict());
				System.out.println("Pincode: " + resObj.getPincode());

				Files.write(Paths.get("/Users/praburaju/P_DRIVE/Projects/ProjectSurvisha/ProjectMeghalayaEPIC/Middleware/eclipse/TestProject/photo.png"), Base64.getDecoder().decode(resObj.getPhoto()));
				
			}
			else
			{
				System.out.println("FACE NOT FOUND\n\n");
				
			}
		}
	}	
}
