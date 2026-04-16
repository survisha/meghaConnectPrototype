package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.service.VisitorOtpService;
import com.survisha.meghaconnect.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

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

    private final VisitorOtpService  visitorOtpService;
    private final VisitorService     visitorService;
    private final VisitorRepository  visitorRepository;

    // ── 1. Check mobile ───────────────────────────────────────────────────────

    /**
     * Returns whether the mobile number is already registered.
     * The frontend uses this to decide between the "Account Not Found" path
     * and the "Generate OTP" path.
     */
    @PostMapping("/check-mobile")
    public ResponseEntity<Map<String, Object>> checkMobile(@RequestBody Map<String, String> body) {
        String phone = body.get("phoneNumber");
        if (phone == null || phone.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "phoneNumber is required");
            return ResponseEntity.badRequest().body(error);
        }
        boolean found = visitorService.findByPhone(phone.trim()).isPresent();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("registered", found);
        response.put("message", found ? "Account found" : "Account not found");
        return ResponseEntity.ok(response);
    }

    // ── 2. Generate OTP ───────────────────────────────────────────────────────

    /**
     * Generates a one-time password for the registered phone number and
     * returns it in the response body (simulation – replace with SMS gateway).
     */
    @PostMapping("/generate-otp")
    public ResponseEntity<Map<String, Object>> generateOtp(@RequestBody Map<String, String> body) {
        String phone = body.get("phoneNumber");
        if (phone == null || phone.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "phoneNumber is required");
            return ResponseEntity.badRequest().body(error);
        }
        try {
            String otp = visitorOtpService.generateOtp(phone.trim());
            // TODO: remove otp from response once SMS gateway is integrated
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("otp", otp);  // simulated; remove when SMS is live
            response.put("message", "OTP sent to " + phone + " (mock)");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("errorCode", e.getMessage());
            error.put("message", "Mobile number not registered. Please register first.");
            return ResponseEntity.status(404).body(error);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("errorCode", e.getMessage());
            error.put("message", "Too many OTP requests. Please try again later.");
            return ResponseEntity.status(429).body(error);
        }
    }

    // ── 3. Validate OTP ───────────────────────────────────────────────────────

    /**
     * Validates the submitted OTP. On success returns a JWT token that the
     * frontend stores and uses as Bearer token for subsequent calls.
     */
    @PostMapping("/validate-otp")
    public ResponseEntity<Map<String, Object>> validateOtp(@RequestBody Map<String, String> body) {
        String phone = body.get("phoneNumber");
        String otp   = body.get("otp");
        if (phone == null || otp == null || phone.trim().isEmpty() || otp.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "phoneNumber and otp are required");
            return ResponseEntity.badRequest().body(error);
        }
        try {
            String jwt = visitorOtpService.validateOtpAndLogin(phone.trim(), otp.trim());
            Optional<Visitor> visitor = visitorRepository.findByPhoneNumber(phone.trim());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", jwt);
            response.put("fullName", visitor.map(Visitor::getFullName).orElse("Visitor"));
            response.put("visitorId", visitor.map(Visitor::getId).orElse(0L));
            response.put("role", "PUBLIC");
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null && e.getMessage().startsWith("OTP_INVALID:")
                    ? "Incorrect OTP. " + e.getMessage().split(":")[1] + " attempts remaining."
                    : "Mobile number not found.";
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("errorCode", e.getMessage());
            error.put("message", msg);
            return ResponseEntity.status(401).body(error);
        } catch (IllegalStateException e) {
            String msg = "OTP_EXPIRED_OR_NOT_FOUND".equals(e.getMessage())
                    ? "OTP has expired or was not found. Please generate a new OTP."
                    : "Maximum OTP attempts exceeded. Please generate a new OTP.";
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("errorCode", e.getMessage());
            error.put("message", msg);
            return ResponseEntity.status(401).body(error);
        }
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
        try {
            Visitor saved = visitorService.registerVisitor(dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("visitorId", saved.getId());
            response.put("kycStatus", saved.getKycStatus());
            response.put("kycType", saved.getKycType());
            response.put("message", "Visitor registration completed successfully.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("errorCode", e.getMessage());
            error.put("message", getErrorMessage(e.getMessage()));
            return ResponseEntity.badRequest().body(error);
        }
    }

    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "MOBILE_ALREADY_REGISTERED":
                return "This mobile number is already registered. Please login instead.";
            case "INVALID_EPIC_FORMAT":
                return "EPIC number must be 3 uppercase letters followed by 7 digits (e.g. ABC1234567)";
            case "INVALID_AADHAAR_FORMAT":
                return "Aadhaar number must be exactly 12 digits";
            default:
                return errorCode;
        }
    }

    // ── 5. Get profile ────────────────────────────────────────────────────────

    /**
     * Returns the profile of the currently authenticated visitor.
     * Caller must provide a valid JWT bearer token issued by /validate-otp.
     */
    @GetMapping("/profile/{visitorId}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable Long visitorId) {
        Optional<Visitor> personOpt = visitorService.findById(visitorId);
        if (personOpt.isPresent()) {
            Visitor p = personOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", p.getId());
            response.put("fullName", p.getFullName());
            response.put("phoneNumber", p.getPhoneNumber() != null ? p.getPhoneNumber() : "");
            response.put("epicNumber", p.getEpicNumber() != null ? p.getEpicNumber() : "");
            response.put("aadhaarNumber", p.getAadhaarNumber() != null ? p.getAadhaarNumber() : "");
            response.put("kycType", p.getKycType() != null ? p.getKycType() : "NONE");
            response.put("kycVerified", Boolean.TRUE.equals(p.getKycVerified()));
            response.put("kycStatus", p.getKycStatus() != null ? p.getKycStatus() : "PENDING");
            response.put("address", p.getAddress() != null ? p.getAddress() : "");
            response.put("district", p.getDistrict() != null ? p.getDistrict() : "");
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Visitor not found");
            return ResponseEntity.status(404).body(error);
        }
    }
}
