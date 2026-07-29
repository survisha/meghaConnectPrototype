package com.survisha.meghaconnect.formextraction.validation;

import com.survisha.meghaconnect.formextraction.config.FormExtractionProperties;
import com.survisha.meghaconnect.formextraction.exception.FormExtractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class VisitorFormImageValidatorTest {
    private FormExtractionProperties properties;
    private VisitorFormImageValidator validator;

    @BeforeEach void setUp() {
        properties = new FormExtractionProperties();
        properties.setMinImageWidth(100);
        properties.setMinImageHeight(100);
        validator = new VisitorFormImageValidator(properties);
    }

    @Test void acceptsValidJpeg() throws Exception {
        var result = validator.validate(file("form.jpg", "image/jpeg", image("jpg", 200, 200)));
        assertTrue(result.quality().isAcceptable());
        assertEquals("image/jpeg", result.mimeType());
    }

    @Test void acceptsValidPng() throws Exception {
        var result = validator.validate(file("form.png", "image/png", image("png", 200, 200)));
        assertTrue(result.quality().isAcceptable());
    }

    @Test void rejectsInvalidMimeType() {
        assertThrows(FormExtractionException.class,
                () -> validator.validate(file("form.gif", "image/gif", new byte[]{1,2,3})));
    }

    @Test void rejectsInvalidSignature() {
        assertThrows(FormExtractionException.class,
                () -> validator.validate(file("form.jpg", "image/jpeg", new byte[]{1,2,3,4})));
    }

    @Test void rejectsBlankImage() {
        assertThrows(FormExtractionException.class,
                () -> validator.validate(file("form.jpg", "image/jpeg", new byte[0])));
    }

    @Test void rejectsOversizedImage() throws Exception {
        properties.setMaxImageSizeBytes(10);
        assertThrows(FormExtractionException.class,
                () -> validator.validate(file("form.png", "image/png", image("png", 200, 200))));
    }

    @Test void reportsLowResolutionWithoutCallingItAcceptable() throws Exception {
        var result = validator.validate(file("form.png", "image/png", image("png", 50, 50)));
        assertFalse(result.quality().isAcceptable());
        assertFalse(result.quality().getIssues().isEmpty());
    }

    private MockMultipartFile file(String name, String type, byte[] bytes) {
        return new MockMultipartFile("image", name, type, bytes);
    }

    private byte[] image(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE); graphics.fillRect(0,0,width,height);
        graphics.setColor(Color.BLACK); graphics.drawString("Synthetic Visitor Form", 10, Math.min(30,height-1));
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
