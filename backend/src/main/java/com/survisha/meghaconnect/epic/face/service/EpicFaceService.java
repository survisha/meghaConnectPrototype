package com.survisha.meghaconnect.epic.face.service;

import com.survisha.meghaconnect.epic.face.client.EpicFaceClient;
import com.survisha.meghaconnect.epic.face.dto.EpicFaceResponse;
import com.survisha.meghaconnect.epic.face.dto.provider.FaceSearch1NProviderResponse;
import com.survisha.meghaconnect.epic.face.dto.provider.FaceVerify11ProviderResponse;
import com.survisha.meghaconnect.epic.face.exception.EpicFaceException;
import com.survisha.meghaconnect.face.validation.FacePhotoValidator;
import com.survisha.meghaconnect.service.AuditLogService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpicFaceService {
    private final EpicFaceClient client;
    private final FacePhotoValidator photoValidator;
    private final MeterRegistry metrics;
    private final AuditLogService audit;

    public EpicFaceResponse search(String photo, String actor) {
        log.debug("Starting EPIC face operation=search_1n photoEncodedSize={}", photo == null ? 0 : photo.length());
        String normalized = photoValidator.normalize(photo, "photo");
        log.debug("Validated EPIC face operation=search_1n normalizedPhotoEncodedSize={}", normalized.length());
        Timer.Sample sample = Timer.start(metrics);
        long started = System.nanoTime();
        try {
            log.debug("Calling EPIC face provider operation=search_1n");
            FaceSearch1NProviderResponse p = client.search(normalized);
            log.debug("EPIC face provider responded operation=search_1n error={} matched={} errorCode={}",
                    p.isError(), p.isMatched(), p.getErrorCode() == null ? null : safe(p.getErrorCode()));
            if (p.isError()) throw providerRejected(p.getErrorCode());
            EpicFaceResponse result = response(p);
            record("search_1n", result.isMatched(), actor, result.getEpicNumber(), normalized.length());
            return result;
        } catch (RuntimeException ex) {
            log.warn("EPIC face operation=search_1n failed errorType={}", ex.getClass().getSimpleName());
            log.debug("EPIC face operation=search_1n failure details", ex);
            throw ex;
        } finally {
            sample.stop(Timer.builder("meghaconnect.epic.face.duration").tag("operation", "search_1n").register(metrics));
            log.info("EPIC face operation={} durationMs={}", "search_1n", (System.nanoTime() - started) / 1_000_000);
        }
    }

    public EpicFaceResponse verify(String epic, String photo, String actor) {
        long started = System.nanoTime();
        String normalizedEpic = epic == null ? null : epic.trim().toUpperCase();
        log.debug("Starting EPIC face operation=verify_11 epic={} photoEncodedSize={}",
                mask(normalizedEpic), photo == null ? 0 : photo.length());
        String normalized = photoValidator.normalize(photo, "photo");
        log.debug("Validated EPIC face operation=verify_11 normalizedPhotoEncodedSize={}", normalized.length());
        Timer.Sample sample = Timer.start(metrics);
        try {
            log.debug("Calling EPIC face provider operation=verify_11 epic={}", mask(normalizedEpic));
            FaceVerify11ProviderResponse p = client.verify(normalizedEpic, normalized);
            log.debug("EPIC face provider responded operation=verify_11 error={} matched={} errorCode={}",
                    p.isError(), p.isMatched(), p.getErrorCode() == null ? null : safe(p.getErrorCode()));
            if (p.isError()) throw providerRejected(p.getErrorCode());
            EpicFaceResponse result = response(p);
            record("verify_11", result.isMatched(), actor, normalizedEpic, normalized.length());
            return result;
        } catch (RuntimeException ex) {
            log.warn("EPIC face operation=verify_11 failed epic={} errorType={}",
                    mask(normalizedEpic), ex.getClass().getSimpleName());
            log.debug("EPIC face operation=verify_11 failure details", ex);
            throw ex;
        } finally {
            sample.stop(Timer.builder("meghaconnect.epic.face.duration").tag("operation", "verify_11").register(metrics));
            log.info("EPIC face operation={} durationMs={}", "verify_11", (System.nanoTime() - started) / 1_000_000);
        }
    }

    private EpicFaceResponse response(FaceSearch1NProviderResponse p) {
        return EpicFaceResponse.builder().matched(p.isMatched()).epicNumber(p.getEpicNumber()).name(p.getName())
                .address(p.getAddress()).serialNumber(p.getSerialNumber()).partNumber(p.getPartNumber())
                .partName(p.getPartName()).acpcName(p.getAcpcName()).district(p.getDistrict()).pincode(p.getPincode())
                .epicPhoto(p.getPhoto()).source("EPIC_FACE_1N").providerStatus(p.isMatched() ? "MATCHED" : "NOT_FOUND").build();
    }
    private EpicFaceResponse response(FaceVerify11ProviderResponse p) {
        return EpicFaceResponse.builder().matched(p.isMatched()).epicNumber(p.getEpicNumber()).name(p.getName())
                .address(p.getAddress()).serialNumber(p.getSerialNumber()).partNumber(p.getPartNumber())
                .partName(p.getPartName()).acpcName(p.getAcpcName()).district(p.getDistrict()).pincode(p.getPincode())
                .epicPhoto(p.getPhoto()).source("EPIC_FACE_11").providerStatus(p.isMatched() ? "MATCHED" : "NOT_MATCHED").build();
    }
    private EpicFaceException providerRejected(String code) {
        return new EpicFaceException("EPIC_FACE_PROVIDER_REJECTED", "EPIC face provider rejected the request" + (code == null ? "." : " (" + safe(code) + ")."), 503);
    }
    private void record(String operation, boolean matched, String actor, String epic, int encodedLength) {
        metrics.counter("meghaconnect.epic.face.result", "operation", operation, "result", matched ? "match" : "no_match").increment();
        audit.log("EpicFace", 0L, operation.toUpperCase(), "matched=" + matched + ", epic=" + mask(epic) + ", encodedSize=" + encodedLength, actor);
        log.info("EPIC face operation={} matched={} epic={} encodedSize={}", operation, matched, mask(epic), encodedLength);
    }
    private String mask(String epic) { return epic == null || epic.length() < 4 ? "****" : "****" + epic.substring(epic.length() - 4); }
    private String safe(String value) { return value.replaceAll("[^A-Za-z0-9_-]", "").substring(0, Math.min(40, value.replaceAll("[^A-Za-z0-9_-]", "").length())); }
}
