package com.survisha.meghaconnect.dto;

import lombok.*;

/**
 * Request payload for a citizen's public self-registration.
 *
 * KYC priority: EPIC first.  If the applicant does not have an EPIC
 * (e.g. minor, new citizen), aadhaarNumber must be provided instead.
 * The backend will attempt API verification in the order:
 *   1. Election Commission API  (when epicNumber is present)
 *   2. UIDAI API                (when only aadhaarNumber is present)
 *
 * Photos and scanned documents are uploaded separately via the
 * /api/v1/storage/upload endpoint, which returns a storagePath that
 * should be referenced in photoStoragePath / documentPath fields.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PublicRegistrationDto {

    // ── Basic identity ───────────────────────────────────────────
    private String fullName;        // required
    private String phoneNumber;     // required; also used as login OTP target
    private String email;           // optional

    // ── KYC: EPIC (primary) ─ Aadhaar (fallback) ────────────────
    /** Voter ID / Election Photo Identity Card number.  Verified via
     *  Election Commission API.  Provide this whenever available. */
    private String epicNumber;

    /** 12-digit Aadhaar number.  Verified via UIDAI API.  Use only
     *  when the applicant does not have an EPIC. */
    private String aadhaarNumber;

    // ── File-store paths (set AFTER upload via /api/v1/storage/upload) ──
    /**
     * Relative path / object-key of the live photo in the file store.
     * Example: "registrations/abc123/photo.jpg"
     * Full URL is resolved using meghaconnect.storage.base-url in application.yml.
     */
    private String photoStoragePath;

    /** Path to the uploaded EPIC scan (PDF/image). */
    private String epicScanPath;

    /** Path to the uploaded Aadhaar scan (PDF/image). */
    private String aadhaarScanPath;

    // ── Demographics ─────────────────────────────────────────────
    private String dateOfBirth;     // ISO-8601 date string (yyyy-MM-dd)
    private String gender;
    private String state;
    private String designation;
    private String district;
    private String constituency;
    private String booth;
    private String village;
    private String address;

    // ── KYC result ───────────────────────────────────────────────
    /**
     * Granular KYC status from the registration flow:
     * PENDING | PHOTO_MATCHED | DEMOGRAPHIC_MATCHED | FAILED | MANUAL_VERIFICATION_REQUIRED
     */
    private String kycStatus;
    private String kycReferenceId;
    private String maskedIdentityNumber;
    private String relativeNameOnVoterId;
    private String borrowerAddressHouseNumber;
    private String borrowerAddressSectionNumber;
    private String pollingPartNo;
    private String pollingStationAddress;
    private Integer nameMatchScore;
    private Boolean idFound;
    private String voterIdVerificationRequestId;
    private String voterIdVerificationCompletionTimestamp;
    private String aadhaarClientTxnId;
    private String aadhaarAppId;

    /**
     * Base64-encoded live photo captured during KYC.
     * Stored in the database for demo; production should use object storage.
     */
    private String livePhotoBase64;

    /**
     * Flag indicating the user provided a manual phone number (not retrieved from ID API).
     * When true, kycStatus is set to MANUAL_VERIFICATION_REQUIRED.
     */
    private Boolean manualVerification;
}
