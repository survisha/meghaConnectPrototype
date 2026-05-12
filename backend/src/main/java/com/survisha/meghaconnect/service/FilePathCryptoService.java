package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@Slf4j
public class FilePathCryptoService {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ENCRYPTED_VALUE_PREFIX = "enc:";
    private static final int AES_256_KEY_BYTES = 32;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    @Value("${file.upload.crypto-key-path:${meghaconnect.storage.crypto-key-path:}}")
    private String cryptoKeyPath;

    private final SecureRandom secureRandom = new SecureRandom();
    private volatile SecretKeySpec secretKey;

    public String encryptPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "filePath"),
                    400
            );
        }

        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(relativePath.getBytes(StandardCharsets.UTF_8));

            ByteBuffer output = ByteBuffer.allocate(iv.length + cipherText.length);
            output.put(iv);
            output.put(cipherText);
            return ENCRYPTED_VALUE_PREFIX + Base64.getEncoder().encodeToString(output.array());
        } catch (GeneralSecurityException e) {
            log.warn("Unable to encrypt stored document path.");
            throw new MeghaConnectException(
                    ErrorCodeConstants.CONFIGURATION_ERROR,
                    ErrorCodeConstants.format(ErrorCodeConstants.CONFIGURATION_ERROR_MSG, "File path encryption failed"),
                    500,
                    e
            );
        }
    }

    public String decryptPath(String encryptedPath) {
        if (encryptedPath == null || encryptedPath.isBlank()) {
            throw documentUnavailable(null);
        }

        try {
            String payload = encryptedPath.startsWith(ENCRYPTED_VALUE_PREFIX)
                    ? encryptedPath.substring(ENCRYPTED_VALUE_PREFIX.length())
                    : encryptedPath;
            byte[] encryptedBytes = Base64.getDecoder().decode(payload);
            if (encryptedBytes.length <= GCM_IV_BYTES) {
                throw new IllegalArgumentException("Encrypted path payload is too short.");
            }

            ByteBuffer input = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[GCM_IV_BYTES];
            input.get(iv);
            byte[] cipherText = new byte[input.remaining()];
            input.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            log.warn("Unable to decrypt stored document path.");
            throw documentUnavailable(e);
        }
    }

    public String hashPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "filePath"),
                    400
            );
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(getSecretKey());
            return Base64.getEncoder().encodeToString(mac.doFinal(relativePath.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            log.warn("Unable to hash stored document path.");
            throw new MeghaConnectException(
                    ErrorCodeConstants.CONFIGURATION_ERROR,
                    ErrorCodeConstants.format(ErrorCodeConstants.CONFIGURATION_ERROR_MSG, "File path hash failed"),
                    500,
                    e
            );
        }
    }

    public boolean verifyPathHash(String relativePath, String expectedHash) {
        if (expectedHash == null || expectedHash.isBlank()) {
            return true;
        }
        String actualHash = hashPath(relativePath);
        return MessageDigest.isEqual(
                actualHash.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    private SecretKeySpec getSecretKey() {
        SecretKeySpec cached = secretKey;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (secretKey == null) {
                secretKey = new SecretKeySpec(loadKeyBytes(), AES_ALGORITHM);
            }
            return secretKey;
        }
    }

    private byte[] loadKeyBytes() {
        if (cryptoKeyPath == null || cryptoKeyPath.isBlank()) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.MISSING_CONFIGURATION,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_CONFIGURATION_MSG, "file.upload.crypto-key-path"),
                    500
            );
        }

        try {
            Path keyPath = Paths.get(cryptoKeyPath).toAbsolutePath().normalize();
            byte[] raw = Files.readAllBytes(keyPath);
            return normalizeKey(raw);
        } catch (MeghaConnectException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Unable to load file path crypto key.");
            throw new MeghaConnectException(
                    ErrorCodeConstants.CONFIGURATION_ERROR,
                    ErrorCodeConstants.format(ErrorCodeConstants.CONFIGURATION_ERROR_MSG, "File path crypto key could not be loaded"),
                    500,
                    e
            );
        }
    }

    private byte[] normalizeKey(byte[] raw) {
        if (raw.length == AES_256_KEY_BYTES) {
            return raw;
        }

        String text = new String(raw, StandardCharsets.UTF_8).trim();
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        if (textBytes.length == AES_256_KEY_BYTES) {
            return textBytes;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            if (decoded.length == AES_256_KEY_BYTES) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to the generic invalid-configuration response.
        }

        throw new MeghaConnectException(
                ErrorCodeConstants.INVALID_CONFIGURATION,
                "File path crypto key must be 32 bytes or base64-encoded 32 bytes.",
                500
        );
    }

    private MeghaConnectException documentUnavailable(Exception cause) {
        if (cause == null) {
            return new MeghaConnectException(
                    ErrorCodeConstants.CONTENT_NOT_FOUND,
                    "Document file is unavailable.",
                    404
            );
        }
        return new MeghaConnectException(
                ErrorCodeConstants.CONTENT_NOT_FOUND,
                "Document file is unavailable.",
                404,
                cause
        );
    }
}
