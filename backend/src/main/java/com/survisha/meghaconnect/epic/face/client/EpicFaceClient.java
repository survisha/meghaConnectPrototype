package com.survisha.meghaconnect.epic.face.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.epic.face.config.EpicFaceProperties;
import com.survisha.meghaconnect.epic.face.dto.provider.*;
import com.survisha.meghaconnect.epic.face.exception.EpicFaceException;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;

@Component
public class EpicFaceClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final EpicFaceProperties properties;

    public EpicFaceClient(@Qualifier("epicFaceOkHttpClient") OkHttpClient client,
                          ObjectMapper mapper, EpicFaceProperties properties) {
        this.client = client; this.mapper = mapper; this.properties = properties;
    }

    public FaceSearch1NProviderResponse search(String photo) {
        requireConfigured(properties.getSearchApiKey());
        return post(properties.getSearch1nPath(), FaceSearch1NProviderRequest.builder()
                .apiKey(properties.getSearchApiKey()).photo(photo).build(), FaceSearch1NProviderResponse.class);
    }

    public FaceVerify11ProviderResponse verify(String epic, String photo) {
        requireConfigured(properties.getVerifyApiKey());
        return post(properties.getVerify11Path(), FaceVerify11ProviderRequest.builder()
                .apiKey(properties.getVerifyApiKey()).epicNumber(epic).photo(photo).build(), FaceVerify11ProviderResponse.class);
    }

    private <T> T post(String path, Object payload, Class<T> type) {
        try {
            Request request = new Request.Builder().url(join(path)).header("Accept", "application/json")
                    .post(RequestBody.create(mapper.writeValueAsBytes(payload), JSON)).build();
            try (Response response = client.newCall(request).execute()) {
                String body = response.body() == null ? null : response.body().string();
                if (!response.isSuccessful()) throw new EpicFaceException("EPIC_FACE_PROVIDER_ERROR", "EPIC face provider is unavailable.", response.code() == 504 ? 504 : 503);
                if (body == null || body.isBlank()) throw invalidResponse();
                JsonNode tree;
                try { tree = mapper.readTree(body); } catch (Exception ex) { throw invalidResponse(); }
                if (!tree.isObject() || !tree.has("error") || !tree.get("error").isBoolean()) throw invalidResponse();
                return mapper.treeToValue(tree, type);
            }
        } catch (SocketTimeoutException ex) {
            throw new EpicFaceException("EPIC_FACE_TIMEOUT", "EPIC face provider timed out.", 504, ex);
        } catch (EpicFaceException ex) { throw ex;
        } catch (IOException ex) {
            throw new EpicFaceException("EPIC_FACE_PROVIDER_ERROR", "EPIC face provider is unavailable.", 503, ex);
        }
    }

    private void requireConfigured(String key) {
        if (!properties.isEnabled()) throw new EpicFaceException("EPIC_FACE_DISABLED", "EPIC face search is disabled.", 503);
        if (key == null || key.isBlank()) throw new EpicFaceException("EPIC_FACE_NOT_CONFIGURED", "EPIC face search is not configured.", 503);
    }
    private EpicFaceException invalidResponse() { return new EpicFaceException("EPIC_FACE_INVALID_RESPONSE", "EPIC face provider returned an invalid response.", 503); }
    private String join(String path) { return properties.getBaseUrl().replaceAll("/$", "") + (path.startsWith("/") ? path : "/" + path); }
}
