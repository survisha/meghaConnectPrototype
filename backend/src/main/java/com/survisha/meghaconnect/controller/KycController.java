package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.*;
import com.survisha.meghaconnect.service.EpicVerificationService;
import com.survisha.meghaconnect.service.RequestValidationService;
import com.survisha.meghaconnect.service.VisitorService;
import com.survisha.meghaconnect.exception.VisitorRegistrationValidationException;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.util.ValidationConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

/**
 * KYC verification endpoints.
 *
 * Architecture is plug-and-play: each provider is a separate adapter
 * implementing a common KycProvider interface (to be added when live
 * API credentials are available).  The mock responses below allow
 * frontend / mobile development to proceed immediately.
 *
 * Planned provider:
 *   • ELECTION_COMMISSION_API – EPIC verification (via NEC / ECI API)
 *
 * Production integration steps (per provider):
 *   1. Obtain API credentials and endpoint URLs.
 *   2. Implement KycProvider interface in service/kyc/ package.
 *   3. Register the bean; KycService auto-selects based on kyc_type.
 *   4. Update application.yml with provider-specific properties.
 */
@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC Verification", description = "KYC verification endpoints for EPIC")
public class KycController {

    private final EpicVerificationService epicVerificationService;
    private final RequestValidationService validationService;

    /**
     * Verify an EPIC number against the Election Commission API.
     *
     * EPIC is the primary KYC document for citizens of Meghalaya.
     * Returns the name, date-of-birth, constituency, district, and
     * verification status from the election roll.
     *
     * Request:
     *   {
     *     "epicNumber": "BCV0259184",
     *     "visitorName": "MAREIAM MOSSANG",
     *     "phoneNumber": "9876543210"  (optional)
     *   }
     *
     * Response (Success):
     *   {
     *     "code": "200",
     *     "message": "Success",
     *     "data": {
     *       "voteridnumber": "BCV0259184",
     *       "borrowernameonvoteridcard": "MAREIAM MOSSANG",
     *       "borroweraddressdistrict": "CHANGLANG",
     *       "borroweraddressstate": "Arunachal Pradesh",
     *       "namematchscore": 95,
     *       "voteridverificationstatus": "id_found",
     *       ...
     *     }
     *   }
     */
    @PostMapping("/verify/epic")
    @Operation(summary = "Verify EPIC against Election Commission API",
            description = "Verify voter ID (EPIC) number and name against Election Commission database")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification completed (check status in response)"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "503", description = "Election Commission API unavailable")
    })
    public ResponseEntity<EpicVerificationResponse> verifyEpic(
            @RequestBody EpicVerificationRequest request) {

        if (request == null || !Boolean.TRUE.equals(request.getConsentGranted())
                || !VisitorService.REGISTRATION_CONSENT_VERSION.equals(request.getConsentVersion())
                || !("WEB".equalsIgnoreCase(request.getConsentChannel())
                || "MOBILE".equalsIgnoreCase(request.getConsentChannel()))) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    "Citizen consent is required before photo capture and voter/EPIC verification.");
        }

        String epicNumber = validationService.requireText(
                request != null ? request.getEpicNumber() : null,
                ValidationConstants.FIELD_EPIC_NUMBER
        );
        validationService.requireEpic(epicNumber);
        validationService.requireText(
                request != null ? request.getVisitorName() : null,
                ValidationConstants.FIELD_VISITOR_NAME
        );

        // Call EPIC verification service
        EpicVerificationResponse response = epicVerificationService.verifyEpic(request);
        response.setKycProvider("EPIC");

        // Return based on API response code
        if ("200".equals(response.getCode())) {
            response.setSuccess(true);
            response.setCanProceed(false);
            response.setKycStatus("DEMOGRAPHIC_MATCHED");
            return ResponseEntity.ok(response);
        } else if ("503".equals(response.getCode())) {
            response.setSuccess(false);
            response.setCanProceed(true);
            response.setKycStatus("KYC_PENDING");
            return ResponseEntity.status(503).body(response);
        } else {
            response.setSuccess(false);
            response.setCanProceed(false);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Legacy EPIC verification endpoint (GET-based).
     * Kept for backward compatibility; new implementations should use POST /verify/epic
     */
    @GetMapping("/verify/epic/{epic}")
    @Deprecated
    public ResponseEntity<Map<String, Object>> verifyEpicLegacy(
            @PathVariable String epic,
            @RequestParam(required = false) String name) {

        Map<String, Object> response = new HashMap<>();
        response.put("kycType", "EPIC");
        response.put("idValue", epic);
        response.put("provider", "ELECTION_COMMISSION_API");
        response.put("verified", true);
        response.put("verifiedName", "Verification pending API integration");
        response.put("nameMatchScore", 0);
        response.put("message", "Mock response – live API integration pending credentials");
        return ResponseEntity.ok(response);
    }

}
