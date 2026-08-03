package com.survisha.meghaconnect.face.controller;

import com.survisha.meghaconnect.face.dto.FaceRequests;
import com.survisha.meghaconnect.face.dto.FaceResponses;
import com.survisha.meghaconnect.face.service.FaceRecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/face-recognition")
@Slf4j
public class FaceRecognitionController {
    private final FaceRecognitionService service;
    private final Executor applicationTaskExecutor;

    public FaceRecognitionController(FaceRecognitionService service,
                                     @Qualifier("applicationTaskExecutor") Executor applicationTaskExecutor) {
        this.service = service;
        this.applicationTaskExecutor = applicationTaskExecutor;
    }

    @PostMapping("/enroll")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DATA_ENTRY_OPERATOR')")
    @Operation(summary = "Enroll a Base64 JPEG or PNG face photo")
    public FaceResponses.Enroll enroll(@Valid @RequestBody FaceRequests.Enroll request) {
        return service.enroll(request);
    }

    @PostMapping("/compare")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OSD','APPROVER','CMO_OFFICER','HCM','DATA_ENTRY_OPERATOR')")
    @Operation(summary = "Compare two face photos (1:1)")
    public FaceResponses.Compare compare(@Valid @RequestBody FaceRequests.Compare request) {
        return service.compare(request);
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Delete an enrolled face")
    public FaceResponses.Delete delete(@Valid @RequestBody FaceRequests.Delete request) {
        return service.delete(request);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OSD','CMO_OFFICER','HCM','DATA_ENTRY_OPERATOR','APPROVER')")
    @Operation(summary = "Search an enrolled face (1:N); matched photo is restricted")
    public CompletableFuture<FaceResponses.Search> search(@Valid @RequestBody FaceRequests.Search request,
                                                           Authentication authentication,
                                                           HttpServletRequest httpRequest) {
        long startedAt = System.nanoTime();
        log.debug("Face search request accepted requestId={} contentType={} requestBytes={} imageMimeType={} imagePresent={}",
                com.survisha.meghaconnect.util.RequestContextUtil.getRequestId(), httpRequest.getContentType(),
                httpRequest.getContentLengthLong(), imageMimeType(request.getPhoto()),
                request.getPhoto() != null && !request.getPhoto().isBlank());
        boolean privileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        return CompletableFuture.supplyAsync(() -> service.search(request, privileged), applicationTaskExecutor)
                .whenComplete((result, error) -> log.debug(
                        "Face search request completed requestId={} matched={} success={} elapsedMs={}",
                        com.survisha.meghaconnect.util.RequestContextUtil.getRequestId(),
                        result != null && result.isMatched(), error == null,
                        (System.nanoTime() - startedAt) / 1_000_000));
    }

    private String imageMimeType(String photo) {
        if (photo == null) return "unknown";
        String value = photo.trim().toLowerCase();
        if (value.startsWith("data:image/jpeg;base64,")) return "image/jpeg";
        if (value.startsWith("data:image/png;base64,")) return "image/png";
        return "base64/unknown";
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OSD','APPROVER','CMO_OFFICER','HCM','DATA_ENTRY_OPERATOR')")
    @Operation(summary = "Verify a face against an enrollment (1:1)")
    public FaceResponses.Verify verify(@Valid @RequestBody FaceRequests.Verify request) {
        return service.verify(request);
    }
}
