package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.*;
import com.survisha.meghaconnect.util.TxnIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

// Aadhaar OVSE SDK imports (com.ovse.client package)
import com.ovse.client.OvseClient;
import com.ovse.client.exception.OvseClientException;
import com.ovse.client.model.OvseRequestJson;
import com.ovse.client.model.OvseResponseJson;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for Aadhaar OVSE (Online Verification of Self-Employed) KYC workflow.
 *
 * Responsibilities:
 *   1. Generate QR code via UIDAI OvseClient SDK
 *   2. Store transaction state (txnId → KycData)
 *   3. Retrieve stored KYC results when callback is received
 *
 * Flow:
 *   1. Frontend calls POST /api/v1/kyc/aadhaar/generate-qr → returns txnId + QR PNG
 *   2. User scans QR with Aadhaar app
 *   3. Aadhaar app sends KYC data to callback endpoint
 *   4. Callback stores result in this service's store
 *   5. Frontend polls GET /api/v1/kyc/aadhaar/result/{txnId} → returns populated KycData
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OvseKycService {

    private static final Logger LOG = Logger.getLogger(OvseKycService.class.getName());

    @Value("${ovse.appId:100001}")
    private String appId;

    @Value("${ovse.apiKey:xwwvygA3MmtwbJmqBiBHsBWQ4xHzwu78}")
    private String apiKey;

    @Value("${ovse.endpoint:https://ovse.aadhaarkyc.com/OvseMWService/OvseRequest}")
    private String ovseEndpoint;

    @Value("${ovse.online:true}")
    private boolean online;

    @Value("${ovse.faceAuth:true}")
    private boolean faceAuth;

    private final RestTemplate restTemplate;

    // In-memory store for KYC results; in production, use Redis or database
    private static final ConcurrentHashMap<String, KycData> kycResultStore = new ConcurrentHashMap<>();

    /**
     * Generate OVSE QR code for Aadhaar KYC verification.
     *
     * The OVSE SDK generates a standard QR code based on appId and transaction ID.
     *
     * @return QR code as base64 data URI with transaction ID
     */
    public AadhaarQrResponseDto generateQrCode() {
        LOG.info("=== OVSE QR Generation Request ===");
        LOG.info("Generating generic QR code");

        try {
            // Generate transaction ID with appId prefix
            String txnId = TxnIdGenerator.generate(appId);
            LOG.info("Generated txnId: " + txnId);

            // Build OVSE request - following the sample code pattern
            OvseRequestDto qrRequest = OvseRequestDto.builder()
                    .apiKey(apiKey)
                    .appId(appId)
                    .txnId(txnId)
                    .qrFlag(true)        // Generate QR, not GWT
                    .gwt(false)          
                    .online(online)      // Online verification mode
                    .faceAuth(faceAuth)  // Face authentication enabled
                    .build();

            LOG.info("Sending OVSE request to: " + ovseEndpoint);
            LOG.info("  appId: " + appId);
            LOG.info("  txnId: " + txnId);
            LOG.info("  online: " + online);
            LOG.info("  faceAuth: " + faceAuth);

            // Call OVSE service to generate QR
            OvseResponseDto response = callOvseService(qrRequest);

            if (response != null && response.isSuccessful() && response.getQrCode() != null) {
                String qrDataUri = "data:image/png;base64," + response.getQrCode();

                // Store transaction state in memory for later polling
                KycData pendingKyc = KycData.builder()
                        .txnId(txnId)
                        .error(false)
                        .build();
                kycResultStore.put(txnId, pendingKyc);

                LOG.info(" QR code generated successfully");
                LOG.info("  txnId: " + txnId);
                LOG.info("  QR size: " + response.getQrCode().length() + " bytes (base64)");


                return AadhaarQrResponseDto.builder()
                        .success(true)
                        .txnId(txnId)
                        .qrDataUri(qrDataUri)
                        .build();
            } else {
                String errorMsg = response != null ?
                        String.format("OVSE error: %s — %s",
                                response.getErrorCode(),
                                response.getErrorDescription()) :
                        "OVSE returned null response";
                LOG.warning("✗ OVSE QR request failed: " + errorMsg);
                return AadhaarQrResponseDto.builder()
                        .success(false)
                        .errorMessage(errorMsg)
                        .build();
            }
        } catch (IllegalArgumentException e) {
            String errorMsg = "Invalid appId: " + e.getMessage();
            LOG.warning("✗ " + errorMsg);
            return AadhaarQrResponseDto.builder()
                    .success(false)
                    .errorMessage(errorMsg)
                    .build();
        } catch (OvseClientException e) {
            String errorMsg = "OVSE service error: " + e.getMessage() +
                    (e.getHttpStatus() > 0 ? " (HTTP " + e.getHttpStatus() + ")" : "");
            LOG.log(Level.SEVERE, "✗ " + errorMsg, e);
            return AadhaarQrResponseDto.builder()
                    .success(false)
                    .errorMessage(errorMsg)
                    .build();
        } catch (LinkageError e) {
            String errorMsg = "OVSE SDK class loading error: " + e.getMessage();
            LOG.log(Level.SEVERE, "✗ " + errorMsg, e);
            return AadhaarQrResponseDto.builder()
                    .success(false)
                    .errorMessage(errorMsg)
                    .build();
        } catch (Exception e) {
            String errorMsg = "Unexpected error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
            LOG.log(Level.SEVERE, "✗ " + errorMsg, e);
            return AadhaarQrResponseDto.builder()
                    .success(false)
                    .errorMessage(errorMsg)
                    .build();
        }
    }

    /**
     * Retrieve KYC verification result for a transaction.
     *
     * @param txnId Transaction ID from QR generation
     * @return KycData with verification status and claims, or null if not yet received
     */
    @Cacheable(value = "kycResults", key = "#txnId")
    public KycData getKycResult(String txnId) {
        LOG.fine("Polling for KYC result: txnId=" + txnId);
        return kycResultStore.get(txnId);
    }

    /**
     * Store KYC result received from OVSE callback.
     * Called by the callback endpoint when Aadhaar app submits consent.
     *
     * @param txnId     Transaction ID from callback payload
     * @param claims    Disclosed claims from Aadhaar (name, DOB, address, photo, etc.)
     */
    @CachePut(value = "kycResults", key = "#txnId")
    public KycData storeKycSuccess(String txnId, Map<String, String> claims) {
        LOG.info("Storing SUCCESS KYC result for txnId=" + txnId);
        KycData data = KycData.success(txnId, claims);
        kycResultStore.put(txnId, data);
        LOG.info(" KYC SUCCESS stored: " + claims.size() + " claims received");
        return data;
    }

    /**
     * Store KYC error result received from OVSE callback.
     * Called when user rejects consent or error occurs.
     *
     * @param txnId         Transaction ID
     * @param errorCode     Error code from Aadhaar app
     * @param errorMessage  Error description
     */
    @CachePut(value = "kycResults", key = "#txnId")
    public KycData storeKycError(String txnId, String errorCode, String errorMessage) {
        LOG.warning("Storing ERROR KYC result for txnId=" + txnId + ", error=" + errorCode);
        KycData data = KycData.error(txnId, errorCode, errorMessage);
        kycResultStore.put(txnId, data);
        return data;
    }

    /**
     * Call OVSE service to generate QR code using the UIDAI SDK.
     * 
     * Uses OvseClient from the OvseClientSDK20260422a.jar:
     *   • Connects to https://ovse.aadhaarkyc.com/OvseMWService/OvseRequest
     *   • Sends transaction request with appId, txnId, apiKey
     *   • Receives base64-encoded QR code PNG
     *
     * @param request OVSE request with credentials and transaction ID
     * @return OVSE response with QR code or error
     */
    private OvseResponseDto callOvseService(OvseRequestDto request) {
        LOG.info("Calling OVSE service using SDK...");
        
        try {
            // Build OVSE client using SDK
            OvseClient client = OvseClient.builder()
                    .endpointUrl(ovseEndpoint)
                    .connectTimeout(Duration.ofSeconds(5))
                    .requestTimeout(Duration.ofSeconds(20))
                    .build();

            LOG.info("✓ OvseClient created: " + client.getClass().getName());

            // Build OVSE request
            OvseRequestJson qrRequest = OvseRequestJson.builder()
                    .apiKey(request.getApiKey())
                    .appId(request.getAppId())
                    .txnId(request.getTxnId())
                    .qrFlag(request.isQrFlag())
                    .gwt(request.isGwt())
                    .online(request.isOnline())
                    .faceAuth(request.isFaceAuth())
                    .build();

            LOG.info("✓ OvseRequest built with txnId=" + request.getTxnId());

            // Invoke SDK to get QR code
            OvseResponseJson ovseResponse = client.invoke(qrRequest);

            LOG.info("✓ OVSE service responded: successful=" + ovseResponse.isSuccessful());

            return OvseResponseDto.builder()
                    .successful(ovseResponse.isSuccessful())
                    .qrCode(ovseResponse.getQrCode())                 // Base64 PNG
                    .errorCode(ovseResponse.getErrorCode())
                    .errorDescription(ovseResponse.getErrorDescription())
                    .txnId(request.getTxnId())
                    .build();

        } catch (OvseClientException e) {
            String errorMsg = "OVSE SDK OvseClientException: " + e.getMessage();
            LOG.log(Level.SEVERE, errorMsg, e);
            return OvseResponseDto.builder()
                    .successful(false)
                    .errorCode("OVSE_CLIENT_ERROR")
                    .errorDescription(errorMsg)
                    .build();
        } catch (LinkageError e) {
            // Catches ClassNotFoundException, NoClassDefFoundError, UnsupportedClassVersionError, etc.
            String errorMsg = "OVSE SDK classes not found on classpath. Ensure OvseClientSDK20260422a.jar is in libs/";
            LOG.log(Level.SEVERE, errorMsg, e);
            return OvseResponseDto.builder()
                    .successful(false)
                    .errorCode("SDK_LOAD_ERROR")
                    .errorDescription(errorMsg)
                    .build();
        } catch (Exception e) {
            String errorMsg = "Unexpected error during OVSE SDK call: " + e.getClass().getSimpleName();
            LOG.log(Level.SEVERE, errorMsg, e);
            return OvseResponseDto.builder()
                    .successful(false)
                    .errorCode("UNEXPECTED_ERROR")
                    .errorDescription(errorMsg + " - " + e.getMessage())
                    .build();
        }
    }

    /**
     * Mask phone number for display (show only last 4 digits).
     * Example: 9876543210 → ****3210
     * Empty string → empty string
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) return "";
        if (phone.length() < 4) return phone;
        return "****" + phone.substring(phone.length() - 4);
    }

    /**
     * Clear old results from store (for cleanup after success/error handling).
     * In production, implement TTL-based eviction via Redis.
     */
    public void clearOldResults(long ageMillis) {
        long cutoff = System.currentTimeMillis() - ageMillis;
        kycResultStore.entrySet().removeIf(entry ->
                entry.getValue().getReceivedAtMillis() < cutoff
        );
    }
}
