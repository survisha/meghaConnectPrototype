package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.QrTokenGenerationResult;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.exception.AppointmentNotFoundException;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
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
    private static final Map<Character, String> CODE39 = Map.ofEntries(
            Map.entry('0', "nnnwwnwnn"), Map.entry('1', "wnnwnnnnw"),
            Map.entry('2', "nnwwnnnnw"), Map.entry('3', "wnwwnnnnn"),
            Map.entry('4', "nnnwwnnnw"), Map.entry('5', "wnnwwnnnn"),
            Map.entry('6', "nnwwwnnnn"), Map.entry('7', "nnnwnnwnw"),
            Map.entry('8', "wnnwnnwnn"), Map.entry('9', "nnwwnnwnn"),
            Map.entry('A', "wnnnnwnnw"), Map.entry('B', "nnwnnwnnw"),
            Map.entry('C', "wnwnnwnnn"), Map.entry('D', "nnnnwwnnw"),
            Map.entry('E', "wnnnwwnnn"), Map.entry('F', "nnwnwwnnn"),
            Map.entry('*', "nwnnwnwnn")
    );

    private final AppointmentRepository appointmentRepository;
    private final QrTokenService qrTokenService;

    @Transactional(readOnly = true)
    public Map<String, Object> getPassDetails(Long appointmentId, Long visitorId) {
        Appointment appointment = loadOwnedAppointment(appointmentId, visitorId);
        ensurePassEligible(appointment);
        Visitor visitor = appointment.getApplicant();
        Map<String, Object> details = new HashMap<>();
        details.put("appointmentId", appointment.getId());
        details.put("applicationId", appointment.getApplicationId());
        details.put("applicantName", visitor != null ? visitor.getFullName() : "");
        details.put("scheduledDateTime", appointment.getScheduledDateTime());
        details.put("location", appointment.getRequestedLocation() != null ? appointment.getRequestedLocation().name() : "");
        details.put("status", "ACTIVE");
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
            throw new MeghaConnectException(
                    ErrorCodeConstants.INSUFFICIENT_PERMISSIONS,
                    "Citizen session is required to access this visitor pass.",
                    HttpStatus.FORBIDDEN.value()
            );
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

                y = row(content, y, "Application ID", appointment.getApplicationId());
                y = row(content, y, "Appointment ID", String.valueOf(appointment.getId()));
                y = row(content, y, "Applicant Name", applicantName);
                y = row(content, y, "Mobile Number", mobile);
                y = row(content, y, "Appointment Date/Time", scheduled);
                y = row(content, y, "Duration", (appointment.getScheduledDurationMinutes() != null ? appointment.getScheduledDurationMinutes() : 30) + " minutes");
                y = row(content, y, "Location", location);
                y = row(content, y, "Agenda Type", firstNonBlank(appointment.getAgendaType(), appointment.getAppointmentType(), "Appointment"));

                drawText(content, PDType1Font.HELVETICA_BOLD, 11, 56, 438, "Secure scan code");
                drawTokenCode(content, qrToken, 56, 355, 480, 72);
                drawText(content, PDType1Font.HELVETICA, 7, 56, 342, qrToken);

                y = 300;
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

    private float row(PDPageContentStream content, float y, String label, String value) throws IOException {
        drawText(content, PDType1Font.HELVETICA_BOLD, 10, 56, y, label + ":");
        drawText(content, PDType1Font.HELVETICA, 10, 190, y, safe(value));
        return y - 22;
    }

    private float bullet(PDPageContentStream content, float y, String value) throws IOException {
        drawText(content, PDType1Font.HELVETICA, 10, 70, y, "- " + value);
        return y - 16;
    }

    private void drawText(PDPageContentStream content, PDType1Font font, int size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safe(text));
        content.endText();
    }

    private void drawLine(PDPageContentStream content, float x1, float y1, float x2, float y2) throws IOException {
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private void drawTokenCode(PDPageContentStream content, String token, float x, float y, float width, float height) throws IOException {
        String value = "*" + safe(token).toUpperCase().replaceAll("[^0-9A-F]", "") + "*";
        int modules = 0;
        for (int i = 0; i < value.length(); i++) {
            String pattern = CODE39.getOrDefault(value.charAt(i), CODE39.get('0'));
            for (int j = 0; j < pattern.length(); j++) {
                modules += pattern.charAt(j) == 'w' ? 3 : 1;
            }
            modules += 1;
        }
        float narrow = width / Math.max(modules, 1);
        float cursor = x;
        for (int i = 0; i < value.length(); i++) {
            String pattern = CODE39.getOrDefault(value.charAt(i), CODE39.get('0'));
            for (int j = 0; j < pattern.length(); j++) {
                float barWidth = narrow * (pattern.charAt(j) == 'w' ? 3 : 1);
                if (j % 2 == 0) {
                    content.addRect(cursor, y, barWidth, height);
                }
                cursor += barWidth;
            }
            cursor += narrow;
        }
        content.fill();
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
