package com.survisha.meghaconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for OVSE QR code generation request to UIDAI service.
 * This is sent to https://ovse.aadhaarkyc.com/OvseMWService/OvseRequest
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvseRequestDto {
    private String apiKey;
    private String appId;
    private String txnId;
    private boolean qrFlag;
    private boolean gwt;
    private boolean online;
    private boolean faceAuth;
}
