package com.survisha.meghaconnect.epic.face.dto.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Exact top-level response contract returned by FaceService1N. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceSearch1NProviderResponse {
    private boolean error;
    private String errorCode;
    private String errorDesc;
    private boolean matched;
    private String epicNumber;
    private String name;
    private String address;
    private String serialNumber;
    private String partNumber;
    private String partName;
    private String acpcName;
    private String district;
    private String pincode;
    /** Provider EPIC reference photo as raw Base64; never use as the visitor live photo. */
    private String photo;
}
