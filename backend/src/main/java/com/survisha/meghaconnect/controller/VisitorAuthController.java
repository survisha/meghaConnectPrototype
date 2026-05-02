package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.service.VisitorAuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Visitor (citizen) authentication endpoints.
 *
 * All paths under /api/v1/visitor/auth/** are publicly accessible (no JWT required).
 *
 * Flow:
 *   1. POST /api/v1/visitor/auth/check-mobile   – check if mobile exists in persons table
 *   2. POST /api/v1/visitor/auth/generate-otp   – generate & deliver OTP (mock; SMS TBD)
 *   3. POST /api/v1/visitor/auth/validate-otp   – validate OTP, return JWT
 *   4. POST /api/v1/visitor/auth/register        – register new visitor
 *   5. GET  /api/v1/visitor/auth/profile         – get visitor profile (JWT required)
 */
@RestController
@RequestMapping("/api/v1/visitor/auth")
@RequiredArgsConstructor
@Tag(name = "Visitor Authentication", description = "Public visitor/citizen authentication endpoints - no JWT required")
@CrossOrigin(origins = "*")
public class VisitorAuthController {

    private final VisitorAuthService visitorAuthService;

    // ── 1. Check mobile ───────────────────────────────────────────────────────

    /**
     * Returns whether the mobile number is already registered.
     * The frontend uses this to decide between the "Account Not Found" path
     * and the "Generate OTP" path.
     */
    @PostMapping("/check-mobile")
    public ResponseEntity<Map<String, Object>> checkMobile(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(visitorAuthService.checkMobile(body));
    }

    // ── 2. Generate OTP ───────────────────────────────────────────────────────

    /**
     * Generates a one-time password for the registered phone number and
     * returns it in the response body (simulation – replace with SMS gateway).
     */
    @PostMapping("/generate-otp")
    public ResponseEntity<Map<String, Object>> generateOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(visitorAuthService.generateOtp(body));
    }

    // ── 3. Validate OTP ───────────────────────────────────────────────────────

    /**
     * Validates the submitted OTP. On success returns a JWT token that the
     * frontend stores and uses as Bearer token for subsequent calls.
     */
    @PostMapping("/validate-otp")
    public ResponseEntity<Map<String, Object>> validateOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(visitorAuthService.validateOtp(body));
    }

    // ── 4. Register visitor ───────────────────────────────────────────────────

    /**
     * Registers a new citizen visitor.
     *
     * Validates:
     *  - Duplicate mobile check
     *  - EPIC format (optional, regex validated)
     *  - Aadhaar format (12 digits, optional)
     *
     * KYC status is stored as PENDING for future verification.
     * After successful registration the client should redirect to login.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody PublicRegistrationDto dto) {
        return ResponseEntity.ok(visitorAuthService.register(dto));
    }

    // ── 5. Get profile ────────────────────────────────────────────────────────

    /**
     * Returns the profile of the currently authenticated visitor.
     * Caller must provide a valid JWT bearer token issued by /validate-otp.
     */
    @GetMapping("/profile/{visitorId}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable Long visitorId) {
        return ResponseEntity.ok(visitorAuthService.getProfile(visitorId));
    }
}
