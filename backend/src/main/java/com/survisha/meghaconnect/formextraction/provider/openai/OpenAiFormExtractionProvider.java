package com.survisha.meghaconnect.formextraction.provider.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.formextraction.config.FormExtractionProperties;
import com.survisha.meghaconnect.formextraction.exception.FormExtractionException;
import com.survisha.meghaconnect.formextraction.provider.*;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.*;

@Component
public class OpenAiFormExtractionProvider implements AIFormExtractionProvider {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final FormExtractionProperties properties;
    private final VisitorFormExtractionPromptBuilder promptBuilder;
    private final VisitorFormExtractionSchema schema;
    private final ProviderResultMapper resultMapper;

    public OpenAiFormExtractionProvider(@Qualifier("openAiFormExtractionOkHttpClient") OkHttpClient client,
            ObjectMapper objectMapper, FormExtractionProperties properties,
            VisitorFormExtractionPromptBuilder promptBuilder, VisitorFormExtractionSchema schema,
            ProviderResultMapper resultMapper) {
        this.client=client; this.objectMapper=objectMapper; this.properties=properties;
        this.promptBuilder=promptBuilder; this.schema=schema; this.resultMapper=resultMapper;
    }

    @Override public AIProviderType getProviderType() { return AIProviderType.OPENAI; }

    @Override public VisitorFormExtractionResult extract(FormExtractionInput input) {
        validate();
        var config=properties.getOpenai();
        String dataUrl="data:"+input.getMimeType()+";base64,"+Base64.getEncoder().encodeToString(input.getImageBytes());
        Map<String,Object> payload=new LinkedHashMap<>();
        payload.put("model",config.getModel());
        payload.put("instructions",promptBuilder.systemInstruction(input));
        payload.put("input",List.of(Map.of("role","user","content",List.of(
                Map.of("type","input_text","text",promptBuilder.userInstruction()),
                Map.of("type","input_image","image_url",dataUrl,"detail",config.getImageDetail())))));
        payload.put("text",Map.of("format",Map.of("type","json_schema","name","visitor_form_extraction",
                "strict",true,"schema",schema.jsonSchema())));
        payload.put("max_output_tokens",config.getMaxOutputTokens());
        payload.put("store",config.isStoreResponse());
        long started=System.nanoTime();
        for(int attempt=0;attempt<2;attempt++) {
            try {
                Request request=new Request.Builder().url(join(config.getBaseUrl(),config.getResponsesPath()))
                        .header("Authorization","Bearer "+config.getApiKey())
                        .post(RequestBody.create(objectMapper.writeValueAsString(payload),JSON)).build();
                try(Response response=client.newCall(request).execute()) {
                    String body=response.body()==null?null:response.body().string();
                    if(transientStatus(response.code())&&attempt==0) continue;
                    if(!response.isSuccessful()) throw status(response.code());
                    if(body==null||body.isBlank()) throw invalid("OpenAI returned an empty response.");
                    JsonNode root=objectMapper.readTree(body);
                    JsonNode output=structuredOutput(root);
                    return resultMapper.map(output,getProviderType(),config.getModel(),text(root,"id"),
                            (System.nanoTime()-started)/1_000_000);
                }
            } catch(SocketTimeoutException ex) {
                if(attempt==0) continue;
                throw new FormExtractionException("FORM_EXTRACTION_TIMEOUT","Form extraction timed out.",504,ex);
            } catch(JsonProcessingException ex) {
                throw new FormExtractionException("FORM_EXTRACTION_PROVIDER_INVALID_RESPONSE","OpenAI returned malformed extraction data.",502,ex);
            } catch(IOException ex) {
                if(attempt==0) continue;
                throw new FormExtractionException("FORM_EXTRACTION_PROVIDER_UNAVAILABLE","The configured form extraction provider is unavailable.",503,ex);
            }
        }
        throw new FormExtractionException("FORM_EXTRACTION_PROVIDER_UNAVAILABLE","The configured form extraction provider is unavailable.",503);
    }

    private JsonNode structuredOutput(JsonNode root) throws JsonProcessingException {
        for(JsonNode output:root.path("output")) for(JsonNode content:output.path("content")) {
            if("refusal".equals(content.path("type").asText()))
                throw new FormExtractionException("FORM_EXTRACTION_REFUSED","The image could not be processed.",422);
            if("output_text".equals(content.path("type").asText())&&content.hasNonNull("text"))
                return objectMapper.readTree(content.get("text").asText());
        }
        throw invalid("OpenAI returned incomplete structured extraction content.");
    }
    private void validate() {
        var c=properties.getOpenai();
        if(!c.isEnabled()||blank(c.getBaseUrl())||blank(c.getApiKey())||blank(c.getModel())||blank(c.getResponsesPath()))
            throw new FormExtractionException("FORM_EXTRACTION_CONFIGURATION_INVALID","OpenAI form extraction is not configured.",503);
        try { URI.create(c.getBaseUrl()).toURL(); } catch(Exception ex) {
            throw new FormExtractionException("FORM_EXTRACTION_CONFIGURATION_INVALID","OpenAI base URL is invalid.",503);
        }
    }
    private FormExtractionException status(int code) {
        if(code==429) return new FormExtractionException("FORM_EXTRACTION_RATE_LIMITED","Form extraction is temporarily rate limited.",429);
        if(code==408||code==504) return new FormExtractionException("FORM_EXTRACTION_TIMEOUT","Form extraction timed out.",504);
        if(code>=500) return new FormExtractionException("FORM_EXTRACTION_PROVIDER_UNAVAILABLE","The configured form extraction provider is unavailable.",503);
        return new FormExtractionException("FORM_EXTRACTION_PROVIDER_REJECTED","The configured form extraction provider rejected the request.",502);
    }
    private boolean transientStatus(int c) { return c==429||c==502||c==503||c==504; }
    private FormExtractionException invalid(String m) { return new FormExtractionException("FORM_EXTRACTION_PROVIDER_INVALID_RESPONSE",m,502); }
    private String join(String b,String p) { return (b.endsWith("/")?b.substring(0,b.length()-1):b)+(p.startsWith("/")?p:"/"+p); }
    private String text(JsonNode n,String f) { return n.hasNonNull(f)?n.get(f).asText():null; }
    private boolean blank(String v) { return v==null||v.isBlank(); }
}
