package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Public-facing registration endpoints.
 *
 * Citizens can self-register via the mobile app or QR-code link
 * without requiring a staff login.  The registration goes through:
 *   1. Phone OTP verification  (POST /api/v1/public/otp/send)
 *   2. OTP confirmation        (POST /api/v1/public/otp/verify)
 *   3. Profile + KYC submission (POST /api/v1/public/register)
 *   4. Photo & document upload  (POST /api/v1/public/upload)
 *
 * File Storage:
 *   Uploaded files (live photo, EPIC scan, Aadhaar scan) are saved to
 *   the configured file store.  The store path is returned in each
 *   upload response so the client can reference it in the registration
 *   payload.  The base URL is meghaconnect.storage.base-url in
 *   application.yml.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicRegistrationController {

    // ── OTP ──────────────────────────────────────────────────────

    /**
     * Send an OTP to the given phone number.
     * TODO: Integrate SMS / WhatsApp OTP provider (Twilio / MSG91 / CDAC).
     */
    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> body) {
        String phone = body.get("phoneNumber");
        if (phone == null || phone.length() != 10) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Invalid phone number");
            return ResponseEntity.badRequest().body(error);
        }
        // TODO: call OTP service
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "OTP sent to " + phone + " (mock – integrate SMS provider)");
        return ResponseEntity.ok(response);
    }

    /**
     * Verify the OTP submitted by the citizen.
     * Returns a short-lived registrationToken used in subsequent calls.
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> body) {
        // TODO: validate OTP from cache / Redis
        String token = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("registrationToken", token);
        response.put("message", "OTP verified (mock)");
        return ResponseEntity.ok(response);
    }

    // ── REGISTRATION ─────────────────────────────────────────────

    /**
     * Submit citizen profile + KYC details.
     *
     * KYC priority enforced here:
     *   • If epicNumber is present  → KYC type = EPIC,   verify via KycController
     *   • If epicNumber is absent   → KYC type = AADHAAR, aadhaarNumber required
     *   • If neither is provided    → KYC type = NONE,   DEO must complete in person
     *
     * Photo and document paths should be obtained from /api/v1/public/upload
     * BEFORE calling this endpoint, then referenced in the DTO.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestHeader("X-Registration-Token") String token,
            @RequestBody PublicRegistrationDto dto) {

        // Determine KYC type
        String kycType = "NONE";
        if (dto.getEpicNumber() != null && !dto.getEpicNumber().trim().isEmpty()) {
            kycType = "EPIC";
        } else if (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().trim().isEmpty()) {
            kycType = "AADHAAR";
        }

        // TODO: persist to public_registrations table
        // TODO: trigger async KYC verification via KycService
        // TODO: attempt match against existing persons via phone/EPIC/Aadhaar

        String appId = "PR-" + System.currentTimeMillis() % 100000;
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("registrationId", appId);
        response.put("kycType", kycType);
        response.put("kycVerificationPending", true);
        response.put("message", "Registration received. KYC verification in progress.");
        return ResponseEntity.ok(response);
    }

    // ── FILE UPLOAD ───────────────────────────────────────────────

    /**
     * Upload a single file (photo, EPIC scan, Aadhaar scan, or document).
     *
     * Files are stored outside the database.  The response returns
     * the storagePath (relative key in the configured file store) that
     * must be included in the registration payload or document_uploads row.
     *
     * Supported documentType values:
     *   PHOTO | EPIC_SCAN | AADHAAR_SCAN | APP_LETTER | PLAN_ESTIMATE |
     *   BANK_DETAILS | HOSPITAL_DOC | ORG_REGISTRATION | MLA_MDC_LETTER
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestHeader("X-Registration-Token") String token,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "File is empty");
            return ResponseEntity.badRequest().body(error);
        }

        // TODO: validate file type / size
        // TODO: store to configured file storage (local / MinIO / S3)
        // TODO: persist metadata to document_uploads table

        String ext        = getExtension(file.getOriginalFilename());
        String storagePath = "registrations/" + token + "/" + documentType.toLowerCase() + ext;

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("storagePath", storagePath);
        response.put("documentType", documentType);
        response.put("message", "File accepted. Persist storagePath in your registration request.");
        return ResponseEntity.ok(response);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".bin";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
