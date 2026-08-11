package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.*;
import com.survisha.meghaconnect.entity.*;
import com.survisha.meghaconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentReportService {
    private static final int EXPORT_LIMIT = 10_000;
    private final AppointmentRepository appointmentRepository;
    private final DirectionFollowUpRepository followUpRepository;
    private final DocumentUploadRepository documentRepository;
    private final SchemeApplicationRepository schemeRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public Page<AppointmentReportRow> search(AppointmentReportFilter filter, Pageable pageable, String actor) {
        Page<Appointment> page = appointmentRepository.findAll(specification(filter, actor), pageable);
        Map<Long, Enrichment> enrichment = enrich(page.getContent());
        return page.map(item -> row(item, enrichment.getOrDefault(item.getId(), new Enrichment())));
    }

    public byte[] exportExcel(AppointmentReportFilter filter, String actor) {
        List<AppointmentReportRow> rows = exportRows(filter, actor);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Appointments");
            String[] headers = {"Application ID","Applicant","Mobile","Constituency","District","Category","Type","Agenda","Status","Scheduled","Completed","Outcome","Directions","Department","Routed Department","Officer","Follow-up","Scheme","Documents"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            int index = 1;
            for (AppointmentReportRow item : rows) {
                Row row = sheet.createRow(index++); String[] values = values(item);
                for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            auditLogService.log("AppointmentReport", null, "REPORT_EXCEL_EXPORTED", "Rows exported: " + rows.size(), actor);
            return out.toByteArray();
        } catch (IOException exception) { throw new IllegalStateException("Unable to generate Excel report.", exception); }
    }

    public byte[] exportPdf(AppointmentReportFilter filter, String actor) {
        List<AppointmentReportRow> rows = exportRows(filter, actor);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())); document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            float y = page.getMediaBox().getHeight() - 35;
            content.beginText(); content.setFont(PDType1Font.HELVETICA_BOLD, 14); content.newLineAtOffset(35, y);
            content.showText("MeghaConnect Appointment Report"); content.endText(); y -= 24;
            for (AppointmentReportRow item : rows) {
                if (y < 45) { content.close(); page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())); document.addPage(page); content = new PDPageContentStream(document, page); y = page.getMediaBox().getHeight() - 35; }
                String line = safe(item.getApplicationId()) + " | " + safe(item.getApplicantName()) + " | " + safe(item.getAppointmentCategory()) + " | " + safe(item.getStatus()) + " | " + safe(item.getDepartment()) + " | Follow-up: " + safe(item.getFollowUpStatus());
                content.beginText(); content.setFont(PDType1Font.HELVETICA, 8); content.newLineAtOffset(35, y); content.showText(clip(line, 150)); content.endText(); y -= 13;
            }
            content.close(); document.save(out);
            auditLogService.log("AppointmentReport", null, "REPORT_PDF_EXPORTED", "Rows exported: " + rows.size(), actor);
            return out.toByteArray();
        } catch (IOException exception) { throw new IllegalStateException("Unable to generate PDF report.", exception); }
    }

    private List<AppointmentReportRow> exportRows(AppointmentReportFilter filter, String actor) {
        Page<Appointment> page = appointmentRepository.findAll(specification(filter, actor), PageRequest.of(0, EXPORT_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, Enrichment> enrichment = enrich(page.getContent());
        return page.stream().map(item -> row(item, enrichment.getOrDefault(item.getId(), new Enrichment()))).toList();
    }

    private Specification<Appointment> specification(AppointmentReportFilter f, String actor) {
        AppointmentReportFilter filter = f != null ? f : new AppointmentReportFilter();
        Long scopedDepartment = scopedDepartment(actor);
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            Long department = scopedDepartment != null ? scopedDepartment : filter.getDepartmentId();
            if (department != null) p.add(cb.or(cb.equal(root.get("tenantDepartment").get("id"), department), cb.equal(root.get("routedDepartment").get("id"), department)));
            if (filter.getAppointmentStatus() != null) p.add(cb.equal(root.get("status"), filter.getAppointmentStatus()));
            if (filter.getAppointmentCategory() != null) p.add(cb.equal(root.get("appointmentCategory"), filter.getAppointmentCategory()));
            if (filter.getConstituency() != null) p.add(cb.equal(cb.lower(root.get("applicant").get("constituency")), filter.getConstituency().toLowerCase()));
            if (filter.getDistrict() != null) p.add(cb.equal(cb.lower(root.get("applicant").get("district")), filter.getDistrict().toLowerCase()));
            if (filter.getAgendaType() != null) p.add(cb.equal(cb.lower(root.get("agendaType")), filter.getAgendaType().toLowerCase()));
            if (filter.getAppointmentType() != null) p.add(cb.equal(cb.lower(root.get("appointmentType")), filter.getAppointmentType().toLowerCase()));
            if (filter.getMla() != null) p.add(cb.like(cb.lower(root.get("referredByName")), "%" + filter.getMla().toLowerCase() + "%"));
            if (filter.getRoutedDepartmentId() != null) p.add(cb.equal(root.get("routedDepartment").get("id"), filter.getRoutedDepartmentId()));
            if (filter.getResponsibleOfficer() != null) p.add(cb.like(cb.lower(root.get("routedOfficer")), "%" + filter.getResponsibleOfficer().toLowerCase() + "%"));
            if (filter.getFromDate() != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate().atStartOfDay()));
            if (filter.getToDate() != null) p.add(cb.lessThan(root.get("createdAt"), filter.getToDate().plusDays(1).atStartOfDay()));
            if (filter.getScheme() != null && !filter.getScheme().isBlank()) {
                Subquery<Long> sq = query.subquery(Long.class); Root<SchemeApplication> scheme = sq.from(SchemeApplication.class);
                sq.select(scheme.get("appointment").get("id")).where(cb.equal(cb.upper(scheme.get("schemeType").as(String.class)), filter.getScheme().toUpperCase()));
                p.add(root.get("id").in(sq));
            }
            if (filter.getFollowUpStatus() != null && !filter.getFollowUpStatus().isBlank()) {
                Subquery<Long> sq = query.subquery(Long.class); Root<DirectionFollowUp> follow = sq.from(DirectionFollowUp.class);
                List<Predicate> fp = new ArrayList<>(); fp.add(cb.equal(follow.get("appointment").get("id"), root.get("id")));
                if ("OVERDUE".equalsIgnoreCase(filter.getFollowUpStatus())) {
                    fp.add(cb.notEqual(follow.get("status"), DirectionFollowUp.FollowUpStatus.COMPLETED)); fp.add(cb.lessThan(follow.get("dueDate"), LocalDate.now()));
                } else fp.add(cb.equal(follow.get("status"), DirectionFollowUp.FollowUpStatus.valueOf(filter.getFollowUpStatus().toUpperCase())));
                sq.select(follow.get("appointment").get("id")).where(fp.toArray(Predicate[]::new)); p.add(cb.exists(sq));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private Map<Long, Enrichment> enrich(List<Appointment> appointments) {
        List<Long> ids = appointments.stream().map(Appointment::getId).toList(); Map<Long, Enrichment> map = new HashMap<>();
        ids.forEach(id -> map.put(id, new Enrichment())); if (ids.isEmpty()) return map;
        followUpRepository.findByAppointment_IdIn(ids).forEach(f -> map.get(f.getAppointment().getId()).followUps.add(f));
        documentRepository.findByAppointment_IdIn(ids).forEach(d -> map.get(d.getAppointment().getId()).documents.add(d));
        schemeRepository.findByAppointment_IdIn(ids).forEach(s -> map.get(s.getAppointment().getId()).schemes.add(s)); return map;
    }

    private AppointmentReportRow row(Appointment a, Enrichment e) {
        Visitor v = a.getApplicant(); String followStatus = e.followUps.stream().anyMatch(f -> f.isOverdue(LocalDate.now())) ? "OVERDUE" : e.followUps.stream().map(f -> f.getStatus().name()).distinct().collect(Collectors.joining(","));
        return AppointmentReportRow.builder().appointmentId(a.getId()).applicationId(a.getApplicationId()).applicantName(v != null ? v.getFullName() : a.getGuestName()).mobile(v != null ? v.getPhoneNumber() : a.getGuestMobile())
                .epicReference(v != null ? mask(v.getEpicNumber()) : null).constituency(v != null ? v.getConstituency() : null).district(v != null ? v.getDistrict() : null)
                .appointmentCategory(a.getAppointmentCategory().name()).appointmentType(a.getAppointmentType()).agendaType(a.getAgendaType()).petitionSummary(a.getAgendaBrief()).status(a.getStatus().name())
                .scheduledDateTime(a.getScheduledDateTime()).completedAt(a.getCompletedAt()).meetingOutcome(a.getMeetingOutcome()).directions(e.followUps.stream().map(DirectionFollowUp::getInstruction).collect(Collectors.joining("; ")))
                .department(a.getDepartment()).routedDepartment(a.getRoutedDepartment() != null ? a.getRoutedDepartment().getDepartmentName() : null).responsibleOfficer(a.getRoutedOfficer()).followUpStatus(followStatus)
                .scheme(e.schemes.stream().map(s -> s.getSchemeType().name()).distinct().collect(Collectors.joining(","))).supportingDocuments(e.documents.stream().map(DocumentUpload::getOriginalFilename).toList()).build();
    }

    private Long scopedDepartment(String actor) { return userRepository.findByNormalizedUsername(actor == null ? "" : actor).filter(u -> u.getRole() == User.UserRole.DEPARTMENT_ADMIN || u.getRole() == User.UserRole.DEPARTMENT_PA).map(User::getDepartment).map(Department::getId).orElse(null); }
    private String mask(String value) { return value == null || value.length() <= 4 ? value : "XXXX-" + value.substring(value.length() - 4); }
    private String safe(Object value) { return value == null ? "" : String.valueOf(value).replaceAll("[\\r\\n]", " "); }
    private String clip(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
    private String[] values(AppointmentReportRow r) { return new String[]{safe(r.getApplicationId()),safe(r.getApplicantName()),safe(r.getMobile()),safe(r.getConstituency()),safe(r.getDistrict()),safe(r.getAppointmentCategory()),safe(r.getAppointmentType()),safe(r.getAgendaType()),safe(r.getStatus()),safe(r.getScheduledDateTime()),safe(r.getCompletedAt()),safe(r.getMeetingOutcome()),safe(r.getDirections()),safe(r.getDepartment()),safe(r.getRoutedDepartment()),safe(r.getResponsibleOfficer()),safe(r.getFollowUpStatus()),safe(r.getScheme()),String.join(", ", r.getSupportingDocuments())}; }
    private static class Enrichment { final List<DirectionFollowUp> followUps = new ArrayList<>(); final List<DocumentUpload> documents = new ArrayList<>(); final List<SchemeApplication> schemes = new ArrayList<>(); }
}
