package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.service.HcmDecisionSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hcm")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM')")
public class HcmDecisionSupportController {
    private final HcmDecisionSupportService service;

    @GetMapping("/dashboard-summary")
    public Map<String, Long> dashboardSummary() { return service.dashboardCounts(); }

    @GetMapping("/citizen-intelligence/{visitorId}")
    public Map<String, Object> citizenIntelligence(@PathVariable Long visitorId, Authentication auth) {
        return service.citizenIntelligence(visitorId, auth.getName());
    }
}
