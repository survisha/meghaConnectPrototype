package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.service.LLMProviderService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiHealthController {

    private final LLMProviderService llmProviderService;

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN','OSD','APPROVER','CMO','CMO_OFFICER','HCM')")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(AiController.toHealthResponse(llmProviderService.healthCheck()));
    }
}
