package com.survisha.meghaconnect.formextraction.provider.ollama;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.formextraction.config.FormExtractionProperties;
import com.survisha.meghaconnect.formextraction.exception.FormExtractionException;
import com.survisha.meghaconnect.formextraction.provider.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class OllamaFormExtractionProvider implements AIFormExtractionProvider {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final FormExtractionProperties properties;
    private final VisitorFormExtractionPromptBuilder promptBuilder;
    private final VisitorFormExtractionSchema schema;
    private final ProviderResultMapper resultMapper;

    public OllamaFormExtractionProvider(@Qualifier("ollamaFormExtractionOkHttpClient") OkHttpClient client,
            ObjectMapper objectMapper, FormExtractionProperties properties,
            VisitorFormExtractionPromptBuilder promptBuilder, VisitorFormExtractionSchema schema,
            ProviderResultMapper resultMapper) {
        this.client=client; this.objectMapper=objectMapper; this.properties=properties;
        this.promptBuilder=promptBuilder; this.schema=schema; this.resultMapper=resultMapper;
    }

    @Override public AIProviderType getProviderType() { return AIProviderType.OLLAMA; }

    @Override public VisitorFormExtractionResult extract(FormExtractionInput input) {
        validate();
        var config = properties.getOllama();
        String base64 = Base64.getEncoder().encodeToString(input.getImageBytes());
        OllamaChatRequest payload = new OllamaChatRequest(config.getModel(), false, List.of(
                new OllamaChatRequest.OllamaMessage("system", promptBuilder.systemInstruction(input), null),
                new OllamaChatRequest.OllamaMessage("user", promptBuilder.userInstruction(), List.of(base64))
        ), schema.jsonSchema(), new OllamaChatRequest.OllamaOptions(config.getTemperature()), config.getKeepAlive());
        long started=System.nanoTime();
        try {
            Request request = new Request.Builder().url(join(config.getBaseUrl(),config.getChatPath()))
                    .post(RequestBody.create(objectMapper.writeValueAsString(payload),JSON))
                    .header("Content-Type","application/json").build();
            try(Response response=client.newCall(request).execute()) {
                if(!response.isSuccessful()) throw status(response.code());
                String body=response.body()==null?null:response.body().string();
                if(body==null||body.isBlank()) throw invalid("Ollama returned an empty response.");
                OllamaChatResponse ollama=objectMapper.readValue(body,OllamaChatResponse.class);
                if(ollama.getMessage()==null||ollama.getMessage().getContent()==null||ollama.getMessage().getContent().isBlank())
                    throw invalid("Ollama returned blank extraction content.");
                JsonNode output=objectMapper.readTree(ollama.getMessage().getContent());
                return resultMapper.map(output,getProviderType(),ollama.getModel()==null?config.getModel():ollama.getModel(),
                        null,(System.nanoTime()-started)/1_000_000);
            }
        } catch(SocketTimeoutException ex) {
            throw new FormExtractionException("FORM_EXTRACTION_TIMEOUT","Form extraction timed out.",504,ex);
        } catch(JsonProcessingException ex) {
            throw new FormExtractionException("FORM_EXTRACTION_PROVIDER_INVALID_RESPONSE","Ollama returned malformed extraction data.",502,ex);
        } catch(IOException ex) {
            throw new FormExtractionException("FORM_EXTRACTION_PROVIDER_UNAVAILABLE","The configured form extraction provider is unavailable.",503,ex);
        }
    }

    private void validate() {
        var c=properties.getOllama();
        if(!c.isEnabled()||blank(c.getBaseUrl())||blank(c.getModel())||blank(c.getChatPath()))
            throw new FormExtractionException("FORM_EXTRACTION_CONFIGURATION_INVALID","Ollama form extraction is not configured.",503);
        try { URI.create(c.getBaseUrl()).toURL(); } catch(Exception ex) {
            throw new FormExtractionException("FORM_EXTRACTION_CONFIGURATION_INVALID","Ollama base URL is invalid.",503);
        }
    }
    private FormExtractionException status(int code) {
        if(code==404) return new FormExtractionException("FORM_EXTRACTION_MODEL_NOT_AVAILABLE","Configured Ollama model or endpoint is unavailable.",503);
        if(code==408||code==504) return new FormExtractionException("FORM_EXTRACTION_TIMEOUT","Form extraction timed out.",504);
        if(code>=500) return new FormExtractionException("FORM_EXTRACTION_PROVIDER_UNAVAILABLE","The configured form extraction provider is unavailable.",503);
        return new FormExtractionException("FORM_EXTRACTION_PROVIDER_REJECTED","The configured form extraction provider rejected the request.",502);
    }
    private FormExtractionException invalid(String message) { return new FormExtractionException("FORM_EXTRACTION_PROVIDER_INVALID_RESPONSE",message,502); }
    private String join(String base,String path) { return (base.endsWith("/")?base.substring(0,base.length()-1):base)+(path.startsWith("/")?path:"/"+path); }
    private boolean blank(String value) { return value==null||value.isBlank(); }
}
