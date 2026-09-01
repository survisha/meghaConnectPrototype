package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.*;
import com.survisha.meghaconnect.entity.*;
import com.survisha.meghaconnect.exception.ResourceNotFoundException;
import com.survisha.meghaconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExecutiveAppointmentReportService {
    private static final int EXPORT_LIMIT = 10_000;
    private static final String AI_UNAVAILABLE = "AI summary is currently unavailable.";
    private static final Set<String> SORT_FIELDS = Set.of("applicationId", "createdAt", "scheduledDateTime", "completedAt", "rejectedAt", "status");
    private final AppointmentRepository appointments;
    private final DirectionFollowUpRepository followUps;
    private final DirectionRepository directions;
    private final DocumentUploadRepository documents;
    private final SchemeApplicationRepository schemes;
    private final AppointmentAuditRepository audits;
    private final AppointmentReportStatusPolicy statusPolicy;
    private final AuditLogService audit;
    private final AiClientService aiClient;
    private final FileStorageService fileStorage;

    public Page<CompletedAppointmentSummaryResponse> completed(ExecutiveAppointmentReportFilter filter, Pageable pageable) {
        Page<Appointment> page = appointments.findAll(specification(filter, true), safePageable(pageable, "completedAt"));
        Map<Long, Enrichment> enrichment = enrich(page.getContent());
        return page.map(a -> completedSummary(a, enrichment.get(a.getId())));
    }

    public Page<RejectedAppointmentSummaryResponse> rejected(ExecutiveAppointmentReportFilter filter, Pageable pageable) {
        Page<Appointment> page = appointments.findAll(specification(filter, false), safePageable(pageable, "rejectedAt"));
        Map<Long, Enrichment> enrichment = enrich(page.getContent());
        return page.map(a -> rejectedSummary(a, enrichment.get(a.getId())));
    }

    public CompletedAppointmentDetailResponse completedDetail(Long id, String actor) {
        Appointment a = appointment(id);
        if (!statusPolicy.isCompleted(a)) throw new ResourceNotFoundException("Completed appointment not found");
        CompletedAppointmentDetailResponse detail = completedDetail(a);
        audit.log("AppointmentReport", id, "COMPLETED_APPOINTMENT_VIEWED", "Completed appointment detail viewed", actor);
        return detail;
    }

    public RejectedAppointmentDetailResponse rejectedDetail(Long id, String actor) {
        Appointment a = appointment(id);
        if (!statusPolicy.rejectedStatuses().contains(a.getStatus())) throw new ResourceNotFoundException("Rejected appointment not found");
        RejectedAppointmentDetailResponse result = RejectedAppointmentDetailResponse.builder()
                .applicant(applicant(a)).appointment(appointmentInfo(a, schemeNames(List.of(a))))
                .petitionSummary(petition(a)).documents(documentItems(documents.findByAppointmentId(id)))
                .rejectionReason(a.getRejectionReason()).rejectedBy(a.getRejectedBy()).rejectedAt(a.getRejectedAt())
                .returnReason(a.getReturnReason()).requiredInformation(a.getRequiredInformation())
                .statusHistory(history(id)).readOnly(true).build();
        audit.log("AppointmentReport", id, "REJECTED_APPOINTMENT_VIEWED", "Rejected appointment detail viewed", actor);
        return result;
    }

    public byte[] photo(Long id) {
        Appointment a = appointment(id);
        if (!statusPolicy.isCompleted(a)) throw new ResourceNotFoundException("Completed appointment not found");
        String uri = fileStorage.loadImageDataUri(photoPath(a.getApplicant())).orElseThrow(() -> new ResourceNotFoundException("Photo not found"));
        return Base64.getDecoder().decode(uri.substring(uri.indexOf(',') + 1));
    }

    public byte[] completedPdf(Long id, String actor) {
        Appointment a = appointment(id);
        if (!statusPolicy.isCompleted(a)) throw new ResourceNotFoundException("Completed appointment not found");
        CompletedAppointmentDetailResponse d = completedDetail(a);
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(pdf);
            writer.title("MEGHACONNECT", "COMPLETED APPOINTMENT REPORT");
            writer.line("Appointment ID: " + text(a.getApplicationId()));
            writer.line("Generated At: " + LocalDateTime.now());
            writer.heading("CITIZEN DETAILS");
            drawPhoto(pdf, writer, a.getApplicant());
            writer.field("Name", d.getApplicant().getName()); writer.field("EPIC", d.getApplicant().getEpic());
            writer.field("Mobile", d.getApplicant().getMobile()); writer.field("Address", d.getApplicant().getAddress());
            writer.field("Constituency", d.getApplicant().getConstituency()); writer.field("District", d.getApplicant().getDistrict());
            writer.heading("APPOINTMENT DETAILS");
            writer.field("Type", d.getAppointment().getType()); writer.field("Scheduled / Walk-in", d.getAppointment().getCategory());
            writer.field("Requested", d.getAppointment().getRequestedAt()); writer.field("Scheduled", d.getAppointment().getScheduledAt());
            writer.field("Completed", d.getAppointment().getCompletedAt()); writer.field("Department", d.getAppointment().getDepartment());
            writer.field("Scheme", d.getAppointment().getScheme()); writer.field("Agenda", d.getAppointment().getAgendaType());
            writer.heading("PETITION SUMMARY"); writer.paragraph(d.getPetitionSummary());
            writer.heading("APPROVER REMARKS"); writer.paragraph(d.getApproverRemarks());
            writer.field("Forwarded Department", d.getForwardedDepartment());
            writer.heading("HCM REMARKS / DIRECTIONS"); writer.paragraph(d.getHcmRemarks());
            writer.heading("CM / HCM DIRECTIONS");
            for (CompletedAppointmentDetailResponse.DirectionItem item : d.getDirections()) {
                writer.paragraph(text(item.getDirectionId()) + " | " + text(item.getDirection()) + " | " + text(item.getDepartment()) + " | " + text(item.getOfficer()) + " | Due: " + text(item.getDueDate()) + " | " + text(item.getFollowUpStatus()));
            }
            writer.heading("FOLLOW-UP / ACTION ITEMS");
            for (CompletedAppointmentDetailResponse.ActionItem item : d.getActionItems()) writer.paragraph(text(item.getDirectionId()) + " | " + text(item.getDepartment()) + " | " + text(item.getOfficer()) + " | " + text(item.getInstruction()) + " | " + text(item.getStatus()));
            writer.heading("SUPPORTING DOCUMENTS");
            for (CompletedAppointmentDetailResponse.DocumentItem item : d.getDocuments()) writer.paragraph(text(item.getFilename()) + " | " + text(item.getDocumentType()) + " | " + text(item.getUploadedDate()));
            writer.heading("AI-ASSISTED SUMMARY"); writer.paragraph(d.getAiSummary());
            writer.heading("MEETING OUTCOME"); writer.paragraph(d.getAppointment().getMeetingOutcome());
            writer.heading("REPORT INFORMATION"); writer.field("Generated By", actor); writer.field("Generated At", LocalDateTime.now());
            writer.close(); writer.addPageNumbers(); pdf.save(out);
            audit.log("AppointmentReport", id, "COMPLETED_APPOINTMENT_PDF_EXPORTED", "Individual completed report exported", actor);
            return out.toByteArray();
        } catch (IOException e) { throw new IllegalStateException("Unable to generate completed appointment PDF.", e); }
    }

    public byte[] completedExcel(ExecutiveAppointmentReportFilter filter, String actor) {
        List<Appointment> list = appointments.findAll(specification(filter, true), PageRequest.of(0, EXPORT_LIMIT, Sort.by(Sort.Direction.DESC, "completedAt"))).getContent();
        Map<Long, Enrichment> data = enrich(list);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet main = sheet(workbook, "Completed Appointments", new String[]{"Appointment ID","Applicant Name","EPIC","Mobile","Department","Scheme","Constituency","District","MLA","Agenda Type","Appointment Type","Requested Date","Meeting Date","Completed Date","Petition Summary","Department Assigned","Follow-up Status","Overall Status"});
            Sheet actions = sheet(workbook, "Directions & Follow-ups", new String[]{"Appointment ID","Direction ID","Department","Officer Responsible","Instruction","Due Date","Follow-up Status","Escalation","Completion Remarks"});
            Sheet docs = sheet(workbook, "Supporting Documents", new String[]{"Appointment ID","Document Name","Document Type","Upload Date","Uploaded By"});
            int mr=1, ar=1, dr=1;
            for (Appointment a : list) {
                Enrichment e=data.get(a.getId()); CompletedAppointmentSummaryResponse s=completedSummary(a,e);
                cells(main.createRow(mr++), a.getApplicationId(),s.getApplicantName(),s.getEpic(),s.getMobile(),s.getDepartment(),s.getScheme(),s.getConstituency(),s.getDistrict(),s.getMla(),s.getAgendaType(),s.getAppointmentType(),a.getCreatedAt(),s.getMeetingAt(),s.getCompletedAt(),petition(a),s.getAssignedDepartment(),s.getFollowUpStatus(),s.getStatus());
                for (DirectionFollowUp f:e.followUps) cells(actions.createRow(ar++),a.getApplicationId(),f.getDirectionId(),f.getDepartment().getDepartmentName(),f.getResponsibleOfficerName(),f.getInstruction(),f.getDueDate(),followStatus(f),f.getLastEscalatedAt()!=null,f.getCompletionRemarks());
                for (DocumentUpload d:e.documents) cells(docs.createRow(dr++),a.getApplicationId(),d.getOriginalFilename(),d.getDocumentType(),d.getUploadedDate(),d.getUploadedBy());
            }
            workbook.write(out); audit.log("AppointmentReport", null, "COMPLETED_APPOINTMENTS_EXCEL_EXPORTED", "Rows exported: "+list.size(), actor); return out.toByteArray();
        } catch(IOException e){throw new IllegalStateException("Unable to generate completed appointment Excel.",e);}
    }

    private CompletedAppointmentDetailResponse completedDetail(Appointment a) {
        List<DirectionFollowUp> actionItems = followUps.findByAppointment_IdOrderByCreatedAtAsc(a.getId());
        List<Direction> legacy = directions.findByAppointment_IdOrderByCreatedAtAsc(a.getId());
        List<CompletedAppointmentDetailResponse.DirectionItem> directionItems = new ArrayList<>();
        for (Direction d : legacy) directionItems.add(CompletedAppointmentDetailResponse.DirectionItem.builder().directionId("LEGACY-"+d.getId()).date(d.getCreatedAt()).direction(d.getDirectionText()).department(d.getAssignedDepartment()).officer(d.getAssignedOfficer()).dueDate(d.getDeadline()).followUpStatus(d.isCompleted()?"COMPLETED":text(d.getCurrentStatus())).build());
        for (DirectionFollowUp f : actionItems) directionItems.add(CompletedAppointmentDetailResponse.DirectionItem.builder().directionId(f.getDirectionId()).date(f.getCreatedAt()).direction(f.getInstruction()).department(f.getDepartment().getDepartmentName()).officer(f.getResponsibleOfficerName()).dueDate(f.getDueDate()).followUpStatus(followStatus(f)).build());
        List<CompletedAppointmentDetailResponse.ActionItem> mapped = actionItems.stream().map(f -> CompletedAppointmentDetailResponse.ActionItem.builder().id(f.getId()).directionId(f.getDirectionId()).department(f.getDepartment().getDepartmentName()).officer(f.getResponsibleOfficerName()).instruction(f.getInstruction()).dueDate(f.getDueDate()).status(followStatus(f)).evidenceRequired(f.getEvidenceRequired()).escalated(f.getLastEscalatedAt()!=null).completedDate(f.getCompletedDate()).completionRemarks(f.getCompletionRemarks()).build()).toList();
        String scheme = schemeNames(List.of(a)).get(a.getId());
        CompletedAppointmentDetailResponse detail = CompletedAppointmentDetailResponse.builder().applicant(applicant(a)).appointment(appointmentInfo(a, Map.of(a.getId(), text(scheme)))).petitionSummary(petition(a)).approverRemarks(a.getApproverRemarks()).hcmRemarks(a.getHcmRemarks()).forwardedDepartment(routed(a)).directions(directionItems).actionItems(mapped).documents(documentItems(documents.findByAppointmentId(a.getId()))).statusHistory(history(a.getId())).build();
        detail.setAiSummary(aiSummary(detail)); return detail;
    }

    private String aiSummary(CompletedAppointmentDetailResponse d) {
        String facts = "Applicant: "+text(d.getApplicant().getName())+"\nPetition: "+text(d.getPetitionSummary())+"\nDepartment: "+text(d.getAppointment().getDepartment())+"\nScheme: "+text(d.getAppointment().getScheme())+"\nOutcome: "+text(d.getAppointment().getMeetingOutcome())+"\nDirections: "+d.getDirections().stream().map(CompletedAppointmentDetailResponse.DirectionItem::getDirection).filter(Objects::nonNull).collect(Collectors.joining("; "))+"\nFollow-ups: "+d.getActionItems().stream().map(i->text(i.getInstruction())+" ["+text(i.getStatus())+"]").collect(Collectors.joining("; "));
        try { return aiClient.chatCompact("Summarize only the supplied MeghaConnect facts in 2-4 sentences. Do not invent facts, outcomes, departments, dates, or recommendations. This is an AI-assisted briefing, not an official direction.", facts, 220).filter(s->!s.isBlank()).orElse(AI_UNAVAILABLE); }
        catch (RuntimeException e) { return AI_UNAVAILABLE; }
    }

    private Specification<Appointment> specification(ExecutiveAppointmentReportFilter f, boolean completed) {
        ExecutiveAppointmentReportFilter filter=f==null?new ExecutiveAppointmentReportFilter():f;
        return (root, query, cb) -> {
            List<Predicate> p=new ArrayList<>();
            p.add(root.get("status").in(completed?statusPolicy.completedStatuses():statusPolicy.rejectedStatuses()));
            if(completed) p.add(cb.or(cb.and(cb.equal(root.get("appointmentCategory"),Appointment.AppointmentCategory.SCHEDULED),root.get("status").in(Appointment.AppointmentStatus.HCM_MET_COMPLETED,Appointment.AppointmentStatus.COMPLETED)),cb.and(cb.notEqual(root.get("appointmentCategory"),Appointment.AppointmentCategory.SCHEDULED),cb.equal(root.get("status"),Appointment.AppointmentStatus.COMPLETED))));
            if(has(filter.getStatus())) {
                try {
                    Appointment.AppointmentStatus requestedStatus=Appointment.AppointmentStatus.valueOf(filter.getStatus().trim().toUpperCase(Locale.ROOT));
                    Set<Appointment.AppointmentStatus> allowed=completed?statusPolicy.completedStatuses():statusPolicy.rejectedStatuses();
                    if(allowed.contains(requestedStatus)) p.add(cb.equal(root.get("status"),requestedStatus));
                    else p.add(cb.disjunction());
                } catch(IllegalArgumentException ignored) { p.add(cb.disjunction()); }
            }
            like(p,cb,root.get("applicationId"),filter.getApplicationId()); like(p,cb,root.get("applicant").get("fullName"),filter.getApplicantName()); like(p,cb,root.get("applicant").get("epicNumber"),filter.getEpic()); like(p,cb,root.get("applicant").get("phoneNumber"),filter.getMobile());
            eq(p,cb,root.get("applicant").get("constituency"),filter.getConstituency()); eq(p,cb,root.get("applicant").get("district"),filter.getDistrict()); eq(p,cb,root.get("agendaType"),filter.getAgendaType()); like(p,cb,root.get("appointmentType"),filter.getAppointmentType()); like(p,cb,root.get("referredByName"),filter.getMla()); like(p,cb,root.get("routedOfficer"),filter.getResponsibleOfficer()); like(p,cb,root.get("rejectedBy"),filter.getRejectedBy()); like(p,cb,root.get("rejectionReason"),filter.getRejectionReason());
            if(filter.getDepartmentId()!=null)p.add(cb.or(cb.equal(root.get("tenantDepartment").get("id"),filter.getDepartmentId()),cb.equal(root.get("routedDepartment").get("id"),filter.getDepartmentId())));
            if(has(filter.getDepartment()))p.add(cb.or(cb.like(cb.lower(root.get("tenantDepartment").get("departmentName")),"%"+filter.getDepartment().trim().toLowerCase()+"%"),cb.like(cb.lower(root.get("routedDepartment").get("departmentName")),"%"+filter.getDepartment().trim().toLowerCase()+"%"),cb.like(cb.lower(root.get("department")),"%"+filter.getDepartment().trim().toLowerCase()+"%")));
            if(has(filter.getAppointmentCategory()))p.add(cb.equal(root.get("appointmentCategory"),Appointment.AppointmentCategory.valueOf(filter.getAppointmentCategory().toUpperCase())));
            Path<LocalDateTime> date=root.get(completed?"completedAt":"rejectedAt"); if(filter.getFromDate()!=null)p.add(cb.greaterThanOrEqualTo(date,filter.getFromDate().atStartOfDay())); if(filter.getToDate()!=null)p.add(cb.lessThan(date,filter.getToDate().plusDays(1).atStartOfDay()));
            if(has(filter.getScheme())){Subquery<Long> sq=query.subquery(Long.class);Root<SchemeApplication>s=sq.from(SchemeApplication.class);sq.select(s.get("appointment").get("id")).where(cb.equal(cb.upper(s.get("schemeType").as(String.class)),filter.getScheme().toUpperCase()));p.add(cb.exists(sq.where(cb.and(cb.equal(s.get("appointment").get("id"),root.get("id")),cb.equal(cb.upper(s.get("schemeType").as(String.class)),filter.getScheme().toUpperCase())))));}
            if(has(filter.getFollowUpStatus())){Subquery<Long>sq=query.subquery(Long.class);Root<DirectionFollowUp>x=sq.from(DirectionFollowUp.class);List<Predicate>fp=new ArrayList<>();fp.add(cb.equal(x.get("appointment").get("id"),root.get("id")));if("OVERDUE".equalsIgnoreCase(filter.getFollowUpStatus())){fp.add(cb.notEqual(x.get("status"),DirectionFollowUp.FollowUpStatus.COMPLETED));fp.add(cb.lessThan(x.get("dueDate"),LocalDate.now()));}else fp.add(cb.equal(x.get("status"),DirectionFollowUp.FollowUpStatus.valueOf(filter.getFollowUpStatus().toUpperCase())));sq.select(x.get("appointment").get("id")).where(fp.toArray(Predicate[]::new));p.add(cb.exists(sq));}
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private Map<Long,Enrichment> enrich(List<Appointment> list){Map<Long,Enrichment>m=new HashMap<>();List<Long>ids=list.stream().map(Appointment::getId).toList();ids.forEach(id->m.put(id,new Enrichment()));if(ids.isEmpty())return m;followUps.findByAppointment_IdIn(ids).forEach(f->m.get(f.getAppointment().getId()).followUps.add(f));documents.findByAppointment_IdIn(ids).forEach(d->m.get(d.getAppointment().getId()).documents.add(d));schemes.findByAppointment_IdIn(ids).forEach(s->m.get(s.getAppointment().getId()).schemes.add(s));return m;}
    private Map<Long,String> schemeNames(List<Appointment> list){Map<Long,Enrichment>e=enrich(list);Map<Long,String>r=new HashMap<>();e.forEach((id,v)->r.put(id,v.schemes.stream().map(s->s.getSchemeType().name()).distinct().collect(Collectors.joining(", "))));return r;}
    private CompletedAppointmentSummaryResponse completedSummary(Appointment a,Enrichment e){Visitor v=a.getApplicant();DirectionFollowUp first=e.followUps.stream().findFirst().orElse(null);return CompletedAppointmentSummaryResponse.builder().appointmentId(a.getId()).applicationId(a.getApplicationId()).applicantName(v.getFullName()).epic(mask(v.getEpicNumber())).mobile(mask(v.getPhoneNumber())).photoAvailable(has(photoPath(v))).appointmentCategory(a.getAppointmentCategory().name()).appointmentType(a.getAppointmentType()).department(department(a)).scheme(e.schemes.stream().map(s->s.getSchemeType().name()).distinct().collect(Collectors.joining(", "))).constituency(v.getConstituency()).district(v.getDistrict()).mla(a.getReferredByName()).agendaType(a.getAgendaType()).requestedAt(a.getCreatedAt()).scheduledAt(a.getScheduledDateTime()).meetingAt(a.getScheduledDateTime()).completedAt(a.getCompletedAt()).directionSummary(e.followUps.stream().map(DirectionFollowUp::getInstruction).collect(Collectors.joining("; "))).assignedDepartment(first!=null?first.getDepartment().getDepartmentName():routed(a)).followUpStatus(overallFollow(e.followUps)).responsibleOfficer(first!=null?first.getResponsibleOfficerName():a.getRoutedOfficer()).dueDate(first!=null?first.getDueDate():null).status(a.getStatus().name()).build();}
    private RejectedAppointmentSummaryResponse rejectedSummary(Appointment a,Enrichment e){Visitor v=a.getApplicant();return RejectedAppointmentSummaryResponse.builder().appointmentId(a.getId()).applicationId(a.getApplicationId()).applicantName(v.getFullName()).epic(mask(v.getEpicNumber())).mobile(mask(v.getPhoneNumber())).department(department(a)).scheme(e.schemes.stream().map(s->s.getSchemeType().name()).distinct().collect(Collectors.joining(", "))).constituency(v.getConstituency()).district(v.getDistrict()).mla(a.getReferredByName()).agendaType(a.getAgendaType()).appointmentType(a.getAppointmentType()).requestedAt(a.getCreatedAt()).rejectedAt(a.getRejectedAt()).rejectedBy(a.getRejectedBy()).rejectionReason(a.getRejectionReason()).status(a.getStatus().name()).build();}
    private CompletedAppointmentDetailResponse.Applicant applicant(Appointment a){Visitor v=a.getApplicant();return CompletedAppointmentDetailResponse.Applicant.builder().id(v.getId()).name(v.getFullName()).epic(mask(v.getEpicNumber())).mobile(mask(v.getPhoneNumber())).address(first(v.getFullAddress(),v.getAddress(),v.getAddressLine())).constituency(v.getConstituency()).district(v.getDistrict()).pincode(v.getPincode()).photoAvailable(has(photoPath(v))).build();}
    private CompletedAppointmentDetailResponse.AppointmentInfo appointmentInfo(Appointment a,Map<Long,String>s){return CompletedAppointmentDetailResponse.AppointmentInfo.builder().id(a.getId()).applicationId(a.getApplicationId()).category(a.getAppointmentCategory().name()).type(a.getAppointmentType()).source(a.getAppointmentSource()).requestedAt(a.getCreatedAt()).scheduledAt(a.getScheduledDateTime()).meetingAt(a.getScheduledDateTime()).completedAt(a.getCompletedAt()).department(department(a)).scheme(s.get(a.getId())).mla(a.getReferredByName()).agendaType(a.getAgendaType()).purpose(first(a.getReasonForAppointment(),a.getSubject())).meetingOutcome(a.getMeetingOutcome()).status(a.getStatus().name()).build();}
    private List<CompletedAppointmentDetailResponse.DocumentItem> documentItems(List<DocumentUpload>ds){return ds.stream().map(d->CompletedAppointmentDetailResponse.DocumentItem.builder().id(d.getId()).filename(d.getOriginalFilename()).documentType(d.getDocumentType()).contentType(d.getContentType()).fileSizeBytes(d.getFileSizeBytes()).uploadedDate(d.getUploadedDate()).uploadedBy(d.getUploadedBy()).uploaderRole(d.getUploaderRole()).remarks(d.getRemarks()).build()).toList();}
    private List<CompletedAppointmentDetailResponse.StatusHistoryItem> history(Long id){return audits.findByAppointment_IdOrderByCreatedAtAsc(id).stream().map(x->CompletedAppointmentDetailResponse.StatusHistoryItem.builder().oldStatus(text(x.getOldStatus())).newStatus(text(x.getNewStatus())).action(x.getAction()).remarks(x.getRemarks()).performedBy(x.getPerformedBy()).performedRole(x.getPerformedRole()).timestamp(x.getCreatedAt()).build()).toList();}
    private Pageable safePageable(Pageable p,String fallback){Pageable in=p==null?PageRequest.of(0,25):p;List<Sort.Order>orders=in.getSort().stream().filter(o->SORT_FIELDS.contains(o.getProperty())).toList();Sort sort=orders.isEmpty()?Sort.by(Sort.Direction.DESC,fallback):Sort.by(orders);return PageRequest.of(in.getPageNumber(),Math.min(Math.max(in.getPageSize(),1),100),sort);}
    private Appointment appointment(Long id){return appointments.findById(id).orElseThrow(()->new ResourceNotFoundException("Appointment not found"));}
    private String petition(Appointment a){return first(a.getAgendaBrief(),a.getReasonForAppointment(),a.getSubject(),a.getShortNotes());}
    private String department(Appointment a){return a.getTenantDepartment()!=null?a.getTenantDepartment().getDepartmentName():first(a.getDepartment(),routed(a));} private String routed(Appointment a){return a.getRoutedDepartment()!=null?a.getRoutedDepartment().getDepartmentName():null;}
    private String overallFollow(List<DirectionFollowUp>fs){if(fs.stream().anyMatch(f->f.isOverdue(LocalDate.now())))return "OVERDUE";if(fs.stream().anyMatch(f->f.getStatus()==DirectionFollowUp.FollowUpStatus.IN_PROGRESS))return "IN_PROGRESS";if(fs.stream().anyMatch(f->f.getStatus()==DirectionFollowUp.FollowUpStatus.PENDING))return "PENDING";return fs.isEmpty()?"NONE":"COMPLETED";} private String followStatus(DirectionFollowUp f){return f.isOverdue(LocalDate.now())?"OVERDUE":f.getStatus().name();}
    private String photoPath(Visitor v){return first(v.getLivePhotoPath(),v.getPhotoStoragePath(),v.getPhotoPath());} private String mask(String s){return !has(s)||s.length()<=4?s:"XXXX"+s.substring(s.length()-4);} private static String first(String...v){return Arrays.stream(v).filter(ExecutiveAppointmentReportService::has).findFirst().orElse(null);} private static boolean has(String s){return s!=null&&!s.isBlank();} private static String text(Object o){return o==null?"":String.valueOf(o).replaceAll("[\\r\\n]"," ");}
    private void like(List<Predicate>p,CriteriaBuilder cb,Path<String>x,String v){if(has(v))p.add(cb.like(cb.lower(x),"%"+v.trim().toLowerCase()+"%"));} private void eq(List<Predicate>p,CriteriaBuilder cb,Path<String>x,String v){if(has(v))p.add(cb.equal(cb.lower(x),v.trim().toLowerCase()));}
    private Sheet sheet(Workbook w,String name,String[]headers){Sheet s=w.createSheet(name);Row r=s.createRow(0);CellStyle st=w.createCellStyle();Font f=w.createFont();f.setBold(true);st.setFont(f);for(int i=0;i<headers.length;i++){Cell c=r.createCell(i);c.setCellValue(headers[i]);c.setCellStyle(st);s.setColumnWidth(i,Math.min(40,Math.max(14,headers[i].length()+4))*256);}s.createFreezePane(0,1);s.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,0,0,headers.length-1));return s;} private void cells(Row r,Object...v){for(int i=0;i<v.length;i++)r.createCell(i).setCellValue(text(v[i]));}
    private void drawPhoto(PDDocument pdf,PdfWriter w,Visitor v)throws IOException{Optional<String>uri=fileStorage.loadImageDataUri(photoPath(v));if(uri.isEmpty()){w.line("Photo Not Available");return;}byte[]b=Base64.getDecoder().decode(uri.get().substring(uri.get().indexOf(',')+1));PDImageXObject image=PDImageXObject.createFromByteArray(pdf,b,"visitor-photo");w.image(image,90,110);}
    private static class Enrichment{final List<DirectionFollowUp>followUps=new ArrayList<>();final List<DocumentUpload>documents=new ArrayList<>();final List<SchemeApplication>schemes=new ArrayList<>();}

    static class PdfWriter {
        private static final float MARGIN = 46;
        private static final float BOTTOM_MARGIN = 45;
        private final PDDocument pdf; private PDPage page; private PDPageContentStream out; private float y; private float contentWidth;
        PdfWriter(PDDocument pdf)throws IOException{this.pdf=pdf;newPage();}
        void newPage()throws IOException{if(out!=null)out.close();page=new PDPage(PDRectangle.A4);pdf.addPage(page);out=new PDPageContentStream(pdf,page);contentWidth=page.getMediaBox().getWidth()-MARGIN-MARGIN;y=page.getMediaBox().getHeight()-MARGIN;}
        void ensure(float needed)throws IOException{if(y-needed<BOTTOM_MARGIN)newPage();}
        void title(String a,String b)throws IOException{write(a,16,true);write(b,14,true);y-=8;}
        void heading(String value)throws IOException{ensure(52);y-=7;lineRule(y);y-=17;writeAt(value,11,true,y);y-=8;lineRule(y);y-=10;}
        void field(String label,Object value)throws IOException{paragraph(label+": "+text(value));}
        void line(String value)throws IOException{write(value,9,false);}
        void paragraph(String value)throws IOException{String clean=text(value);if(clean.isBlank())clean="—";for(String line:wrap(clean,92))write(line,9,false);y-=3;}
        void write(String value,float size,boolean bold)throws IOException{ensure(size+6);writeAt(value,size,bold,y);y-=size+4;}
        void writeAt(String value,float size,boolean bold,float baseline)throws IOException{out.beginText();out.setFont(bold?PDType1Font.HELVETICA_BOLD:PDType1Font.HELVETICA,size);out.newLineAtOffset(MARGIN,baseline);out.showText(ascii(value));out.endText();}
        void lineRule(float lineY)throws IOException{out.setLineWidth(.6f);out.moveTo(MARGIN,lineY);out.lineTo(MARGIN+contentWidth,lineY);out.stroke();}
        void image(PDImageXObject image,float width,float height)throws IOException{ensure(height+8);float ratio=Math.min(width/image.getWidth(),height/image.getHeight());float w=image.getWidth()*ratio,h=image.getHeight()*ratio;out.drawImage(image,MARGIN,y-h,w,h);y-=h+8;}
        void close()throws IOException{if(out!=null){out.close();out=null;}}
        void addPageNumbers()throws IOException{int total=pdf.getNumberOfPages();for(int i=0;i<total;i++){try(PDPageContentStream footer=new PDPageContentStream(pdf,pdf.getPage(i),PDPageContentStream.AppendMode.APPEND,true)){footer.beginText();footer.setFont(PDType1Font.HELVETICA,8);footer.newLineAtOffset(270,22);footer.showText("Page "+(i+1)+" of "+total);footer.endText();}}}
        static List<String>wrap(String s,int max){List<String>r=new ArrayList<>();StringBuilder line=new StringBuilder();for(String word:s.split("\\s+")){if(line.length()+word.length()+1>max){r.add(line.toString());line.setLength(0);}if(line.length()>0)line.append(' ');line.append(word);}if(line.length()>0)r.add(line.toString());return r.isEmpty()?List.of(""):r;} static String ascii(String s){return new String(text(s).getBytes(StandardCharsets.US_ASCII),StandardCharsets.US_ASCII);}
    }
}
