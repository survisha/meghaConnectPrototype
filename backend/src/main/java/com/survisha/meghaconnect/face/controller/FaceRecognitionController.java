package com.survisha.meghaconnect.face.controller;

import com.survisha.meghaconnect.face.dto.FaceRequests;
import com.survisha.meghaconnect.face.dto.FaceResponses;
import com.survisha.meghaconnect.face.service.FaceRecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;

@RestController
@RequestMapping("/api/v1/face-recognition")
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
    public CompletableFuture<FaceResponses.Search> search(@Valid @RequestBody FaceRequests.Search request, Authentication authentication) {
        boolean privileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        return CompletableFuture.supplyAsync(() -> service.search(request, privileged), applicationTaskExecutor);
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OSD','APPROVER','CMO_OFFICER','HCM','DATA_ENTRY_OPERATOR')")
    @Operation(summary = "Verify a face against an enrollment (1:1)")
    public FaceResponses.Verify verify(@Valid @RequestBody FaceRequests.Verify request) {
        return service.verify(request);
    }
}
