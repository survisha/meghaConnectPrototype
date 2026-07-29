package com.survisha.meghaconnect.formextraction.validation;

import com.survisha.meghaconnect.formextraction.config.FormExtractionProperties;
import com.survisha.meghaconnect.formextraction.dto.ImageQualityResult;
import com.survisha.meghaconnect.formextraction.exception.FormExtractionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class VisitorFormImageValidator {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png");
    private final FormExtractionProperties properties;

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("FORM_IMAGE_REQUIRED", "A form image is required.", 400);
        if (file.getSize() > properties.getMaxImageSizeBytes()) throw invalid("FORM_IMAGE_TOO_LARGE", "The form image exceeds the configured size limit.", 413);
        String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED.contains(mime)) throw invalid("FORM_IMAGE_UNSUPPORTED", "Only JPEG and PNG form images are supported.", 400);
        try {
            byte[] bytes = file.getBytes();
            boolean jpeg = bytes.length >= 3 && (bytes[0]&255)==255 && (bytes[1]&255)==216 && (bytes[2]&255)==255;
            boolean png = bytes.length >= 8 && (bytes[0]&255)==137 && bytes[1]==80 && bytes[2]==78 && bytes[3]==71;
            if ((!jpeg && !png) || (jpeg && !"image/jpeg".equals(mime)) || (png && !"image/png".equals(mime))) {
                throw invalid("FORM_IMAGE_SIGNATURE_INVALID", "The uploaded file is not a valid JPEG or PNG image.", 400);
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) throw invalid("FORM_IMAGE_INVALID", "The uploaded image could not be decoded.", 400);
            List<String> issues = new ArrayList<>();
            if (image.getWidth() < properties.getMinImageWidth() || image.getHeight() < properties.getMinImageHeight()) {
                issues.add("Image resolution is too low.");
            }
            ImageQualityResult quality = new ImageQualityResult(issues.isEmpty(), issues);
            return new ValidatedImage(bytes, mime, image.getWidth(), image.getHeight(), quality);
        } catch (IOException ex) {
            throw new FormExtractionException("FORM_IMAGE_INVALID", "The uploaded image could not be read.", 400, ex);
        }
    }

    private FormExtractionException invalid(String code, String message, int status) {
        return new FormExtractionException(code, message, status);
    }

    public record ValidatedImage(byte[] bytes, String mimeType, int width, int height, ImageQualityResult quality) {}
}
