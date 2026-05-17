package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.PublicIdentificationHistoryDto;
import com.survisha.meghaconnect.service.PublicIdentificationService;
import com.survisha.meghaconnect.util.RequestContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public-identification")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Public Identification", description = "Citizen profile identification and history endpoints")
@Slf4j
public class PublicIdentificationController {

    private final PublicIdentificationService publicIdentificationService;

    @GetMapping("/citizens/{citizenId}/full-history")
    @PreAuthorize("hasAnyRole('HCM','ADMIN','OSD','DATA_ENTRY_OPERATOR')")
    @Operation(summary = "Get full citizen history", description = "Returns scheme and appointment history for public identification.")
    public ResponseEntity<PublicIdentificationHistoryDto> getFullHistory(
            @PathVariable Long citizenId,
            @AuthenticationPrincipal UserDetails user) {
        log.info("Public identification history request requestId={} citizenId={}", RequestContextUtil.getRequestId(), citizenId);
        String actor = user != null ? user.getUsername() : "public-identification";
        return ResponseEntity.ok(publicIdentificationService.getCitizenFullHistory(citizenId, actor));
    }
}
