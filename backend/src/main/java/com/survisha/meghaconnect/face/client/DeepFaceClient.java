package com.survisha.meghaconnect.face.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.face.config.FaceRecognitionProperties;
import com.survisha.meghaconnect.face.exception.FaceRecognitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeepFaceClient implements FaceRecognitionClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient faceRecognitionOkHttpClient;
    private final ObjectMapper objectMapper;
    private final FaceRecognitionProperties properties;

    @Override
    public JsonNode post(String operation, Map<String, Object> payload) {
        validateConfiguration();
        long started = System.nanoTime();
        try {
            String json = objectMapper.writeValueAsString(payload);
            Request request = new Request.Builder()
                    .url(joinUrl(properties.getBaseUrl(), operation))
                    .post(RequestBody.create(json, JSON))
                    .header("Accept", "application/json")
                    .build();
            try (Response response = faceRecognitionOkHttpClient.newCall(request).execute()) {
                String body = response.body() == null ? null : response.body().string();
                long elapsedMs = (System.nanoTime() - started) / 1_000_000;
                log.info("Face provider operation={} httpStatus={} durationMs={}", operation, response.code(), elapsedMs);
                if (!response.isSuccessful()) {
                    throw provider("FACE_PROVIDER_HTTP_ERROR", "Face recognition provider rejected the request.", mapStatus(response.code()));
                }
                if (body == null || body.isBlank()) {
                    throw provider("FACE_PROVIDER_EMPTY_RESPONSE", "Face recognition provider returned an empty response.", 502);
                }
                JsonNode result;
                try {
                    result = objectMapper.readTree(body);
                } catch (JsonProcessingException ex) {
                    throw new FaceRecognitionException("FACE_PROVIDER_INVALID_RESPONSE",
                            "Face recognition provider returned an invalid response.", 502, ex);
                }
                if (!result.isObject() || !result.hasNonNull("error") || !result.get("error").isBoolean()) {
                    throw provider("FACE_PROVIDER_INVALID_RESPONSE", "Face recognition provider returned an incomplete response.", 502);
                }
                if (result.path("error").asBoolean()) {
                    String code = safeText(result, "errorCode");
                    log.warn("Face provider operation={} providerErrorCode={}", operation, code);
                    throw provider(code == null ? "FACE_PROVIDER_REJECTED" : code,
                            safeDescription(result), providerBusinessStatus(code));
                }
                return result;
            }
        } catch (SocketTimeoutException ex) {
            throw new FaceRecognitionException("FACE_PROVIDER_TIMEOUT",
                    "Face recognition provider timed out.", 504, ex);
        } catch (IOException ex) {
            throw new FaceRecognitionException("FACE_PROVIDER_UNAVAILABLE",
                    "Face recognition provider is unavailable.", 503, ex);
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) throw provider("FACE_INTEGRATION_DISABLED", "Face recognition is disabled.", 503);
        if (blank(properties.getBaseUrl()) || blank(properties.getApiKey())
                || blank(properties.getClientId()) || blank(properties.getAppId())) {
            throw provider("FACE_CONFIGURATION_INVALID", "Face recognition is not configured.", 503);
        }
    }

    private String joinUrl(String base, String operation) {
        return base.endsWith("/") ? base + operation : base + "/" + operation;
    }

    private int mapStatus(int status) {
        if (status == 401) return 502;
        if (status == 404) return 404;
        if (status == 408 || status == 504) return 504;
        if (status >= 500) return 503;
        return 502;
    }

    private int providerBusinessStatus(String code) {
        if (code != null && code.toLowerCase().contains("not") && code.toLowerCase().contains("found")) return 404;
        return 422;
    }

    private String safeDescription(JsonNode result) {
        String description = safeText(result, "errorDesc");
        return blank(description) ? "Face recognition provider rejected the request."
                : description.substring(0, Math.min(description.length(), 300));
    }

    private String safeText(JsonNode result, String field) {
        JsonNode value = result.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private FaceRecognitionException provider(String code, String message, int status) {
        return new FaceRecognitionException(code, message, status);
    }
}
