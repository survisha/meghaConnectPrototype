package gov.meghalaya.meghaconnect.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
public class KycController {

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

        return ResponseEntity.ok(Map.of(
            "kycType",        "EPIC",
            "idValue",        epic,
            "provider",       "ELECTION_COMMISSION_API",
            "verified",       true,
            "verifiedName",   "Verification pending API integration",
            "nameMatchScore", 0,
            "message",        "Mock response – live API integration pending credentials"
        ));
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
            return ResponseEntity.badRequest().body(Map.of(
                "verified", false,
                "message",  "Invalid Aadhaar number – must be 12 digits"
            ));
        }

        // TODO: Replace with call to UIDAI API adapter
        // KycProvider provider = kycProviderFactory.getProvider("UIDAI_API");
        // KycResult result = provider.verify(aadhaar, name);

        String maskedAadhaar = "XXXX-XXXX-" + aadhaar.substring(aadhaar.length() - 4);
        return ResponseEntity.ok(Map.of(
            "kycType",        "AADHAAR",
            "idValue",        maskedAadhaar,          // full number is never echoed
            "provider",       "UIDAI_API",
            "verified",       true,
            "verifiedName",   "Verification pending API integration",
            "nameMatchScore", 0,
            "message",        "Mock response – live API integration pending credentials"
        ));
    }
}
