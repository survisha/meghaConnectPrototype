package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.*;
import com.survisha.meghaconnect.service.OvseKycService;
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
 * Planned providers:
 *   • ELECTION_COMMISSION_API – EPIC verification (via NEC / ECI API)
 *   • UIDAI_API               – Aadhaar verification (via UIDAI Auth API)
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
@CrossOrigin(origins = "*")
@Tag(name = "KYC Verification", description = "KYC verification endpoints for EPIC and Aadhaar")
public class KycController {

    private final OvseKycService ovseKycService;

    /**
     * Verify an EPIC number against the Election Commission API.
     *
     * EPIC is the primary KYC document for citizens of Meghalaya.
     * Returns the name, date-of-birth, constituency, and
     * verification status from the election roll.
     *
     * @param epic  The voter ID number (e.g. "MH/01/001/234567")
     * @param name  Applicant's stated name (for fuzzy-match scoring)
     */
    @GetMapping("/verify/epic/{epic}")
    public ResponseEntity<Map<String, Object>> verifyEpic(
            @PathVariable String epic,
            @RequestParam(required = false) String name) {

        // TODO: Replace with call to Election Commission API adapter
        // KycProvider provider = kycProviderFactory.getProvider("ELECTION_COMMISSION_API");
        // KycResult result = provider.verify(epic, name);

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

    /**
     * Verify an Aadhaar number against the UIDAI API.
     *
     * Aadhaar is accepted as a KYC fallback when the applicant does
     * not have an EPIC (e.g. minors, persons outside state).
     * Only the last 4 digits of the Aadhaar number are echoed back
     * in the response; the full number is never logged.
     *
     * @param aadhaar  12-digit Aadhaar number
     * @param name    Applicant's stated name (for fuzzy-match scoring)
     */
    @GetMapping("/verify/aadhaar/{aadhaar}")
    public ResponseEntity<Map<String, Object>> verifyAadhaar(
            @PathVariable String aadhaar,
            @RequestParam(required = false) String name) {

        if (aadhaar == null || aadhaar.replaceAll("\\D", "").length() != 12) {
            Map<String, Object> error = new HashMap<>();
            error.put("verified", false);
            error.put("message", "Invalid Aadhaar number – must be 12 digits");
            return ResponseEntity.badRequest().body(error);
        }

        // TODO: Replace with call to UIDAI API adapter
        // KycProvider provider = kycProviderFactory.getProvider("UIDAI_API");
        // KycResult result = provider.verify(aadhaar, name);

        String maskedAadhaar = "XXXX-XXXX-" + aadhaar.substring(aadhaar.length() - 4);
        Map<String, Object> response = new HashMap<>();
        response.put("kycType", "AADHAAR");
        response.put("idValue", maskedAadhaar);  // full number is never echoed
        response.put("provider", "UIDAI_API");
        response.put("verified", true);
        response.put("verifiedName", "Verification pending API integration");
        response.put("nameMatchScore", 0);
        response.put("message", "Mock response – live API integration pending credentials");
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Aadhaar OVSE (Online Verification of Self-Employed) Endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generate OVSE QR code for Aadhaar KYC verification.
     *
     *
     * Response:
     *   {
     *     "success": true,
     *     "txnId": "100001db-fe6f-4a5d-98fe-8cabe809c039",
     *     "qrDataUri": "data:image/png;base64,iVBORw0KGgo...",
     *     "maskedMobile": ""      // Empty if phone not provided
     *   }
     *
     * Frontend flow:
     *   1. Click "Generate QR" button
     *   2. Call this endpoint (no Aadhaar/phone required)
     *   3. Display returned QR code to user
     *   4. User scans with Aadhaar app
     *   5. Poll /result/{txnId} every 3 seconds for KYC data
     *   6. When result available, populate form with verified Aadhaar claims
     *
     * Note: The OVSE SDK generates a generic QR code based on appId and transaction ID.
     * Aadhaar and phone number are not needed for QR generation; they are only used
     * when the Aadhaar app returns KYC data in the callback.
     */
    @PostMapping("/aadhaar/generate-qr")
    @Operation(summary = "Generate Aadhaar OVSE QR code",
            description = "Generate a generic QR code for Aadhaar online verification via OVSE service. " +
                    "No Aadhaar or phone number required.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "QR code generated successfully"),
        @ApiResponse(responseCode = "503", description = "OVSE service unavailable")
    })
    public ResponseEntity<AadhaarQrResponseDto> generateAadhaarQr(
            @RequestBody(required = false) Map<String, String> body) {


        // Generate QR code (OVSE SDK )
        AadhaarQrResponseDto response = ovseKycService.generateQrCode();

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);  // Service unavailable
        }
    }

    /**
     * Retrieve KYC verification result for a transaction.
     *
     * Called by frontend polling after QR scan.
     * Returns null while waiting for Aadhaar app response.
     * Returns populated KycData once callback is received.
     *
     * Example response:
     *   {
     *     "error": false,
     *     "txnId": "100001db-fe6f-4a5d-98fe-8cabe809c039",
     *     "claims": {
     *       "residentName": "Raj Kumar",
     *       "dob": "1990-05-15",
     *       "gender": "M",
     *       "mobile": "9876543210",
     *       "address": "123 Main St, Bengaluru Karnataka 560001",
     *       "residentImage": "base64-encoded-photo-jpeg"
     *     },
     *     "receivedAtMillis": 1629876543210
     *   }
     */
    @GetMapping("/aadhaar/result/{txnId}")
    @Operation(summary = "Poll for Aadhaar KYC result",
            description = "Check if KYC verification has completed for a transaction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Result available (or still pending)"),
        @ApiResponse(responseCode = "404", description = "Transaction ID not found (likely expired)")
    })
    public ResponseEntity<KycData> getKycResult(@PathVariable String txnId) {
        KycData result = ovseKycService.getKycResult(txnId);
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Webhook endpoint to receive KYC result from OVSE callback service.
     *
     * Called by OvseCallback servlet when:
     *   1. User completes Aadhaar app verification (success)
     *   2. User rejects consent (error)
     *   3. Timeout occurs (error)
     *
     * Request body:
     *   Success case:
     *     {
     *       "error": false,
     *       "appId": "100001",
     *       "clientTxnId": "100001db-fe6f-4a5d-98fe-8cabe809c039",
     *       "claimData": {
     *         "residentName": "Raj Kumar",
     *         "dob": "1990-05-15",
     *         "gender": "M",
     *         "mobile": "9876543210",
     *         "address": "...",
     *         "residentImage": "base64-jpeg"
     *       }
     *     }
     *   Error case:
     *     {
     *       "error": true,
     *       "errorCode": "301",
     *       "errorMessage": "User rejected",
     *       "appId": "100001",
     *       "clientTxnId": "100001db-fe6f-4a5d-98fe-8cabe809c039"
     *     }
     *
     * Response: { "accepted": true, "clientTxnId": "..." }
     *
     * NOTE: This endpoint is called by the shared OvseCallback servlet,
     * which routes based on appId. In this deployment, the OvseCallback
     * servlet should be configured with:
     *   100001 = http://meghaconnect-server/api/v1/kyc/aadhaar/callback
     *   This should be kycResults url pattern, once we deploy in environment, need to share that endpoint to prabu to integrate
     */
    @PostMapping("/aadhaar/callback")
    @Operation(summary = "OVSE callback endpoint",
            description = "Receives KYC result from UIDAI Aadhaar app verification")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Callback accepted"),
        @ApiResponse(responseCode = "400", description = "Malformed request"),
        @ApiResponse(responseCode = "403", description = "appId mismatch")
    })
    public ResponseEntity<Map<String, Object>> kycCallback(
            @RequestBody Map<String, Object> body) {

        String clientTxnId = (String) body.get("clientTxnId");
        boolean isError = body.containsKey("error") && (boolean) body.get("error");

        if (clientTxnId == null || clientTxnId.isBlank()) {
            Map<String, Object> response = new HashMap<>();
            response.put("accepted", false);
            response.put("reason", "missing-clientTxnId");
            return ResponseEntity.badRequest().body(response);
        }

        KycData data;
        if (isError) {
            String errorCode = (String) body.getOrDefault("errorCode", "UNKNOWN");
            String errorMessage = (String) body.getOrDefault("errorMessage", "Unknown error");
            data = ovseKycService.storeKycError(clientTxnId, errorCode, errorMessage);
        } else {
            @SuppressWarnings("unchecked")
            Map<String, String> claimData = (Map<String, String>) body.get("claimData");
            data = ovseKycService.storeKycSuccess(clientTxnId, claimData != null ? claimData : new HashMap<>());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("accepted", true);
        response.put("clientTxnId", clientTxnId);
        return ResponseEntity.ok(response);
    }
}
