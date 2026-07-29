package com.survisha.meghaconnect.face.validation;

import com.survisha.meghaconnect.face.config.FaceRecognitionProperties;
import com.survisha.meghaconnect.face.exception.FaceRecognitionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class FacePhotoValidator {
    private final FaceRecognitionProperties properties;

    public String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field + " is required.");
        }
        String encoded = value.trim();
        int comma = encoded.indexOf(',');
        if (encoded.regionMatches(true, 0, "data:", 0, 5)) {
            if (comma < 0 || !encoded.substring(0, comma).toLowerCase().contains(";base64")) {
                throw invalid(field + " must be a Base64 image data URL.");
            }
            encoded = encoded.substring(comma + 1);
        }
        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException ex) {
            throw invalid(field + " is not valid Base64.");
        }
        if (bytes.length == 0) throw invalid(field + " is empty.");
        if (bytes.length > properties.getMaxPhotoSizeBytes()) throw invalid(field + " exceeds the maximum size.");
        boolean jpeg = bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
        boolean png = bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e
                && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a;
        if ((!jpeg || !properties.getAllowedPhotoFormats().contains("image/jpeg"))
                && (!png || !properties.getAllowedPhotoFormats().contains("image/png"))) {
            throw invalid(field + " must contain a permitted JPEG or PNG image.");
        }
        return encoded;
    }

    private FaceRecognitionException invalid(String message) {
        return new FaceRecognitionException("FACE_INVALID_PHOTO", message, 400);
    }
}
