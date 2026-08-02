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
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

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
        ), schema.jsonSchema(), new OllamaChatRequest.OllamaOptions(config.getTemperature(),config.getNumPredict(),config.getNumCtx()), config.getKeepAlive());
        long started=System.nanoTime();
        log.info("Ollama form extraction request started provider=ollama model={} requestId={} imageSizeBytes={}",
                config.getModel(), input.getRequestId(), input.getImageBytes().length);
        try {
            Request request = new Request.Builder().url(join(config.getBaseUrl(),config.getChatPath()))
                    .post(RequestBody.create(objectMapper.writeValueAsString(payload),JSON))
                    .header("Content-Type","application/json").build();
            try(Response response=client.newCall(request).execute()) {
                String body=response.body()==null?null:response.body().string();
                log.info("Ollama form extraction response received provider=ollama model={} requestId={} elapsedMs={} httpOutcome={}",
                        config.getModel(), input.getRequestId(), (System.nanoTime()-started)/1_000_000, response.code());
                if(!response.isSuccessful()) throw status(response.code(), body);
                if(body==null||body.isBlank()) throw invalid("Ollama returned an empty response.");
                OllamaChatResponse ollama=objectMapper.readValue(body,OllamaChatResponse.class);
                if(ollama.getMessage()==null||ollama.getMessage().getContent()==null||ollama.getMessage().getContent().isBlank())
                    throw invalid("Ollama returned blank extraction content.");
                JsonNode output=objectMapper.readTree(jsonContent(ollama.getMessage().getContent()));
                return resultMapper.map(output,getProviderType(),ollama.getModel()==null?config.getModel():ollama.getModel(),
                        null,(System.nanoTime()-started)/1_000_000);
            }
        } catch(FormExtractionException ex) {
            logFailure(input, started, "provider-response", ex);
            throw ex;
        } catch(SocketTimeoutException ex) {
            String phase = timeoutPhase(ex, started, config);
            FormExtractionException mapped = timeoutException(phase, ex);
            logFailure(input, started, phase, ex);
            throw mapped;
        } catch(ConnectException ex) {
            FormExtractionException mapped = new FormExtractionException("FORM_EXTRACTION_PROVIDER_UNREACHABLE",
                    "The form extraction service cannot be reached. Please try again.",503,ex);
            logFailure(input, started, "connect", ex);
            throw mapped;
        } catch(InterruptedIOException ex) {
            FormExtractionException mapped = new FormExtractionException("FORM_EXTRACTION_TIMEOUT",
                    "Form extraction is taking longer than expected. Please try again.",504,ex);
            logFailure(input, started, "call", ex);
            throw mapped;
        } catch(JsonProcessingException ex) {
            logFailure(input, started, "response-parse", ex);
            throw new FormExtractionException("FORM_EXTRACTION_PROVIDER_INVALID_RESPONSE","Ollama returned malformed extraction data.",502,ex);
        } catch(IOException ex) {
            logFailure(input, started, "transport", ex);
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
    private FormExtractionException status(int code, String responseBody) {
        String error = responseBody == null ? "" : responseBody.toLowerCase(Locale.ROOT);
        if (error.contains("model") && (error.contains("not found") || error.contains("not installed")))
            return new FormExtractionException("FORM_EXTRACTION_MODEL_NOT_INSTALLED","The configured extraction model is not installed.",503);
        if (error.contains("out of memory") || error.contains("insufficient memory") || error.contains("cuda error"))
            return new FormExtractionException("FORM_EXTRACTION_INSUFFICIENT_MEMORY","The form extraction service does not have enough memory.",503);
        if(code==404) return new FormExtractionException("FORM_EXTRACTION_MODEL_NOT_INSTALLED","The configured extraction model is not installed.",503);
        if(code==408||code==504) return new FormExtractionException("FORM_EXTRACTION_TIMEOUT","Form extraction is taking longer than expected. Please try again.",504);
        if(code>=500) return new FormExtractionException("FORM_EXTRACTION_PROVIDER_UNAVAILABLE","The configured form extraction provider is unavailable.",503);
        return new FormExtractionException("FORM_EXTRACTION_PROVIDER_REJECTED","The configured form extraction provider rejected the request.",502);
    }
    private String timeoutPhase(SocketTimeoutException ex, long started, FormExtractionProperties.Ollama config) {
        String trace = stackTraceText(ex).toLowerCase(Locale.ROOT);
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
        long elapsedMs = (System.nanoTime()-started)/1_000_000;
        if (message.contains("connect") || trace.contains("connectsocket")) return "connect";
        if (trace.contains("requestbody") || trace.contains("knownlengthsink") || trace.contains("write")) return "upload";
        if (elapsedMs >= config.getCallTimeoutSeconds()*1000L-1000L) return "call";
        return "inference";
    }
    private FormExtractionException timeoutException(String phase, Exception ex) {
        String message = "Form extraction is taking longer than expected. Please try again.";
        if ("connect".equals(phase)) return new FormExtractionException("FORM_EXTRACTION_PROVIDER_UNREACHABLE",message,503,ex);
        if ("upload".equals(phase)) return new FormExtractionException("FORM_EXTRACTION_UPLOAD_TIMEOUT",message,504,ex);
        if ("inference".equals(phase)) return new FormExtractionException("FORM_EXTRACTION_INFERENCE_TIMEOUT",message,504,ex);
        return new FormExtractionException("FORM_EXTRACTION_TIMEOUT",message,504,ex);
    }
    private void logFailure(FormExtractionInput input, long started, String phase, Exception ex) {
        log.warn("Visitor form extraction failed provider=ollama model={} requestId={} imageSizeBytes={} elapsedMs={} phase={} exception={}",
                properties.getOllama().getModel(), input.getRequestId(), input.getImageBytes().length,
                (System.nanoTime()-started)/1_000_000, phase, ex.getClass().getName());
    }
    private String stackTraceText(Throwable throwable) {
        StringBuilder value = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) value.append(element.toString()).append('\n');
        return value.toString();
    }
    private String jsonContent(String content) {
        String value=content.trim();
        if(value.startsWith("```")) {
            int newline=value.indexOf('\n'); int closing=value.lastIndexOf("```");
            if(newline>=0 && closing>newline) value=value.substring(newline+1,closing).trim();
        }
        if(!value.startsWith("{")) {
            int start=value.indexOf('{'), end=value.lastIndexOf('}');
            if(start>=0 && end>start) value=value.substring(start,end+1);
        }
        return value;
    }
    private FormExtractionException invalid(String message) { return new FormExtractionException("FORM_EXTRACTION_PROVIDER_INVALID_RESPONSE",message,502); }
    private String join(String base,String path) { return (base.endsWith("/")?base.substring(0,base.length()-1):base)+(path.startsWith("/")?path:"/"+path); }
    private boolean blank(String value) { return value==null||value.isBlank(); }
}
