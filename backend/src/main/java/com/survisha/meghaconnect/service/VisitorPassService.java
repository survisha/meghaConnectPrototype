package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.QrTokenGenerationResult;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.entity.WalkIn;
import com.survisha.meghaconnect.exception.AppointmentNotFoundException;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.WalkInRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class VisitorPassService {

    private static final Set<Appointment.AppointmentStatus> PASS_STATUSES = Set.of(
            Appointment.AppointmentStatus.SCHEDULED,
            Appointment.AppointmentStatus.HCM_ACCEPTED,
            Appointment.AppointmentStatus.APPROVED_WITH_DATE_TIME,
            Appointment.AppointmentStatus.SCHEDULED_FOR_PUBLIC_DARBAR
    );
    private static final int QR_IMAGE_SIZE = 320;

    private final AppointmentRepository appointmentRepository;
    private final WalkInRepository walkInRepository;
    private final QrTokenService qrTokenService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public Map<String, Object> getPassDetails(Long appointmentId, Long visitorId) {
        Appointment appointment = loadOwnedAppointment(appointmentId, visitorId);
        ensurePassEligible(appointment);
        Visitor visitor = appointment.getApplicant();
        Map<String, Object> details = new HashMap<>();
        details.put("appointmentId", appointment.getId());
        details.put("applicationId", appointment.getApplicationId());
        details.put("applicantName", visitor != null ? visitor.getFullName() : "");
        details.put("mobileNumber", visitor != null ? visitor.getPhoneNumber() : "");
        details.put("tokenNumber", walkInRepository.findByAppointment_Id(appointment.getId()).map(WalkIn::getTokenNumber).orElse(""));
        details.put("appointmentType", Boolean.TRUE.equals(appointment.getIsWalkIn()) ? "B2 Walk-in" : firstNonBlank(appointment.getAppointmentType(), appointment.getAgendaType()));
        details.put("scheduledDateTime", appointment.getScheduledDateTime());
        details.put("location", appointment.getRequestedLocation() != null ? appointment.getRequestedLocation().name() : "");
        details.put("status", "ACTIVE");
        details.put("visitorPhoto", loadVisitorPhotoDataUri(visitor).orElse(null));
        details.put("downloadUrl", "/api/v1/appointments/" + appointment.getId() + "/visitor-pass/download");
        return details;
    }

    @Transactional
    public byte[] generatePassPdf(Long appointmentId, Long visitorId, String actor) {
        Appointment appointment = loadOwnedAppointment(appointmentId, visitorId);
        ensurePassEligible(appointment);
        QrTokenGenerationResult qr = qrTokenService.generateFreshForVisitorPass(appointment, actor);
        try {
            return buildPdf(appointment, qr.getQrToken());
        } catch (IOException e) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.QR_GENERATION_FAILED,
                    "Visitor pass PDF generation failed.",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }

    private Appointment loadOwnedAppointment(Long appointmentId, Long visitorId) {
        if (visitorId == null) {
            return appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        }
        return appointmentRepository.findByIdAndApplicant_Id(appointmentId, visitorId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
    }

    private void ensurePassEligible(Appointment appointment) {
        if (appointment == null || appointment.getScheduledDateTime() == null) {
            throw notEligible();
        }
        if (appointment.getStatus() == Appointment.AppointmentStatus.APPROVED) {
            return;
        }
        if (!PASS_STATUSES.contains(appointment.getStatus())) {
            throw notEligible();
        }
    }

    private MeghaConnectException notEligible() {
        return new MeghaConnectException(
                ErrorCodeConstants.APPT_INVALID_STATUS,
                "Visitor pass is available only after the appointment is scheduled.",
                HttpStatus.CONFLICT.value()
        );
    }

    private byte[] buildPdf(Appointment appointment, String qrToken) throws IOException {
        if (Boolean.TRUE.equals(appointment.getIsWalkIn())) {
            return buildWalkInPdf(appointment, qrToken);
        }
        Visitor visitor = appointment.getApplicant();
        String applicantName = firstNonBlank(visitor != null ? visitor.getFullName() : null, appointment.getGuestName(), "Visitor");
        String mobile = firstNonBlank(visitor != null ? visitor.getPhoneNumber() : null, appointment.getGuestMobile(), "");
        String scheduled = appointment.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        String location = appointment.getRequestedLocation() != null ? appointment.getRequestedLocation().name() : "SHILLONG";

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 780;
                drawText(content, PDType1Font.HELVETICA_BOLD, 20, 56, y, "MeghaConnect");
                y -= 28;
                drawText(content, PDType1Font.HELVETICA_BOLD, 16, 56, y, "Visitor Pass");
                y -= 34;
                drawLine(content, 56, y, 540, y);
                y -= 28;

                drawVisitorPhoto(document, content, visitor, 422, 610, 104, 124);

                y = row(content, y, "Application ID", appointment.getApplicationId());
                y = row(content, y, "Appointment ID", String.valueOf(appointment.getId()));
                y = row(content, y, "Applicant Name", applicantName);
                y = row(content, y, "Mobile Number", mobile);
                y = row(content, y, "Appointment Date/Time", scheduled);
                y = row(content, y, "Duration", (appointment.getScheduledDurationMinutes() != null ? appointment.getScheduledDurationMinutes() : 30) + " minutes");
                y = row(content, y, "Location", location);
                y = row(content, y, "Agenda Type", firstNonBlank(appointment.getAgendaType(), appointment.getAppointmentType(), "Appointment"));

                drawText(content, PDType1Font.HELVETICA_BOLD, 11, 56, 438, "QR Code Scanner");
                drawQrCode(document, content, qrToken, 56, 286, 142, 142);
                drawText(content, PDType1Font.HELVETICA, 7, 214, 374, qrToken);

                y = 250;
                drawText(content, PDType1Font.HELVETICA_BOLD, 12, 56, y, "Instructions");
                y -= 18;
                y = bullet(content, y, "Carry valid ID proof.");
                y = bullet(content, y, "Show this pass at security.");
                y = bullet(content, y, "Pass is valid only for the scheduled date/time.");
                y -= 24;
                drawText(content, PDType1Font.HELVETICA, 8, 56, y, "Generated: " + DateTimeUtil.nowIST().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildWalkInPdf(Appointment appointment, String qrToken) throws IOException {
        Visitor visitor = appointment.getApplicant();
        String applicantName = firstNonBlank(visitor != null ? visitor.getFullName() : null, appointment.getGuestName(), "Visitor");
        String mobile = firstNonBlank(visitor != null ? visitor.getPhoneNumber() : null, appointment.getGuestMobile(), "");
        String scheduled = appointment.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        String tokenNumber = walkInRepository.findByAppointment_Id(appointment.getId()).map(WalkIn::getTokenNumber).orElse("");

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight() / 2));
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 382;
                drawText(content, PDType1Font.HELVETICA_BOLD, 18, 36, y, "MeghaConnect Walk-in Pass");
                y -= 22;
                drawLine(content, 36, y, 558, y);
                y -= 28;

                y = tokenRow(content, y, "Token Number", tokenNumber);
                y = row(content, y, "Visitor Name", applicantName);
                y = row(content, y, "Mobile", mobile);
                y = row(content, y, "Appointment Type", "B2 Walk-in");
                y = row(content, y, "Date/Time", scheduled);
                y = row(content, y, "Application ID", appointment.getApplicationId());
                y = row(content, y, "Generated", DateTimeUtil.nowIST().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));

                drawVisitorPhoto(document, content, visitor, 430, 252, 92, 108);
                drawQrCode(document, content, qrToken, 416, 88, 120, 120);
                drawText(content, PDType1Font.HELVETICA, 7, 414, 72, qrToken);
                drawText(content, PDType1Font.HELVETICA, 8, 36, 42, "Please show this pass at the counter/security desk.");
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private float row(PDPageContentStream content, float y, String label, String value) throws IOException {
        drawText(content, PDType1Font.HELVETICA_BOLD, 10, 56, y, label + ":");
        drawText(content, PDType1Font.HELVETICA, 10, 190, y, safe(value));
        return y - 22;
    }

    private float tokenRow(PDPageContentStream content, float y, String label, String tokenNumber) throws IOException {
        drawText(content, PDType1Font.HELVETICA_BOLD, 10, 56, y, label + ":");
        drawTokenNumber(content, 190, y, safe(tokenNumber));
        return y - 22;
    }

    private float bullet(PDPageContentStream content, float y, String value) throws IOException {
        drawText(content, PDType1Font.HELVETICA, 10, 70, y, "- " + value);
        return y - 16;
    }

    private void drawVisitorPhoto(PDDocument document,
                                  PDPageContentStream content,
                                  Visitor visitor,
                                  float x,
                                  float y,
                                  float width,
                                  float height) throws IOException {
        drawText(content, PDType1Font.HELVETICA_BOLD, 9, x, y + height + 10, "Visitor Photo");
        content.addRect(x, y, width, height);
        content.stroke();

        Optional<String> dataUri = loadVisitorPhotoDataUri(visitor);
        if (dataUri.isEmpty()) {
            drawText(content, PDType1Font.HELVETICA, 8, x + 10, y + (height / 2), "Photo not available");
            return;
        }

        try {
            byte[] bytes = decodeDataUri(dataUri.get());
            PDImageXObject image = PDImageXObject.createFromByteArray(document, bytes, "visitor-photo");
            float imageWidth = image.getWidth();
            float imageHeight = image.getHeight();
            float scale = Math.min((width - 8) / imageWidth, (height - 8) / imageHeight);
            float drawWidth = imageWidth * scale;
            float drawHeight = imageHeight * scale;
            float drawX = x + (width - drawWidth) / 2;
            float drawY = y + (height - drawHeight) / 2;
            content.drawImage(image, drawX, drawY, drawWidth, drawHeight);
        } catch (IllegalArgumentException | IOException e) {
            drawText(content, PDType1Font.HELVETICA, 8, x + 10, y + (height / 2), "Photo not available");
        }
    }

    private Optional<String> loadVisitorPhotoDataUri(Visitor visitor) {
        if (visitor == null) {
            return Optional.empty();
        }
        return fileStorageService.loadImageDataUri(firstNonBlank(
                visitor.getLivePhotoPath(),
                visitor.getPhotoStoragePath(),
                visitor.getPhotoPath()
        ));
    }

    private byte[] decodeDataUri(String dataUri) {
        int commaIndex = dataUri.indexOf(',');
        String payload = commaIndex >= 0 ? dataUri.substring(commaIndex + 1) : dataUri;
        return Base64.getDecoder().decode(payload);
    }

    private void drawQrCode(PDDocument document,
                            PDPageContentStream content,
                            String token,
                            float x,
                            float y,
                            float width,
                            float height) throws IOException {
        byte[] qrPng = createQrPng(token);
        PDImageXObject qrImage = PDImageXObject.createFromByteArray(document, qrPng, "visitor-pass-qr");
        content.drawImage(qrImage, x, y, width, height);
    }

    private void drawText(PDPageContentStream content, PDType1Font font, int size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safe(text));
        content.endText();
    }

    private void drawTokenNumber(PDPageContentStream content, float x, float y, String tokenNumber) throws IOException {
        String safeToken = safe(tokenNumber);
        if (safeToken.length() <= 4) {
            drawText(content, PDType1Font.HELVETICA_BOLD, 12, x, y, safeToken);
            return;
        }

        String prefix = safeToken.substring(0, safeToken.length() - 4);
        String suffix = safeToken.substring(safeToken.length() - 4);
        int size = 10;
        drawText(content, PDType1Font.HELVETICA, size, x, y, prefix);
        float prefixWidth = PDType1Font.HELVETICA.getStringWidth(prefix) / 1000f * size;
        drawText(content, PDType1Font.HELVETICA_BOLD, 12, x + prefixWidth, y, suffix);
    }

    private void drawLine(PDPageContentStream content, float x1, float y1, float x2, float y2) throws IOException {
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private byte[] createQrPng(String token) throws IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);

        try {
            BitMatrix matrix = new MultiFormatWriter().encode(safe(token), BarcodeFormat.QR_CODE, QR_IMAGE_SIZE, QR_IMAGE_SIZE, hints);
            BufferedImage image = new BufferedImage(QR_IMAGE_SIZE, QR_IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < QR_IMAGE_SIZE; y++) {
                for (int x = 0; x < QR_IMAGE_SIZE; x++) {
                    image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (WriterException e) {
            throw new IOException("Unable to generate visitor pass QR code.", e);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", " ");
    }
}
