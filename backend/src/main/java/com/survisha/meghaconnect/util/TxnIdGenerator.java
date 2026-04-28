package com.survisha.meghaconnect.util;

import java.util.UUID;

/**
 * Utility for generating OVSE transaction IDs that carry a 6-character appId
 * prefix while still looking like valid UUIDs to strict parsers.
 *
 * <p>Background: the OVSE callback endpoint is shared by multiple client
 * applications, so each generated JWT needs to carry an {@code appId} so the
 * callback can be routed back to the originating app. An earlier scheme
 * concatenated {@code appId + "-" + UUID} — but that produces a 6-group
 * string which fails Java's {@link UUID#fromString(String)} on strict
 * verifier implementations (Android's Aadhaar App in particular).</p>
 *
 * <p>This generator instead <em>overwrites</em> the first 6 hex characters of
 * a freshly-generated UUID with the appId. The result is still a
 * syntactically valid UUID (8-4-4-4-12 groups, 36 chars total) and can be
 * recovered by reading {@code substring(0, 6)} at the callback side.</p>
 *
 * <h3>Constraints on appId</h3>
 * <ul>
 *   <li>Must be exactly 6 characters.</li>
 *   <li>Must be hexadecimal — only {@code [0-9a-fA-F]} — so the output
 *       remains a syntactically valid UUID.</li>
 * </ul>
 */
public final class TxnIdGenerator {

    /** Required length of the appId prefix. */
    public static final int APP_ID_LENGTH = 6;

    private TxnIdGenerator() { /* utility class */ }

    /**
     * Generates a random UUID and overwrites its first 6 hex characters with
     * {@code appId}. Returns a 36-character string that is still a valid UUID.
     *
     * @param appId exactly 6 hex characters ({@code [0-9a-fA-F]})
     * @return the composite transaction ID
     * @throws IllegalArgumentException if {@code appId} is null, not exactly
     *         6 characters, or contains non-hex characters
     */
    public static String generate(String appId) {
        validateAppId(appId);
        String uuid = UUID.randomUUID().toString();        // 8-4-4-4-12
        return appId + uuid.substring(APP_ID_LENGTH);      // replace first 6 chars
    }

    /**
     * Returns the appId prefix from a txnId produced by {@link #generate}.
     *
     * @param txnId a 36-character string previously produced by this class
     * @return the 6-character appId
     * @throws IllegalArgumentException if {@code txnId} is null or shorter
     *         than 6 characters
     */
    public static String extractAppId(String txnId) {
        if (txnId == null || txnId.length() < APP_ID_LENGTH) {
            throw new IllegalArgumentException(
                "txnId must be at least " + APP_ID_LENGTH + " characters: " + txnId);
        }
        return txnId.substring(0, APP_ID_LENGTH);
    }

    // ─────────────────────────────────────────────────────────────

    private static void validateAppId(String appId) {
        if (appId == null) {
            throw new IllegalArgumentException("appId must not be null");
        }
        if (appId.length() != APP_ID_LENGTH) {
            throw new IllegalArgumentException(
                "appId must be exactly " + APP_ID_LENGTH
                + " characters, got " + appId.length() + ": '" + appId + "'");
        }
        for (int i = 0; i < APP_ID_LENGTH; i++) {
            char c = appId.charAt(i);
            boolean isHex = (c >= '0' && c <= '9')
                         || (c >= 'a' && c <= 'f')
                         || (c >= 'A' && c <= 'F');
            if (!isHex) {
                throw new IllegalArgumentException(
                    "appId must contain only hex characters [0-9a-fA-F] to "
                    + "remain a valid UUID prefix. Got '" + appId
                    + "' (non-hex at index " + i + ": '" + c + "')");
            }
        }
    }
}
