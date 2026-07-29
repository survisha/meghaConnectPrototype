package com.survisha.meghaconnect.face.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.survisha.meghaconnect.face.client.FaceRecognitionClient;
import com.survisha.meghaconnect.face.config.FaceRecognitionProperties;
import com.survisha.meghaconnect.face.dto.FaceRequests;
import com.survisha.meghaconnect.face.dto.FaceResponses;
import com.survisha.meghaconnect.face.validation.FacePhotoValidator;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FaceRecognitionServiceImpl implements FaceRecognitionService {
    private final FaceRecognitionClient client;
    private final FaceRecognitionProperties properties;
    private final FacePhotoValidator photoValidator;
    private final MeterRegistry meterRegistry;

    @Override public FaceResponses.Enroll enroll(FaceRequests.Enroll r) {
        Map<String,Object> p = credentials();
        p.put("enrollmentId", r.getEnrollmentId()); p.put("name", r.getName());
        p.put("lat", r.getLatitude()); p.put("lon", r.getLongitude());
        p.put("photo", photoValidator.normalize(r.getPhoto(), "photo"));
        call("enroll", p);
        return FaceResponses.Enroll.builder().success(true).enrollmentId(r.getEnrollmentId())
                .message("Face enrolled successfully.").build();
    }

    @Override public FaceResponses.Compare compare(FaceRequests.Compare r) {
        Map<String,Object> p = credentials();
        p.put("photo1", photoValidator.normalize(r.getPhoto1(), "photo1"));
        p.put("photo2", photoValidator.normalize(r.getPhoto2(), "photo2"));
        JsonNode n = call("compare", p);
        boolean identical = n.path("identical").asBoolean(false);
        countResult("compare", identical);
        return FaceResponses.Compare.builder().success(true).identical(identical).distance(decimal(n, "distance"))
                .message(identical ? "Face matched successfully." : "Face did not match.").build();
    }

    @Override public FaceResponses.Delete delete(FaceRequests.Delete r) {
        Map<String,Object> p = credentials(); p.put("id", r.getEnrollmentId());
        call("delete", p);
        return FaceResponses.Delete.builder().success(true).enrollmentId(r.getEnrollmentId())
                .message("Enrolled face deleted successfully.").build();
    }

    @Override public FaceResponses.Search search(FaceRequests.Search r, boolean mayReturnMatchedPhoto) {
        Map<String,Object> p = credentials();
        p.put("photo", photoValidator.normalize(r.getPhoto(), "photo"));
        p.put("lat", r.getLatitude()); p.put("lon", r.getLongitude());
        JsonNode n = call("search", p);
        boolean matched = n.path("matched").asBoolean(false);
        countResult("search", matched);
        String photo = Boolean.TRUE.equals(r.getIncludeMatchedPhoto()) && mayReturnMatchedPhoto ? text(n, "enrollPhoto") : null;
        return FaceResponses.Search.builder().success(true).matched(matched).enrollmentId(text(n, "id"))
                .name(text(n, "name")).distance(decimal(n, "distance")).score(decimal(n, "score"))
                .matchedPhoto(photo).message(matched ? "Matching face found." : "No matching face found.").build();
    }

    @Override public FaceResponses.Verify verify(FaceRequests.Verify r) {
        Map<String,Object> p = credentials();
        p.put("enrollmentId", r.getEnrollmentId()); p.put("photo", photoValidator.normalize(r.getPhoto(), "photo"));
        p.put("lat", r.getLatitude()); p.put("lon", r.getLongitude());
        JsonNode n = call("verify", p);
        boolean verified = n.path("identical").asBoolean(false);
        countResult("verify", verified);
        return FaceResponses.Verify.builder().success(true).verified(verified).distance(decimal(n, "distance"))
                .score(decimal(n, "score")).enrollmentId(r.getEnrollmentId())
                .message(verified ? "Face verified successfully." : "Face verification failed.").build();
    }

    private JsonNode call(String operation, Map<String,Object> payload) {
        meterRegistry.counter("face.recognition.requests", "operation", operation).increment();
        try {
            JsonNode result = client.post(operation, payload);
            meterRegistry.counter("face.recognition.success", "operation", operation).increment();
            return result;
        } catch (RuntimeException ex) {
            meterRegistry.counter("face.recognition.failures", "operation", operation).increment();
            throw ex;
        }
    }
    private Map<String,Object> credentials() {
        Map<String,Object> p = new LinkedHashMap<>();
        p.put("apiKey", properties.getApiKey()); p.put("clientId", properties.getClientId()); p.put("appId", properties.getAppId());
        return p;
    }
    private void countResult(String operation, boolean matched) {
        meterRegistry.counter("face.recognition.results", "operation", operation, "matched", Boolean.toString(matched)).increment();
    }
    private String text(JsonNode n, String field) { return n.hasNonNull(field) ? n.get(field).asText() : null; }
    private Double decimal(JsonNode n, String field) { return n.hasNonNull(field) && n.get(field).isNumber() ? n.get(field).asDouble() : null; }
}
