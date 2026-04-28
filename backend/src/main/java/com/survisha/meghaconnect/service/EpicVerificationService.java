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
    
    // Name match score threshold (0-100 scale)
    // Scores below this are treated as name mismatch
    private static final int NAME_MATCH_THRESHOLD = 60;

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
        LOG.info("API Endpoint: " + epicApiEndpoint);
        LOG.info("API Enabled: " + epicApiEnabled);

        // If API is disabled, return mock response for development
        if (!epicApiEnabled || epicApiKey.isBlank()) {
            LOG.warning("⚠ EPIC API disabled or API key missing. Returning mock response.");
            LOG.warning("  To enable real API, set epic.api.enabled=true and epic.api.key=<key> in application.properties or environment variables");
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

            LOG.info("✓ Calling LIVE Election Commission API: " + epicApiEndpoint + "/api/ovd/verify");

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
                
                // Validate name match - voteridverificationstatus "id_found" only confirms EPIC exists,
                // NOT that the name matches. We must validate the name separately.
                EpicVerificationResponse nameValidationResult = validateNameMatch(request, result);
                if (nameValidationResult != null) {
                    // Name mismatch detected
                    return nameValidationResult;
                }
                
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

    /**
     * Validate that the input name matches the verified name from EPIC verification.
     * 
     * Important: voteridverificationstatus "id_found" only means the EPIC number exists,
     * it does NOT confirm the name matches. We must validate the name separately using:
     * 1. namematchscore (0-100 scale) - must exceed threshold (60)
     * 2. Fuzzy comparison of input name vs verified name
     *
     * @param request Input request containing the name from UI
     * @param response Response from Election Commission API
     * @return Error response if name doesn't match, or null if validation passes
     */
    private EpicVerificationResponse validateNameMatch(EpicVerificationRequest request, EpicVerificationResponse response) {
        
        if (response.getData() == null) {
            return EpicVerificationResponse.builder()
                    .code("400")
                    .message("Name validation failed: No data in response")
                    .build();
        }
        
        Integer nameMatchScore = response.getNameMatchScore();
        String verifiedName = response.getVerifiedName();
        String inputName = request.getVisitorName();
        
        // Log the name validation details
        LOG.info("=== Name Validation ===");
        LOG.info("Input Name: " + inputName);
        LOG.info("Verified Name: " + verifiedName);
        LOG.info("Name Match Score: " + nameMatchScore + " (Threshold: " + NAME_MATCH_THRESHOLD + ")");
        
        // Check 1: Name match score must be above threshold
        // A score of 5 indicates very low match (almost no similarity)
        if (nameMatchScore == null || nameMatchScore < NAME_MATCH_THRESHOLD) {
            String errorMsg = String.format(
                    "Name mismatch detected. Input name '%s' does not match verified name '%s'. " +
                    "Match confidence: %d%% (minimum required: %d%%). " +
                    "Please verify the name on your EPIC card matches exactly.",
                    inputName, verifiedName,
                    nameMatchScore != null ? nameMatchScore : 0,
                    NAME_MATCH_THRESHOLD
            );
            LOG.warning("✗ " + errorMsg);
            return EpicVerificationResponse.builder()
                    .code("400")
                    .message(errorMsg)
                    .data(response.getData())  // Include data for UI reference
                    .build();
        }
        
        // Check 2: Perform basic fuzzy matching (trim and normalize for comparison)
        String normalizedInput = normalizeString(inputName);
        String normalizedVerified = normalizeString(verifiedName);
        
        // Calculate similarity percentage
        double similarity = calculateSimilarity(normalizedInput, normalizedVerified);
        
        LOG.info("✓ Normalized similarity: " + String.format("%.1f%%", similarity * 100));
        
        if (similarity < 0.70 && nameMatchScore < 80) {
            // High mismatch in fuzzy match AND API score is moderate
            String errorMsg = String.format(
                    "Name verification failed. Your input '%s' is significantly different from " +
                    "the verified name '%s'. Match confidence: %d%%. " +
                    "Please ensure the name matches exactly as it appears on your EPIC card.",
                    inputName, verifiedName, nameMatchScore
            );
            LOG.warning("✗ " + errorMsg);
            return EpicVerificationResponse.builder()
                    .code("400")
                    .message(errorMsg)
                    .data(response.getData())
                    .build();
        }
        
        LOG.info("✓ Name validation passed");
        return null;  // Null indicates validation passed, proceed with success
    }
    
    /**
     * Normalize a string for comparison: trim, lowercase, remove extra spaces
     */
    private String normalizeString(String str) {
        if (str == null) return "";
        return str.trim().toLowerCase().replaceAll("\\s+", " ");
    }
    
    /**
     * Calculate similarity between two strings using Levenshtein distance.
     * Returns a value between 0.0 (completely different) and 1.0 (identical).
     */
    private double calculateSimilarity(String str1, String str2) {
        if (str1.equals(str2)) return 1.0;
        
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) return 1.0;  // Both empty
        
        int distance = levenshteinDistance(str1, str2);
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * Calculate Levenshtein distance between two strings.
     * This is the minimum number of single-character edits needed to change one word to another.
     */
    private int levenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];
        
        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                int cost = str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(
                                dp[i - 1][j] + 1,      // deletion
                                dp[i][j - 1] + 1       // insertion
                        ),
                        dp[i - 1][j - 1] + cost        // substitution
                );
            }
        }
        
        return dp[str1.length()][str2.length()];
    }
}
