package com.survisha.meghaconnect.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.survisha.meghaconnect.service.VisitorKycService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Visitor KYC validation endpoints (mock implementation).
 *
 * All paths under /api/v1/visitor/** are publicly accessible for KYC validation.
 *
 * Multi-step KYC Flow:
 *   1. POST /api/v1/visitor/validate-idType   – Validate EPIC/Aadhaar and send OTP
 *   2. POST /api/v1/visitor/verify-otp        – Verify OTP and return mock demographics
 *   3. POST /api/v1/visitor/validate-face     – Validate live photo (always returns PHOTO_MATCHED for demo)
 *
 * NOTE: This is a MOCK implementation for frontend/mobile development.
 * - OTP is always "123456" for demo purposes
 * - EPIC/Aadhaar validation is format-only (no API integration)
 * - Face matching always returns PHOTO_MATCHED
 * - Demographics returned are hard-coded sample data
 *
 * Production TODO:
 * - Integrate with Election Commission API for EPIC verification
 * - Integrate with UIDAI API for Aadhaar verification
 * - Implement actual SMS gateway for OTP delivery
 * - Implement face recognition service (e.g., AWS Rekognition, Azure Face API)
 * - Store KYC validation results in database with audit trail
 */
@RestController
@RequestMapping("/api/v1/visitor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Visitor KYC", description = "Public visitor KYC validation endpoints - no JWT required")
public class VisitorKycController {

    private final VisitorKycService visitorKycService;

    // ── 1. Validate ID (EPIC/Aadhaar) ────────────────────────────────────────

    /**
     * Validates the visitor's ID (EPIC or Aadhaar) and sends an OTP to the registered mobile number.
     *
     * Mock behavior:
     * - EPIC format: 3 uppercase letters + 7 digits (e.g., ABC1234567)
     * - Aadhaar format: Exactly 12 digits
     * - In production, EPIC/Aadhaar API will return registered mobile number and send OTP
     * - For demo, if phoneNumber is provided, OTP is sent to that number (for manual verification)
     * - If phoneNumber not provided, mock registered number is used (simulating API response)
     *
     * Request body:
     * {
     *   "idType": "EPIC" | "AADHAAR",
     *   "idValue": "ABC1234567" | "123456789012",
     *   "phoneNumber": "9876543210" (optional - for manual verification fallback)
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "otpSent": true,
     *   "phoneNumber": "****3210",
     *   "message": "OTP sent to ****3210",
     *   "manualVerification": false (true if phoneNumber was provided manually)
     * }
     */
    @PostMapping("/validate-idType")
    public ResponseEntity<Map<String, Object>> validateIdType(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(visitorKycService.validateIdType(request));
    }

    // ── 2. Verify OTP ─────────────────────────────────────────────────────────

    /**
     * Verifies the OTP and returns mock demographics data.
     *
     * Mock behavior:
     * - OTP is always "123456" for successful verification
     * - Returns hard-coded demographic data
     * - In production, this should fetch actual data from EPIC/Aadhaar API
     *
     * Request body:
     * {
     *   "phoneNumber": "9876543210",
     *   "otp": "123456",
     *   "idType": "EPIC" | "AADHAAR",
     *   "idValue": "ABC1234567"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "verified": true,
     *   "demographics": {
     *     "fullName": "John Doe",
     *     "address": "Main Street, Shillong",
     *     "district": "East Khasi Hills",
     *     "constituency": "Shillong"
     *   }
     * }
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(visitorKycService.verifyOtp(request));
    }

    // ── 3. Validate Face Photo ───────────────────────────────────────────────

    /**
     * Validates the live photo against the ID photo using face recognition.
     *
     * Mock behavior:
     * - Always returns PHOTO_MATCHED for demo purposes
     * - In production, should call actual face recognition API
     *
     * Request body:
     * {
     *   "idType": "EPIC" | "AADHAAR",
     *   "idValue": "ABC1234567",
     *   "livePhotoBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "matched": true,
     *   "kycStatus": "PHOTO_MATCHED",
     *   "confidence": 95.5,
     *   "message": "Face matched successfully"
     * }
     */
    @PostMapping("/validate-face")
    public ResponseEntity<Map<String, Object>> validateFace(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(visitorKycService.validateFace(request));
    }
}
