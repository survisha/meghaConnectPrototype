package com.survisha.meghaconnect.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrScanRequest {

    @NotBlank
    private String qrToken;

    private String qrData;

    @NotBlank
    private String deviceId;

    private String gateName;
    private String location;
}
