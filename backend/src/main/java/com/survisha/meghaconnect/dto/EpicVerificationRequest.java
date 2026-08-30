package com.survisha.meghaconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for EPIC verification against Election Commission API.
 * 
 * Maps to external API requirement:
 *   POST https://devuat.offlinekyc.com/ECSOVDServiceV2/api/ovd/verify
 *   Body: {
 *     "txnType": "VID_VERIFICATION",
 *     "apiKey": "...",
 *     "voterIdNumber": "BCV0259184",
 *     "nameOnVoterCard": "MAREIAM MOSSANG",
 *     "consumerIdentifier": "ref-vid-001"
 *   }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpicVerificationRequest {
    private String epicNumber;           // EPIC number (e.g., BCV0259184)
    private String visitorName;          // Name on voter card (for matching)
    private String phoneNumber;          // Optional: mobile for OTP sending
    private Boolean consentGranted;
    private String consentVersion;
    private String consentChannel;
}
