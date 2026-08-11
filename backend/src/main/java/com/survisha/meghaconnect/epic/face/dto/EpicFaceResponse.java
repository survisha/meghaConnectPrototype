package com.survisha.meghaconnect.epic.face.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EpicFaceResponse {
    boolean matched;
    String epicNumber;
    String name;
    String address;
    String serialNumber;
    String partNumber;
    String partName;
    String acpcName;
    String district;
    String pincode;
    String epicPhoto;
    String source;
    String providerStatus;
}
