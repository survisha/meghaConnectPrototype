package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.EpicVerificationRequest;
import com.survisha.meghaconnect.dto.EpicVerificationResponse;
import com.survisha.meghaconnect.dto.EpicVerificationData;
import com.survisha.meghaconnect.dto.PollingDetails;
import com.survisha.meghaconnect.exception.EpicNameMismatchException;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.ExternalServiceException;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.util.DateTimeUtil;
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

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
 *   3. API returns verification status, district, state, gender, and polling details
 *   4. Service returns matched claims or error
 *   5. Frontend displays matched details and sends OTP if applicable
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EpicVerificationService {

    private final RestTemplate restTemplate;

    @Value("${epic.api.endpoint:https://devuat.offlinekyc.com/ECSOVDServiceV2}")
    private String epicApiEndpoint;

    @Value("${epic.api.key:}")  // Will be provided as env var or application property
    private String epicApiKey;

    @Value("${epic.api.enabled:false}")
    private boolean epicApiEnabled;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;
    
    // Local name comparison threshold. The EPIC API currently returns a fixed
    // low namematchscore, so validation must compare input and returned names.
    private static final double LOCAL_NAME_SIMILARITY_THRESHOLD = 0.85;

    /**
     * Verify EPIC against Election Commission API.
     *
     * @param request Contains epicNumber, visitorName (for name matching), and optional phoneNumber
     * @return Response with verification status and matched claims
     */
    public EpicVerificationResponse verifyEpic(EpicVerificationRequest request) {

        log.info("EPIC verification requested epic={} apiEnabled={}", maskEpic(request.getEpicNumber()), epicApiEnabled);

        // If API is disabled, return mock response for development
        if (!epicApiEnabled || epicApiKey.isBlank()) {
            if (isProductionProfile()) {
                log.error("EPIC API disabled or API key missing in production profile.");
                return EpicVerificationResponse.builder()
                        .code("503")
                        .success(false)
                        .canProceed(true)
                        .kycStatus("KYC_PENDING")
                        .kycProvider("EPIC")
                        .message("EPIC verification service is not configured.")
                        .build();
            }
            log.warn("EPIC API disabled or API key missing. Returning mock response.");
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

            log.info("Calling live Election Commission API for EPIC verification");

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
                log.info("EPIC verification successful status={}",
                        result.getData() != null ? result.getData().getVoterIdVerificationStatus() : "N/A");
                
                // Validate name match - voteridverificationstatus "id_found" only confirms EPIC exists,
                // NOT that the name matches. We must validate the name separately.
                validateNameMatch(request, result);
                
                return result;
            } else {
                String errorMsg = result != null ? result.getMessage() : "Unknown error";
                log.warn("EPIC verification failed: {}", errorMsg);
                if (isProviderNameMismatch(result)) {
                    return buildGenericNameMismatchResponse();
                }
                return result != null ? result : EpicVerificationResponse.builder()
                        .code("500")
                        .message("Error parsing response from EPIC API")
                        .build();
            }

        } catch (MeghaConnectException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("EPIC API request failed", e);
            return EpicVerificationResponse.builder()
                    .code("503")
                    .message("Election Commission API is currently unavailable")
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error during EPIC verification", e);
            return EpicVerificationResponse.builder()
                    .code("500")
                    .message(ErrorCodeConstants.GENERAL_ERROR_MSG)
                    .build();
        }
    }

    private boolean isProductionProfile() {
        return activeProfiles != null && Arrays.stream(activeProfiles.split(","))
                .map(String::trim)
                .anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));
    }

    /**
     * Mock EPIC verification for development when API is unavailable.
     */
    private EpicVerificationResponse mockVerifyEpic(EpicVerificationRequest request) {
        log.info("Using mock EPIC verification response");

        PollingDetails pollingDetails = PollingDetails.builder()
                .pollingPartNo("14")
                .pollingstationpartname("NAMPONG TANGSA COMMUNITY HALL")
                .build();

        EpicVerificationData mockData = EpicVerificationData.builder()
                .voterIdNumber(request.getEpicNumber())
                .borrowerNameOnVoterIdCard(request.getVisitorName().toUpperCase())
                .relativeNameOnVoterId("RELATIVE NAME")
                .borrowerGender("M")
                .borrowerDateOfBirth("")
                .borrowerAddressState("Meghalaya")
                .borrowerAddressDistrict("East Khasi Hills")
                .assemblyConstituencyNumber("46")
                .assemblyConstituencyName("Kukatpally")
                .borrowerAddressHouseNumber("Not Available")
                .borrowerAddressSectionNumber("1")
                .accountNumber("123")
                .nameMatchScore(95)
                .voterIdVerificationStatus("id_found")
                .sourceInformation("government_website")
                .pollingDetails(pollingDetails)
                .voterIdVerificationRequestId(UUID.randomUUID().toString())
                .voterIdVerificationCompletionTimestamp(DateTimeUtil.nowIST().toString())
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
     * Important: voteridverificationstatus "id_found" only means the EPIC number exists.
     * The external API's namematchscore is not reliable for this integration,
     * so the service compares the input name with borrowernameonvoteridcard locally.
     *
     * @param request Input request containing the name from UI
     * @param response Response from Election Commission API
     */
    private void validateNameMatch(EpicVerificationRequest request, EpicVerificationResponse response) {
        
        if (response.getData() == null) {
            throw new ExternalServiceException(
                    "EPIC",
                    "EPIC verification succeeded but response data was missing"
            );
        }
        
        String verifiedName = response.getVerifiedName();
        String inputName = request.getVisitorName();
        
        // Log the name validation details
        log.info("Validating EPIC voter name match");

        if (isBlank(inputName) || isBlank(verifiedName)) {
            log.warn("EPIC name validation failed: input or verified name is missing");
            throw new EpicNameMismatchException(response.getData());
        }
        
        String normalizedInput = normalizeName(inputName);
        String normalizedVerified = normalizeName(verifiedName);
        
        double similarity = calculateSimilarity(normalizedInput, normalizedVerified);
        boolean tokenMatch = hasSameNameTokens(normalizedInput, normalizedVerified);
        
        log.info("EPIC local name comparison similarity={} tokenMatch={}",
                String.format("%.1f%%", similarity * 100), tokenMatch);
        
        if (!tokenMatch && similarity < LOCAL_NAME_SIMILARITY_THRESHOLD) {
            log.warn("EPIC name mismatch detected");
            throw new EpicNameMismatchException(response.getData());
        }

        // Make downstream UI confidence meaningful without relying on the API's
        // fixed namematchscore value.
        response.getData().setNameMatchScore((int) Math.round(similarity * 100));
        
        log.info("EPIC name validation passed");
    }
    
    /**
     * Normalize a name for comparison: uppercase, remove accents/punctuation, collapse spaces.
     */
    private String normalizeName(String str) {
        if (str == null) return "";
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized
                .toUpperCase()
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean hasSameNameTokens(String inputName, String verifiedName) {
        Set<String> inputTokens = nameTokens(inputName);
        Set<String> verifiedTokens = nameTokens(verifiedName);
        return !inputTokens.isEmpty() && inputTokens.equals(verifiedTokens);
    }

    private Set<String> nameTokens(String name) {
        return Arrays.stream(name.split(" "))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isProviderNameMismatch(EpicVerificationResponse response) {
        if (response == null || response.getMessage() == null) {
            return false;
        }
        String message = response.getMessage().toLowerCase();
        return message.contains("name mismatch")
                || message.contains("match confidence")
                || message.contains("verified name")
                || message.contains("voter id name")
                || message.contains("epic card matches");
    }

    private EpicVerificationResponse buildGenericNameMismatchResponse() {
        return EpicVerificationResponse.builder()
                .code("400")
                .message(ErrorCodeConstants.EPIC_NAME_MISMATCH_MSG)
                .build();
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
