package com.survisha.meghaconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for OVSE response from UIDAI service.
 * Contains QR code (base64 PNG) or error information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvseResponseDto {
    private boolean successful;
    private String qrCode;              // Base64-encoded PNG
    private String errorCode;
    private String errorDescription;
    private String txnId;
}
