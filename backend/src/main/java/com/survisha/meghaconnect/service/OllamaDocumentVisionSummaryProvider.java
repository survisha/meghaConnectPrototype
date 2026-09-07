package com.survisha.meghaconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.config.AiNotesProperties;
import com.survisha.meghaconnect.formextraction.config.FormExtractionProperties;
import com.survisha.meghaconnect.formextraction.provider.ollama.OllamaChatRequest;
import com.survisha.meghaconnect.formextraction.provider.ollama.OllamaChatResponse;
import com.survisha.meghaconnect.util.RequestContextUtil;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component @Slf4j
public class OllamaDocumentVisionSummaryProvider implements DocumentVisionSummaryProvider {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client; private final ObjectMapper mapper; private final FormExtractionProperties formProperties; private final AiNotesProperties notesProperties;
    public OllamaDocumentVisionSummaryProvider(@Qualifier("ollamaFormExtractionOkHttpClient") OkHttpClient client, ObjectMapper mapper, FormExtractionProperties formProperties, AiNotesProperties notesProperties) {
        this.client=client; this.mapper=mapper; this.formProperties=formProperties; this.notesProperties=notesProperties;
    }

    @Override public VisionSummary summarize(List<byte[]> images) {
        if(images==null||images.isEmpty()) throw new IllegalArgumentException("At least one document image is required.");
        String model=notesProperties.getVisionModel();
        List<String> encoded=images.stream().map(x->Base64.getEncoder().encodeToString(x)).toList();
        String prompt="""
                Analyze the attached supporting document image(s) for a government appointment record. Describe the actual visible content, including document type, names, IDs, dates, addresses, departments, schemes, petition or proposal details, and actionable information where clearly readable. Handwriting may be interpreted cautiously. Treat signatures and stamps only as visible observations, never as authentication. If details are unclear, say they are unreadable. Do not invent values. Return JSON only.
                """;
        Map<String,Object> schema=Map.of("type","object","properties",Map.of(
                "aiSummary",Map.of("type","string"),"importantDetails",Map.of("type","string"),
                "missingInfo",Map.of("type","string"),"riskFlags",Map.of("type","string")),
                "required",List.of("aiSummary","importantDetails","missingInfo","riskFlags"));
        var config=formProperties.getOllama();
        OllamaChatRequest payload=new OllamaChatRequest(model,false,List.of(
                new OllamaChatRequest.OllamaMessage("system","You produce concise factual document notes for Meghalaya government officers.",null),
                new OllamaChatRequest.OllamaMessage("user",prompt,encoded)),schema,
                new OllamaChatRequest.OllamaOptions(0,Math.max(500,config.getNumPredict()),config.getNumCtx()),config.getKeepAlive());
        long started=System.nanoTime();
        log.info("AI notes vision request started model={} requestId={} images={} totalImageBytes={}",model,RequestContextUtil.getRequestId(),images.size(),images.stream().mapToInt(x->x.length).sum());
        try {
            Request request=new Request.Builder().url(join(config.getBaseUrl(),config.getChatPath())).post(RequestBody.create(mapper.writeValueAsString(payload),JSON)).build();
            try(Response response=client.newCall(request).execute()) {
                String body=response.body()==null?null:response.body().string();
                if(!response.isSuccessful()||body==null||body.isBlank()) throw new IllegalStateException("Vision provider returned HTTP "+response.code()+".");
                OllamaChatResponse envelope=mapper.readValue(body,OllamaChatResponse.class);
                if(envelope.getMessage()==null||envelope.getMessage().getContent()==null) throw new IllegalStateException("Vision provider returned no content.");
                String content=jsonContent(envelope.getMessage().getContent()); JsonNode json=mapper.readTree(content); long duration=(System.nanoTime()-started)/1_000_000;
                String actual=envelope.getModel()==null||envelope.getModel().isBlank()?model:envelope.getModel();
                log.info("AI notes vision response completed model={} requestId={} durationMs={}",actual,RequestContextUtil.getRequestId(),duration);
                return new VisionSummary(value(json,"aiSummary"),value(json,"importantDetails"),value(json,"missingInfo"),value(json,"riskFlags"),actual,content,duration);
            }
        } catch(IOException e){throw new IllegalStateException("Vision AI service is unavailable or returned invalid data.",e);}
    }
    private String value(JsonNode n,String field){JsonNode v=n.get(field);return v==null?"":v.asText("").trim();}
    private String jsonContent(String s){String v=s.trim();if(v.startsWith("```")){int a=v.indexOf('\n'),b=v.lastIndexOf("```");if(a>=0&&b>a)v=v.substring(a+1,b).trim();}int a=v.indexOf('{'),b=v.lastIndexOf('}');return a>=0&&b>a?v.substring(a,b+1):v;}
    private String join(String base,String path){return(base.endsWith("/")?base.substring(0,base.length()-1):base)+(path.startsWith("/")?path:"/"+path);}
}
