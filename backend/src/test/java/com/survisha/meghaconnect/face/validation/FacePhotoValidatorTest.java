package com.survisha.meghaconnect.face.validation;

import com.survisha.meghaconnect.face.config.FaceRecognitionProperties;
import com.survisha.meghaconnect.face.exception.FaceRecognitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FacePhotoValidatorTest {
    private FaceRecognitionProperties properties;
    private FacePhotoValidator validator;

    @BeforeEach
    void setUp() {
        properties = new FaceRecognitionProperties();
        validator = new FacePhotoValidator(properties);
    }

    @Test
    void acceptsAndNormalizesJpegDataUrl() {
        String encoded = Base64.getEncoder().encodeToString(new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0});
        assertEquals(encoded, validator.normalize("data:image/jpeg;base64," + encoded, "photo"));
    }

    @Test
    void rejectsMalformedBase64AndBlankPhoto() {
        assertThrows(FaceRecognitionException.class, () -> validator.normalize("not-base64!", "photo"));
        assertThrows(FaceRecognitionException.class, () -> validator.normalize(" ", "photo"));
    }

    @Test
    void rejectsOversizedImage() {
        properties.setMaxPhotoSizeBytes(3);
        String encoded = Base64.getEncoder().encodeToString(new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0});
        FaceRecognitionException error = assertThrows(FaceRecognitionException.class,
                () -> validator.normalize(encoded, "photo"));
        assertEquals(400, error.getHttpStatus());
    }
}
