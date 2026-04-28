package com.survisha.meghaconnect.dto;

import lombok.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Immutable snapshot of one KYC outcome — either a successful set of disclosed
 * resident claims, or an error payload — as forwarded by the
 * {@code OvseCallback} endpoint.
 *
 * <p>Instances are created by {@link #success(String, Map)} or
 * {@link #error(String, String, String)} and read by the verification clients.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycData implements Serializable {

    private static final long serialVersionUID = 1L;

    // ─────────────────────────────────────────────────────────────
    // Well-known claim names (as they arrive from the SD-JWT disclosures).
    // ─────────────────────────────────────────────────────────────
    public static final String CLAIM_RESIDENT_NAME  = "residentName";
    public static final String CLAIM_LOCAL_NAME     = "localResidentName";
    public static final String CLAIM_DOB            = "dob";
    public static final String CLAIM_GENDER         = "gender";
    public static final String CLAIM_MOBILE         = "mobile";
    public static final String CLAIM_MASKED_MOBILE  = "maskedMobile";
    public static final String CLAIM_EMAIL          = "email";
    public static final String CLAIM_MASKED_EMAIL   = "maskedEmail";
    public static final String CLAIM_PHOTO          = "residentImage";
    public static final String CLAIM_ADDRESS        = "address";
    public static final String CLAIM_REG_ADDRESS    = "regionalAddress";

    private boolean error;
    private String  errorCode;
    private String  errorMessage;
    private String  txnId;
    private Map<String, String> claims;
    private long receivedAtMillis;

    // Static factory methods
    public static KycData success(String txnId, Map<String, String> claims) {
        KycData data = new KycData();
        data.error = false;
        data.errorCode = null;
        data.errorMessage = null;
        data.txnId = txnId;
        if (claims == null || claims.isEmpty()) {
            data.claims = Collections.emptyMap();
        } else {
            // Case-insensitive mapping for claim lookups
            TreeMap<String, String> ci = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            ci.putAll(claims);
            data.claims = Collections.unmodifiableMap(ci);
        }
        data.receivedAtMillis = System.currentTimeMillis();
        return data;
    }

    public static KycData error(String txnId, String errorCode, String errorMessage) {
        KycData data = new KycData();
        data.error = true;
        data.errorCode = errorCode;
        data.errorMessage = errorMessage;
        data.txnId = txnId;
        data.claims = Collections.emptyMap();
        data.receivedAtMillis = System.currentTimeMillis();
        return data;
    }

    // ─────────────────────────────────────────────────────────────
    // Convenience accessors used by the registration page.
    // ─────────────────────────────────────────────────────────────

    /**
     * @return the resident's English name, or the local name as a fallback.
     */
    public String getResidentName() {
        if (claims == null || claims.isEmpty()) return "";
        String name = claims.get(CLAIM_RESIDENT_NAME);
        if (name != null && !name.isBlank()) return name;
        return claims.getOrDefault(CLAIM_LOCAL_NAME, "");
    }

    public String getDob() {
        return claims != null ? claims.getOrDefault(CLAIM_DOB, "") : "";
    }

    public String getGender() {
        return claims != null ? claims.getOrDefault(CLAIM_GENDER, "") : "";
    }

    /**
     * @return the full mobile if disclosed, otherwise the masked form.
     */
    public String getMobile() {
        if (claims == null || claims.isEmpty()) return "";
        String m = claims.get(CLAIM_MOBILE);
        if (m != null && !m.isBlank()) return m;
        return claims.getOrDefault(CLAIM_MASKED_MOBILE, "");
    }

    /**
     * @return the full email if disclosed, otherwise the masked form.
     */
    public String getEmail() {
        if (claims == null || claims.isEmpty()) return "";
        String e = claims.get(CLAIM_EMAIL);
        if (e != null && !e.isBlank()) return e;
        return claims.getOrDefault(CLAIM_MASKED_EMAIL, "");
    }

    /**
     * @return a best-effort English address, else the regional one.
     */
    public String getAddress() {
        if (claims == null || claims.isEmpty()) return "";
        String a = claims.get(CLAIM_ADDRESS);
        if (a != null && !a.isBlank()) return a;
        return claims.getOrDefault(CLAIM_REG_ADDRESS, "");
    }

    /**
     * @return the Base64-encoded JPEG/PNG of the resident photo, or null if not disclosed.
     *         Callers should embed as {@code data:image/jpeg;base64,<value>}.
     */
    public String getPhotoBase64() {
        if (claims == null || claims.isEmpty()) return null;
        String v = claims.get(CLAIM_PHOTO);
        return (v == null || v.isBlank()) ? null : v;
    }
}
