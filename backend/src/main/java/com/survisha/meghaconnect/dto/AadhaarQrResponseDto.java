package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for QR code generation endpoint.
 * Sent to frontend to display the OVSE QR code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AadhaarQrResponseDto {
    private boolean success;
    private String txnId;
    private String qrDataUri;           // data:image/png;base64,...
    private String errorMessage;
    private String maskedMobile;        // For user reference
    private Boolean canProceed;
    private String kycStatus;
    private String kycProvider;
    @Builder.Default
    private String requestId = RequestContextUtil.getRequestId();
}
