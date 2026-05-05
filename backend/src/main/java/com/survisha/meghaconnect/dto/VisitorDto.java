package com.survisha.meghaconnect.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VisitorDto {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String epicNumber;
    /** Aadhaar number – KYC fallback when EPIC is unavailable. */
    private String aadhaarNumber;
    /** EPIC | AADHAR | NONE */
    private String kycType;
    private Boolean kycVerified;
    private String designation;
    private String addressLine;
    private String district;
    private String constituency;
    private String booth;
    private String boothVillage;
    private String village;
    private Boolean outsideMeghalaya;
    private String location;
    private String briefProfile;
    /** Relative path/key in the file store (e.g. "visitors/42/photo.jpg"). */
    private String photoStoragePath;
    private String livePhotoPath;
}
