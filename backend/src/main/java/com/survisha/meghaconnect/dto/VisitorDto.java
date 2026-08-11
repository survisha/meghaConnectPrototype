package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.MobileOtpVerificationStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VisitorDto {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private MobileOtpVerificationStatus mobileOtpVerification;
    private String epicNumber;
    /** Aadhaar number – KYC fallback when EPIC is unavailable. */
    private String aadhaarNumber;
    /** EPIC | AADHAR | NONE */
    private String kycType;
    private String kycProvider;
    private Boolean kycVerified;
    private String kycStatus;
    private String kycFailureReason;
    private String kycRequestId;
    private LocalDate dateOfBirth;
    private String gender;
    private String designation;
    private String address;
    private String fullAddress;
    private String address1;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    private String district;
    private String constituency;
    private String assemblyConstituencyNumber;
    private String assemblyConstituencyName;
    private String booth;
    private String boothVillage;
    private String village;
    private Boolean outsideMeghalaya;
    private String location;
    private String briefProfile;
    private String agendaType;
    private String briefDescription;
    private String partNumber;
    /** Relative path/key in the file store (e.g. "visitors/42/photo.jpg"). */
    private String photoStoragePath;
    private String photoUrl;
    private String livePhotoPath;
    private String photoPath;
    private String livePhotoBase64;
    private String photoBase64;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
