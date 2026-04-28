package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.EpicVerificationRequest;
import com.survisha.meghaconnect.dto.EpicVerificationResponse;
import com.survisha.meghaconnect.dto.EpicVerificationData;
import com.survisha.meghaconnect.dto.PollingDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for EPIC (Voter ID) verification against Election Commission API.
 *
 * Integrates with external API:
 *   Endpoint: https://devuat.offlinekyc.com/ECSOVDServiceV2/api/ovd/verify
 *   Payment: VID_VERIFICATION transaction type
 *   Auth: apiKey (to be provided by Election Commission)
 *
 * Flow:
 *   1. Frontend sends EPIC number + visitorName to POST /api/v1/kyc/verify/epic
 *   2. Service calls external Election Commission API with credentials
 *   3. API returns verification status, district, state, and name match score
 *   4. Service returns matched claims or error
 *   5. Frontend displays matched details and sends OTP if applicable
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EpicVerificationService {

    private static final Logger LOG = Logger.getLogger(EpicVerificationService.class.getName());

    private final RestTemplate restTemplate;

    @Value("${epic.api.endpoint:https://devuat.offlinekyc.com/ECSOVDServiceV2}")
    private String epicApiEndpoint;

    @Value("${epic.api.key:}")  // Will be provided as env var or application property
    private String epicApiKey;

    @Value("${epic.api.enabled:false}")
    private boolean epicApiEnabled;

    /**
     * Verify EPIC against Election Commission API.
     *
     * @param request Contains epicNumber, visitorName (for name matching), and optional phoneNumber
     * @return Response with verification status and matched claims
     */
    public EpicVerificationResponse verifyEpic(EpicVerificationRequest request) {

        LOG.info("=== EPIC Verification Request ===");
        LOG.info("EPIC Number: " + maskEpic(request.getEpicNumber()));
        LOG.info("Visitor Name: " + request.getVisitorName());

        // If API is disabled, return mock response for development
        if (!epicApiEnabled || epicApiKey.isBlank()) {
            LOG.warning("⚠ EPIC API disabled or API key missing. Returning mock response.");
            LOG.warning("  To enable, set epic.api.enabled=true and epic.api.key=<key> in application properties");
            return mockVerifyEpic(request);
        }

        try {
            // Build request to external API
            Map<String, Object> apiRequest = new HashMap<>();
            apiRequest.put("txnType", "VID_VERIFICATION");
            apiRequest.put("apiKey", epicApiKey);
            apiRequest.put("voterIdNumber", request.getEpicNumber());
            apiRequest.put("nameOnVoterCard", request.getVisitorName().toUpperCase());
            apiRequest.put("consumerIdentifier", "ref-vid-" + UUID.randomUUID().toString().substring(0, 8));

            LOG.info("✓ Calling Election Commission API: " + epicApiEndpoint + "/api/ovd/verify");

            // Make HTTP request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(apiRequest, headers);
            ResponseEntity<EpicVerificationResponse> response = restTemplate.postForEntity(
                    epicApiEndpoint + "/api/ovd/verify",
                    entity,
                    EpicVerificationResponse.class
            );

            EpicVerificationResponse result = response.getBody();

            if (result != null && result.isSuccess()) {
                LOG.info("✓ EPIC verification successful");
                LOG.info("  Status: " + (result.getData() != null ? result.getData().getVoterIdVerificationStatus() : "N/A"));
                LOG.info("  Verified Name: " + result.getVerifiedName());
                LOG.info("  District: " + result.getDistrict());
                LOG.info("  Name Match Score: " + result.getNameMatchScore());
                return result;
            } else {
                String errorMsg = result != null ? result.getMessage() : "Unknown error";
                LOG.warning("✗ EPIC verification failed: " + errorMsg);
                return result != null ? result : EpicVerificationResponse.builder()
                        .code("500")
                        .message("Error parsing response from EPIC API")
                        .build();
            }

        } catch (RestClientException e) {
            LOG.log(Level.SEVERE, "✗ EPIC API request failed", e);
            return EpicVerificationResponse.builder()
                    .code("503")
                    .message("Election Commission API unavailable: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "✗ Unexpected error during EPIC verification", e);
            return EpicVerificationResponse.builder()
                    .code("500")
                    .message("Unexpected error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Mock EPIC verification for development when API is unavailable.
     */
    private EpicVerificationResponse mockVerifyEpic(EpicVerificationRequest request) {
        LOG.info("↻ Using mock EPIC verification response");

        PollingDetails pollingDetails = PollingDetails.builder()
                .pollingPartNo("14")
                .pollingStationAddress("NAMPONG TANGSA COMMUNITY HALL")
                .build();

        EpicVerificationData mockData = EpicVerificationData.builder()
                .voterIdNumber(request.getEpicNumber())
                .borrowerNameOnVoterIdCard(request.getVisitorName().toUpperCase())
                .relativeNameOnVoterId("RELATIVE NAME")
                .borrowerGender("M")
                .borrowerDateOfBirth("")
                .borrowerAddressState("Meghalaya")
                .borrowerAddressDistrict("East Khasi Hills")
                .borrowerAddressHouseNumber("Not Available")
                .borrowerAddressSectionNumber("1")
                .accountNumber("123")
                .nameMatchScore(95)
                .voterIdVerificationStatus("id_found")
                .sourceInformation("government_website")
                .pollingDetails(pollingDetails)
                .voterIdVerificationRequestId(UUID.randomUUID().toString())
                .voterIdVerificationCompletionTimestamp(java.time.LocalDateTime.now().toString())
                .build();

        return EpicVerificationResponse.builder()
                .code("200")
                .message("Success")
                .data(mockData)
                .build();
    }

    /**
     * Mask EPIC number for logging (show only last 4 digits).
     */
    private String maskEpic(String epic) {
        if (epic == null || epic.length() < 4) return "****";
        return "****" + epic.substring(epic.length() - 4);
    }
}
