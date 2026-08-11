package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.service.VisitorAuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Visitor (citizen) authentication endpoints.
 *
 * All paths under /api/v1/visitor/auth/** and /api/v1/auth/** are publicly accessible (no JWT required).
 *
 * Flow:
 *   1. POST /api/v1/visitor/auth/check-mobile   – check if mobile exists in persons table
 *   1a.POST /api/v1/visitor/auth/check-registration – check mobile and EPIC+mobile duplicate status
 *   2. POST /api/v1/visitor/auth/generate-otp   – generate & deliver OTP (mock; SMS TBD)
 *   3. POST /api/v1/auth/validate-otp           – validate login or registration OTP
 *   4. POST /api/v1/visitor/auth/register        – register new visitor
 *   5. GET  /api/v1/visitor/auth/profile         – get visitor profile (JWT required)
 */
@RestController
@RequestMapping({"/api/v1/visitor/auth", "/api/v1/auth"})
@RequiredArgsConstructor
@Tag(name = "Visitor Authentication", description = "Public visitor/citizen authentication endpoints - no JWT required")
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

    /**
     * Checks registration duplicate status without exposing existing visitor data.
     *
     * Mobile duplicates are warnings only. An exact EPIC + mobile duplicate is
     * blocked because it represents the same visitor registration.
     */
    @PostMapping("/check-registration")
    public ResponseEntity<Map<String, Object>> checkRegistration(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(visitorAuthService.checkRegistration(body));
    }

    @PostMapping("/search-registrations")
    public ResponseEntity<Map<String, Object>> searchRegistrations(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(visitorAuthService.searchRegistrations(body));
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
     * Validates the submitted OTP for citizen login or citizen registration.
     * Login requests return visitor session details and a JWT. Registration
     * requests return a success acknowledgement and do not issue a JWT.
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable Long visitorId,
                                                          Authentication authentication) {
        assertVisitorOwnerOrStaff(visitorId, authentication);
        return ResponseEntity.ok(visitorAuthService.getProfile(visitorId));
    }

    @PostMapping("/profile/{visitorId}/kyc/retry")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> retryKyc(@PathVariable Long visitorId,
                                                        @RequestBody(required = false) Map<String, String> body,
                                                        Authentication authentication) {
        assertVisitorOwnerOrStaff(visitorId, authentication);
        return ResponseEntity.ok(visitorAuthService.retryKyc(visitorId, body));
    }

    private void assertVisitorOwnerOrStaff(Long visitorId, Authentication authentication) {
        if (visitorId == null || authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        if (hasStaffAuthority(authentication)) {
            return;
        }
        Long authenticatedVisitorId = parseVisitorId(authentication.getName());
        if (authenticatedVisitorId != null && authenticatedVisitorId.equals(visitorId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Visitor profile access is not permitted.");
    }

    private boolean hasStaffAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN")
                        || authority.equals("ROLE_APPROVER")
                        || authority.equals("ROLE_HCM")
                        || authority.equals("ROLE_DEO"));
    }

    private Long parseVisitorId(String username) {
        if (username == null || !username.startsWith("visitor_")) {
            return null;
        }
        try {
            return Long.parseLong(username.substring("visitor_".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
