package com.survisha.meghaconnect.face.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.survisha.meghaconnect.face.client.FaceRecognitionClient;
import com.survisha.meghaconnect.face.config.FaceRecognitionProperties;
import com.survisha.meghaconnect.face.dto.FaceRequests;
import com.survisha.meghaconnect.face.dto.FaceResponses;
import com.survisha.meghaconnect.face.validation.FacePhotoValidator;
import com.survisha.meghaconnect.entity.Visitor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaceRecognitionServiceImpl implements FaceRecognitionService {
    private final FaceRecognitionClient client;
    private final FaceRecognitionProperties properties;
    private final FacePhotoValidator photoValidator;
    private final MeterRegistry meterRegistry;
    private final VisitorEnrollmentLookupService visitorLookupService;

    @Override public FaceResponses.Enroll enroll(FaceRequests.Enroll r) {
        log.debug("Face enrollment started enrollmentId={}", r.getEnrollmentId());
        Map<String,Object> p = credentials(true);
        p.put("enrollmentId", r.getEnrollmentId()); p.put("name", r.getName());
        p.put("lat", r.getLatitude()); p.put("lon", r.getLongitude());
        p.put("photo", photoValidator.normalize(r.getPhoto(), "photo"));
        call("enroll", p);
        log.debug("Face enrollment completed enrollmentId={} success=true", r.getEnrollmentId());
        return FaceResponses.Enroll.builder().success(true).enrollmentId(r.getEnrollmentId())
                .message("Face enrolled successfully.").build();
    }

    @Override public FaceResponses.Compare compare(FaceRequests.Compare r) {
        Map<String,Object> p = credentials(false);
        p.put("photo1", photoValidator.normalize(r.getPhoto1(), "photo1"));
        p.put("photo2", photoValidator.normalize(r.getPhoto2(), "photo2"));
        JsonNode n = call("compare", p);
        boolean identical = n.path("identical").asBoolean(false);
        countResult("compare", identical);
        log.debug("Face comparison result identical={} distance={}", identical, decimal(n, "distance"));
        return FaceResponses.Compare.builder().success(true).identical(identical).distance(decimal(n, "distance"))
                .message(identical ? "Face matched successfully." : "Face did not match.").build();
    }

    @Override public FaceResponses.Delete delete(FaceRequests.Delete r) {
        log.debug("Face deletion started enrollmentId={}", r.getEnrollmentId());
        Map<String,Object> p = credentials(false); p.put("id", r.getEnrollmentId());
        call("delete", p);
        log.debug("Face deletion completed enrollmentId={} success=true", r.getEnrollmentId());
        return FaceResponses.Delete.builder().success(true).enrollmentId(r.getEnrollmentId())
                .message("Enrolled face deleted successfully.").build();
    }

    @Override public FaceResponses.Search search(FaceRequests.Search r, boolean mayReturnMatchedPhoto) {
        Map<String,Object> p = credentials(false);
        p.put("photo", photoValidator.normalize(r.getPhoto(), "photo"));
        p.put("lat", r.getLatitude()); p.put("lon", r.getLongitude());
        JsonNode n = call("search", p);
        boolean matched = n.path("matched").asBoolean(false);
        countResult("search", matched);
        String photo = Boolean.TRUE.equals(r.getIncludeMatchedPhoto()) && mayReturnMatchedPhoto ? text(n, "enrollPhoto") : null;
        String enrollmentId = text(n, "id");
        FaceResponses.MatchedVisitor visitor = matched ? resolveVisitor(enrollmentId, mayReturnMatchedPhoto) : null;
        boolean resolved = matched && visitor != null;
        log.debug("Face search result providerMatched={} resolved={} enrollmentId={} distance={} score={} matchedPhotoReturned={}",
                matched, resolved, enrollmentId, decimal(n, "distance"), decimal(n, "score"), photo != null);
        return FaceResponses.Search.builder().success(true).matched(resolved).enrollmentId(resolved ? enrollmentId : null)
                .name(text(n, "name")).distance(decimal(n, "distance")).score(decimal(n, "score"))
                .matchedPhoto(photo).visitor(visitor)
                .message(resolved ? "Matching visitor found." : "No matching visitor found.").build();
    }

    @Override public FaceResponses.Verify verify(FaceRequests.Verify r) {
        Map<String,Object> p = credentials(false);
        p.put("enrollmentId", r.getEnrollmentId()); p.put("photo", photoValidator.normalize(r.getPhoto(), "photo"));
        p.put("lat", r.getLatitude()); p.put("lon", r.getLongitude());
        JsonNode n = call("verify", p);
        boolean verified = n.path("identical").asBoolean(false);
        countResult("verify", verified);
        log.debug("Face verification result enrollmentId={} verified={} distance={} score={}",
                r.getEnrollmentId(), verified, decimal(n, "distance"), decimal(n, "score"));
        return FaceResponses.Verify.builder().success(true).verified(verified).distance(decimal(n, "distance"))
                .score(decimal(n, "score")).enrollmentId(r.getEnrollmentId())
                .message(verified ? "Face verified successfully." : "Face verification failed.").build();
    }

    private JsonNode call(String operation, Map<String,Object> payload) {
        Timer.Sample sample = Timer.start(meterRegistry);
        long startedAt = System.nanoTime();
        log.debug("Face recognition provider call started operation={}", operation);
        try {
            JsonNode result = client.post(operation, payload);
            recordCall(sample, operation, "success");
            log.debug("Face recognition provider call completed operation={} durationMs={}",
                    operation, elapsedMillis(startedAt));
            return result;
        } catch (RuntimeException ex) {
            String result = isTimeout(ex) ? "timeout" : "technical_error";
            recordCall(sample, operation, result);
            meterRegistry.counter("meghaconnect.external.api.errors", "provider", "deepface",
                    "operation", operation, "result", result).increment();
            log.error("Face recognition provider call failed operation={} durationMs={} error={}",
                    operation, elapsedMillis(startedAt), ex.getMessage(), ex);
            throw ex;
        }
    }

    private void recordCall(Timer.Sample sample, String operation, String result) {
        meterRegistry.counter("meghaconnect.face.operation", "provider", "deepface",
                "operation", operation, "result", result).increment();
        sample.stop(Timer.builder("meghaconnect.face.operation.duration")
                .description("DeepFace operation duration")
                .tags("provider", "deepface", "operation", operation, "result", result)
                .publishPercentileHistogram().register(meterRegistry));
    }

    private boolean isTimeout(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof java.net.SocketTimeoutException
                    || current instanceof java.util.concurrent.TimeoutException) return true;
        }
        return false;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private Map<String,Object> credentials(boolean enrollment) {
        Map<String,Object> p = new LinkedHashMap<>();
        String enrollmentApiKey = properties.getEnroll() != null ? properties.getEnroll().getApiKey() : null;
        p.put("apiKey", enrollment && enrollmentApiKey != null && !enrollmentApiKey.isBlank()
                ? enrollmentApiKey : properties.getApiKey());
        p.put("clientId", properties.getClientId()); p.put("appId", properties.getAppId());
        return p;
    }
    private void countResult(String operation, boolean matched) {
        meterRegistry.counter("meghaconnect.face.result", "operation", operation,
                "result", matched ? "match" : "no_match").increment();
    }
    private String text(JsonNode n, String field) { return n.hasNonNull(field) ? n.get(field).asText() : null; }
    private Double decimal(JsonNode n, String field) { return n.hasNonNull(field) && n.get(field).isNumber() ? n.get(field).asDouble() : null; }

    private FaceResponses.MatchedVisitor resolveVisitor(String enrollmentId, boolean mayReturnPhoto) {
        return visitorLookupService.findVisitorByEnrollmentId(enrollmentId)
                .map(v -> toMatchedVisitor(v, mayReturnPhoto)).orElse(null);
    }

    private FaceResponses.MatchedVisitor toMatchedVisitor(Visitor v, boolean mayReturnPhoto) {
        return FaceResponses.MatchedVisitor.builder().id(v.getId()).fullName(v.getFullName())
                .phoneNumber(v.getPhoneNumber()).epicNumber(v.getEpicNumber()).designation(v.getDesignation())
                .address(v.getAddress()).district(v.getDistrict()).constituency(v.getConstituency())
                .kycStatus(v.getKycStatus()).build();
    }
}
