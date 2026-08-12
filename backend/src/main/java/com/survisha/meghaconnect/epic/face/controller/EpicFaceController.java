package com.survisha.meghaconnect.epic.face.controller;

import com.survisha.meghaconnect.epic.face.dto.*;
import com.survisha.meghaconnect.epic.face.service.EpicFaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/epic/face")
@RequiredArgsConstructor
@Slf4j
public class EpicFaceController {
    private final EpicFaceService service;

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DEO','APPROVER','HCM')")
    public EpicFaceResponse search(@Valid @RequestBody EpicFaceSearchRequest request, Authentication authentication) {
        log.debug("Received EPIC face search request photoEncodedSize={}", encodedSize(request.getPhoto()));
        EpicFaceResponse response = service.search(request.getPhoto(), authentication.getName());
        log.debug("Completed EPIC face search request matched={}", response.isMatched());
        return response;
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DEO','APPROVER','HCM')")
    public EpicFaceResponse verify(@Valid @RequestBody EpicFaceVerifyRequest request, Authentication authentication) {
        log.debug("Received EPIC face verification request epicPresent={} photoEncodedSize={}",
                request.getEpicNumber() != null && !request.getEpicNumber().isBlank(), encodedSize(request.getPhoto()));
        EpicFaceResponse response = service.verify(request.getEpicNumber(), request.getPhoto(), authentication.getName());
        log.debug("Completed EPIC face verification request matched={}", response.isMatched());
        return response;
    }

    private int encodedSize(String photo) {
        return photo == null ? 0 : photo.length();
    }
}
