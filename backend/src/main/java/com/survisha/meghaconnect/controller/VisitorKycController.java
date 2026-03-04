package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.service.VisitorOtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Visitor KYC validation endpoints (mock implementation).
 *
 * All paths under /api/v1/visitor/** are publicly accessible for KYC validation.
 *
 * Multi-step KYC Flow:
 *   1. POST /api/v1/visitor/validate-id   – Validate EPIC/Aadhaar and send OTP
 *   2. POST /api/v1/visitor/verify-otp    – Verify OTP and return mock demographics
 *   3. POST /api/v1/visitor/validate-face – Validate live photo (always returns PHOTO_MATCHED for demo)
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
public class VisitorKycController {

    private final VisitorOtpService visitorOtpService;

    // ── 1. Validate ID (EPIC/Aadhaar) ────────────────────────────────────────

    /**
     * Validates the visitor's ID (EPIC or Aadhaar) and sends an OTP to the mobile number.
     *
     * Mock behavior:
     * - EPIC format: 3 uppercase letters + 7 digits (e.g., ABC1234567)
     * - Aadhaar format: Exactly 12 digits
     * - OTP is generated and sent to the mobile number (mock: OTP = 123456)
     *
     * Request body:
     * {
     *   "idType": "EPIC" | "AADHAAR",
     *   "idValue": "ABC1234567" | "123456789012",
     *   "phoneNumber": "9876543210"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "otpSent": true,
     *   "message": "OTP sent to ****3210"
     * }
     */
    @PostMapping("/validate-id")
    public ResponseEntity<Map<String, Object>> validateId(@RequestBody Map<String, String> request) {
        String idType = request.get("idType");
        String idValue = request.get("idValue");
        String phoneNumber = request.get("phoneNumber");

        // Validate input
        if (idType == null || idValue == null || phoneNumber == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "idType, idValue, and phoneNumber are required");
            return ResponseEntity.badRequest().body(error);
        }

        // Validate EPIC format
        if ("EPIC".equals(idType)) {
            if (!idValue.matches("^[A-Z]{3}[0-9]{7}$")) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("errorCode", "INVALID_EPIC_FORMAT");
                error.put("message", "EPIC number must be 3 uppercase letters followed by 7 digits (e.g., ABC1234567)");
                return ResponseEntity.badRequest().body(error);
            }
        }

        // Validate Aadhaar format
        if ("AADHAAR".equals(idType)) {
            if (!idValue.matches("^[0-9]{12}$")) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("errorCode", "INVALID_AADHAAR_FORMAT");
                error.put("message", "Aadhaar number must be exactly 12 digits");
                return ResponseEntity.badRequest().body(error);
            }
        }

        // Validate phone number format
        if (!phoneNumber.matches("^[0-9]{10}$")) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("errorCode", "INVALID_PHONE_FORMAT");
            error.put("message", "Phone number must be exactly 10 digits");
            return ResponseEntity.badRequest().body(error);
        }

        // TODO: In production, call actual EPIC/Aadhaar verification API here
        // For now, just validate format and generate OTP

        try {
            // Generate OTP (mock: always returns "123456")
            String otp = visitorOtpService.generateKycOtp(phoneNumber);
            
            // Mask phone number for display
            String maskedPhone = "****" + phoneNumber.substring(6);

            // TODO: Remove OTP from response once SMS gateway is integrated
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("otpSent", true);
            response.put("otp", otp);  // DEMO ONLY: Remove in production
            response.put("message", "OTP sent to " + maskedPhone);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("errorCode", "TOO_MANY_REQUESTS");
            error.put("message", "Too many OTP requests. Please try again later.");
            return ResponseEntity.status(429).body(error);
        }
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
        String phoneNumber = request.get("phoneNumber");
        String otp = request.get("otp");
        String idType = request.get("idType");
        String idValue = request.get("idValue");

        if (phoneNumber == null || otp == null || idType == null || idValue == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "phoneNumber, otp, idType, and idValue are required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // Validate OTP (mock: always "123456")
            boolean isValid = visitorOtpService.validateKycOtp(phoneNumber, otp);
            
            if (!isValid) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("errorCode", "INVALID_OTP");
                error.put("message", "Invalid OTP. Please try again.");
                return ResponseEntity.status(401).body(error);
            }

            // TODO: In production, fetch actual demographics from EPIC/Aadhaar API
            // For now, return mock data based on ID type
            Map<String, String> demographics = new HashMap<>();
            if ("EPIC".equals(idType)) {
                demographics.put("fullName", "Rajesh Kumar Sharma");
                demographics.put("address", "Laitumkhrah, Shillong");
                demographics.put("district", "East Khasi Hills");
                demographics.put("constituency", "Shillong North");
            } else if ("AADHAAR".equals(idType)) {
                demographics.put("fullName", "Priya Singh");
                demographics.put("address", "Police Bazar, Shillong");
                demographics.put("district", "East Khasi Hills");
                demographics.put("constituency", "Shillong Central");
            } else {
                demographics.put("fullName", "Unknown");
                demographics.put("address", "");
                demographics.put("district", "");
                demographics.put("constituency", "");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("verified", true);
            response.put("demographics", demographics);
            response.put("message", "OTP verified successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("errorCode", "OTP_EXPIRED");
            error.put("message", "OTP has expired. Please generate a new OTP.");
            return ResponseEntity.status(401).body(error);
        }
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
        String idType = request.get("idType");
        String idValue = request.get("idValue");
        String livePhotoBase64 = request.get("livePhotoBase64");

        if (idType == null || idValue == null || livePhotoBase64 == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "idType, idValue, and livePhotoBase64 are required");
            return ResponseEntity.badRequest().body(error);
        }

        // Validate base64 image format
        if (!livePhotoBase64.startsWith("data:image/")) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("errorCode", "INVALID_IMAGE_FORMAT");
            error.put("message", "Live photo must be a valid base64 encoded image");
            return ResponseEntity.badRequest().body(error);
        }

        // TODO: In production, call actual face recognition API
        // For now, always return PHOTO_MATCHED for demo
        
        // Mock face recognition result (always successful)
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("matched", true);
        response.put("kycStatus", "PHOTO_MATCHED");
        response.put("confidence", 95.5);  // Mock confidence score
        response.put("message", "Face matched successfully");
        return ResponseEntity.ok(response);
    }
}
