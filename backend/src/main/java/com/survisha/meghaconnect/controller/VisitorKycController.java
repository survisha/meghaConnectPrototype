package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.service.VisitorKycService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Backward-compatible visitor OTP/photo validation endpoints used by the
 * current Angular registration flow.
 *
 * KycController remains the owner of real EPIC and Aadhaar provider APIs.
 * Do not add EPIC/Aadhaar verification calls here.
 */
@RestController
@RequestMapping("/api/v1/visitor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Visitor Registration Validation", description = "Backward-compatible OTP and face validation endpoints")
@Deprecated
public class VisitorKycController {

    private final VisitorKycService visitorKycService;

    /**
     * Legacy/mock endpoint retained for existing clients. New EPIC/Aadhaar
     * verification should use KycController.
     */
    @PostMapping("/validate-idType")
    public ResponseEntity<Map<String, Object>> validateIdType(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(visitorKycService.validateIdType(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(visitorKycService.verifyOtp(request));
    }

    @PostMapping("/validate-face")
    public ResponseEntity<Map<String, Object>> validateFace(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(visitorKycService.validateFace(request));
    }
}
