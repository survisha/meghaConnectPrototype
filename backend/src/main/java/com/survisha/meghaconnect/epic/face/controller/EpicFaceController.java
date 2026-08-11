package com.survisha.meghaconnect.epic.face.controller;

import com.survisha.meghaconnect.epic.face.dto.*;
import com.survisha.meghaconnect.epic.face.service.EpicFaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/epic/face")
@RequiredArgsConstructor
public class EpicFaceController {
    private final EpicFaceService service;

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DEO','APPROVER','HCM')")
    public EpicFaceResponse search(@Valid @RequestBody EpicFaceSearchRequest request, Authentication authentication) {
        return service.search(request.getPhoto(), authentication.getName());
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DEO','APPROVER','HCM')")
    public EpicFaceResponse verify(@Valid @RequestBody EpicFaceVerifyRequest request, Authentication authentication) {
        return service.verify(request.getEpicNumber(), request.getPhoto(), authentication.getName());
    }
}
