package com.survisha.meghaconnect.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.config.AiNotesProperties;
import com.survisha.meghaconnect.formextraction.config.FormExtractionProperties;
import java.io.IOException;
import java.util.List;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;

class OllamaDocumentVisionSummaryProviderTest {
    @Test void sendsAllImagesToConfiguredVisionModelAndReportsActualModel() throws Exception {
        ObjectMapper mapper=new ObjectMapper(); CapturingInterceptor interceptor=new CapturingInterceptor();
        FormExtractionProperties form=new FormExtractionProperties();form.getOllama().setBaseUrl("http://localhost:11434");
        AiNotesProperties notes=new AiNotesProperties();notes.setVisionModel("qwen2.5vl:3b");
        var provider=new OllamaDocumentVisionSummaryProvider(new OkHttpClient.Builder().addInterceptor(interceptor).build(),mapper,form,notes);
        var result=provider.summarize(List.of(new byte[]{1,2,3},new byte[]{4,5}));
        JsonNode request=mapper.readTree(interceptor.requestBody);
        assertEquals("qwen2.5vl:3b",request.path("model").asText());
        assertEquals(2,request.path("messages").get(1).path("images").size());
        assertEquals("AQID",request.path("messages").get(1).path("images").get(0).asText());
        assertEquals("qwen2.5vl:7b",result.modelName());assertTrue(result.aiSummary().contains("proposal"));
    }
    private static class CapturingInterceptor implements Interceptor {
        String requestBody;
        @Override public Response intercept(Chain chain)throws IOException {Request request=chain.request();Buffer buffer=new Buffer();request.body().writeTo(buffer);requestBody=buffer.readUtf8();String content="{\"aiSummary\":\"Readable proposal document.\",\"importantDetails\":\"Vehicle supply proposal\",\"missingInfo\":\"None\",\"riskFlags\":\"None\"}";String body="{\"model\":\"qwen2.5vl:7b\",\"message\":{\"role\":\"assistant\",\"content\":"+new ObjectMapper().writeValueAsString(content)+"},\"done\":true}";return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(ResponseBody.create(body,MediaType.get("application/json"))).build();}
    }
}
