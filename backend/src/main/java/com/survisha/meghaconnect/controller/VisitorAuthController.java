package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.entity.Person;
import com.survisha.meghaconnect.repository.PersonRepository;
import com.survisha.meghaconnect.service.VisitorOtpService;
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
@CrossOrigin(origins = "*")
public class VisitorAuthController {

    private final VisitorOtpService  visitorOtpService;
    private final PersonRepository   personRepository;

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
        boolean found = visitorOtpService.isMobileRegistered(phone.trim());
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
            Optional<Person> visitor = personRepository.findByPhoneNumber(phone.trim());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", jwt);
            response.put("fullName", visitor.map(Person::getFullName).orElse("Visitor"));
            response.put("visitorId", visitor.map(Person::getId).orElse(0L));
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
        if (dto.getFullName() == null || dto.getFullName().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Full name is required");
            return ResponseEntity.badRequest().body(error);
        }
        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Phone number is required");
            return ResponseEntity.badRequest().body(error);
        }
        if (dto.getPhoneNumber().length() != 10) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Phone number must be 10 digits");
            return ResponseEntity.badRequest().body(error);
        }

        // Duplicate mobile check
        if (personRepository.findByPhoneNumber(dto.getPhoneNumber()).isPresent()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("errorCode", "MOBILE_ALREADY_REGISTERED");
            error.put("message", "This mobile number is already registered. Please login instead.");
            return ResponseEntity.status(409).body(error);
        }

        // Validate EPIC format if provided (alphanumeric, typically 3 letters + 7 digits)
        if (dto.getEpicNumber() != null && !dto.getEpicNumber().trim().isEmpty()) {
            if (!dto.getEpicNumber().matches("^[A-Z]{3}[0-9]{7}$")) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("errorCode", "INVALID_EPIC_FORMAT");
                error.put("message", "EPIC number must be 3 uppercase letters followed by 7 digits (e.g. ABC1234567)");
                return ResponseEntity.badRequest().body(error);
            }
        }

        // Validate Aadhaar format if provided (exactly 12 digits)
        if (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().trim().isEmpty()) {
            if (!dto.getAadhaarNumber().matches("^[0-9]{12}$")) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("errorCode", "INVALID_AADHAAR_FORMAT");
                error.put("message", "Aadhaar number must be exactly 12 digits");
                return ResponseEntity.badRequest().body(error);
            }
        }

        // Determine KYC type
        String kycType = "NONE";
        if (dto.getEpicNumber() != null && !dto.getEpicNumber().trim().isEmpty()) {
            kycType = "EPIC";
        } else if (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().trim().isEmpty()) {
            kycType = "AADHAAR";
        }

        // Determine KYC status
        // If manual verification was used, override to MANUAL_VERIFICATION_REQUIRED regardless of claimed status
        String kycStatus;
        if (Boolean.TRUE.equals(dto.getManualVerification())) {
            kycStatus = "MANUAL_VERIFICATION_REQUIRED";
        } else if (dto.getKycStatus() != null && !dto.getKycStatus().trim().isEmpty()) {
            kycStatus = dto.getKycStatus().trim();
        } else {
            kycStatus = "PENDING";
        }

        boolean kycVerified = "PHOTO_MATCHED".equals(kycStatus) || "DEMOGRAPHIC_MATCHED".equals(kycStatus);

        // Persist visitor
        Person visitor = Person.builder()
                .fullName(dto.getFullName().trim())
                .phoneNumber(dto.getPhoneNumber().trim())
                .email(dto.getEmail())
                .epicNumber(dto.getEpicNumber())
                .aadhaarNumber(dto.getAadhaarNumber())
                .kycType(kycType)
                .kycVerified(kycVerified)
                .kycVerifiedAt(kycVerified ? java.time.LocalDateTime.now() : null)
                .kycStatus(kycStatus)
                .address(dto.getAddress())
                .designation(dto.getDesignation())
                .district(dto.getDistrict())
                .constituency(dto.getConstituency())
                .booth(dto.getBooth())
                .village(dto.getVillage())
                .photoStoragePath(dto.getPhotoStoragePath())
                .build();
        Person saved = personRepository.save(visitor);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("visitorId", saved.getId());
        response.put("kycStatus", kycStatus);
        response.put("kycType", kycType);
        response.put("message", "Visitor registration completed successfully.");
        return ResponseEntity.ok(response);
    }

    // ── 5. Get profile ────────────────────────────────────────────────────────

    /**
     * Returns the profile of the currently authenticated visitor.
     * Caller must provide a valid JWT bearer token issued by /validate-otp.
     */
    @GetMapping("/profile/{visitorId}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable Long visitorId) {
        Optional<Person> personOpt = personRepository.findById(visitorId);
        if (personOpt.isPresent()) {
            Person p = personOpt.get();
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
