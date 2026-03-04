package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.entity.Person;
import com.survisha.meghaconnect.repository.PersonRepository;
import com.survisha.meghaconnect.service.VisitorOtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

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
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "phoneNumber is required"
            ));
        }
        boolean found = visitorOtpService.isMobileRegistered(phone.trim());
        return ResponseEntity.ok(Map.of(
                "success",    true,
                "registered", found,
                "message",    found ? "Account found" : "Account not found"
        ));
    }

    // ── 2. Generate OTP ───────────────────────────────────────────────────────

    /**
     * Generates a one-time password for the registered phone number and
     * returns it in the response body (simulation – replace with SMS gateway).
     */
    @PostMapping("/generate-otp")
    public ResponseEntity<Map<String, Object>> generateOtp(@RequestBody Map<String, String> body) {
        String phone = body.get("phoneNumber");
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "phoneNumber is required"
            ));
        }
        try {
            String otp = visitorOtpService.generateOtp(phone.trim());
            // TODO: remove otp from response once SMS gateway is integrated
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "otp",     otp,          // simulated; remove when SMS is live
                    "message", "OTP sent to " + phone + " (mock)"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "success",    false,
                    "errorCode",  e.getMessage(),
                    "message",    "Mobile number not registered. Please register first."
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(429).body(Map.of(
                    "success",   false,
                    "errorCode", e.getMessage(),
                    "message",   "Too many OTP requests. Please try again later."
            ));
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
        if (phone == null || otp == null || phone.isBlank() || otp.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "phoneNumber and otp are required"
            ));
        }
        try {
            String jwt = visitorOtpService.validateOtpAndLogin(phone.trim(), otp.trim());
            Optional<Person> visitor = personRepository.findByPhoneNumber(phone.trim());
            return ResponseEntity.ok(Map.of(
                    "success",  true,
                    "token",    jwt,
                    "fullName", visitor.map(Person::getFullName).orElse("Visitor"),
                    "visitorId", visitor.map(Person::getId).orElse(0L),
                    "role",     "PUBLIC",
                    "message",  "Login successful"
            ));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null && e.getMessage().startsWith("OTP_INVALID:")
                    ? "Incorrect OTP. " + e.getMessage().split(":")[1] + " attempts remaining."
                    : "Mobile number not found.";
            return ResponseEntity.status(401).body(Map.of(
                    "success",   false,
                    "errorCode", e.getMessage(),
                    "message",   msg
            ));
        } catch (IllegalStateException e) {
            String msg = "OTP_EXPIRED_OR_NOT_FOUND".equals(e.getMessage())
                    ? "OTP has expired or was not found. Please generate a new OTP."
                    : "Maximum OTP attempts exceeded. Please generate a new OTP.";
            return ResponseEntity.status(401).body(Map.of(
                    "success",   false,
                    "errorCode", e.getMessage(),
                    "message",   msg
            ));
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
        if (dto.getFullName() == null || dto.getFullName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Full name is required"));
        }
        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Phone number is required"));
        }
        if (dto.getPhoneNumber().length() != 10) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Phone number must be 10 digits"));
        }

        // Duplicate mobile check
        if (personRepository.findByPhoneNumber(dto.getPhoneNumber()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of(
                    "success",   false,
                    "errorCode", "MOBILE_ALREADY_REGISTERED",
                    "message",   "This mobile number is already registered. Please login instead."
            ));
        }

        // Validate EPIC format if provided (alphanumeric, typically 3 letters + 7 digits)
        if (dto.getEpicNumber() != null && !dto.getEpicNumber().isBlank()) {
            if (!dto.getEpicNumber().matches("^[A-Z]{3}[0-9]{7}$")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success",   false,
                        "errorCode", "INVALID_EPIC_FORMAT",
                        "message",   "EPIC number must be 3 uppercase letters followed by 7 digits (e.g. ABC1234567)"
                ));
            }
        }

        // Validate Aadhaar format if provided (exactly 12 digits)
        if (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().isBlank()) {
            if (!dto.getAadhaarNumber().matches("^[0-9]{12}$")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success",   false,
                        "errorCode", "INVALID_AADHAAR_FORMAT",
                        "message",   "Aadhaar number must be exactly 12 digits"
                ));
            }
        }

        // Determine KYC type
        String kycType = "NONE";
        if (dto.getEpicNumber() != null && !dto.getEpicNumber().isBlank()) {
            kycType = "EPIC";
        } else if (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().isBlank()) {
            kycType = "AADHAAR";
        }

        // Persist visitor with KYC status PENDING (kycVerified = false)
        Person visitor = Person.builder()
                .fullName(dto.getFullName().trim())
                .phoneNumber(dto.getPhoneNumber().trim())
                .email(dto.getEmail())
                .epicNumber(dto.getEpicNumber())
                .aadhaarNumber(dto.getAadhaarNumber())
                .kycType(kycType)
                .kycVerified(false)
                .address(dto.getAddress())
                .designation(dto.getDesignation())
                .district(dto.getDistrict())
                .constituency(dto.getConstituency())
                .booth(dto.getBooth())
                .village(dto.getVillage())
                .photoStoragePath(dto.getPhotoStoragePath())
                .build();
        Person saved = personRepository.save(visitor);

        return ResponseEntity.ok(Map.of(
                "success",    true,
                "visitorId",  saved.getId(),
                "kycStatus",  "PENDING",
                "kycType",    kycType,
                "message",    "Registration successful. Please login with your mobile number."
        ));
    }

    // ── 5. Get profile ────────────────────────────────────────────────────────

    /**
     * Returns the profile of the currently authenticated visitor.
     * Caller must provide a valid JWT bearer token issued by /validate-otp.
     */
    @GetMapping("/profile/{visitorId}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable Long visitorId) {
        return personRepository.findById(visitorId)
                .map(p -> ResponseEntity.ok(Map.<String, Object>of(
                        "success",     true,
                        "id",          p.getId(),
                        "fullName",    p.getFullName(),
                        "phoneNumber", p.getPhoneNumber() != null ? p.getPhoneNumber() : "",
                        "epicNumber",  p.getEpicNumber()  != null ? p.getEpicNumber()  : "",
                        "aadhaarNumber", p.getAadhaarNumber() != null ? p.getAadhaarNumber() : "",
                        "kycType",     p.getKycType()     != null ? p.getKycType()     : "NONE",
                        "kycVerified", Boolean.TRUE.equals(p.getKycVerified()),
                        "address",     p.getAddress()     != null ? p.getAddress()     : "",
                        "district",    p.getDistrict()    != null ? p.getDistrict()    : ""
                )))
                .orElse(ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Visitor not found"
                )));
    }
}
