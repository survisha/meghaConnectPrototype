package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Grievance;
import com.survisha.meghaconnect.entity.Grievance.GrievanceStatus;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.GrievanceRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.util.ValidationConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final VisitorRepository visitorRepository;
    private final AuditLogService auditLogService;
    private final RequestValidationService validationService;

    public Page<Grievance> findAll(Pageable pageable) {
        return grievanceRepository.findAll(pageable);
    }

    public Optional<Grievance> findById(Long id) {
        return grievanceRepository.findById(id);
    }

    public Page<Grievance> findByVisitorId(Long visitorId, Pageable pageable) {
        return grievanceRepository.findByVisitorId(visitorId, pageable);
    }

    public Optional<Grievance> findByIdForVisitor(Long id, Long visitorId) {
        return grievanceRepository.findByIdAndVisitorId(id, visitorId);
    }

    @Transactional
    public Grievance createForVisitor(Grievance grievance, Long visitorId, String createdBy) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Visitor not found: " + visitorId));
        grievance.setVisitor(visitor);
        return create(grievance, createdBy);
    }

    @Transactional
    public Grievance create(Grievance grievance, String createdBy) {
        grievance.setStatus(GrievanceStatus.SUBMITTED);
        grievance.setSubmittedAt(LocalDateTime.now());
        grievance.setCreatedBy(createdBy);
        grievance.setUpdatedBy(createdBy);
        // Temporary placeholder; replaced with DB-ID-based value after persist (no race condition)
        grievance.setTicketId("GRV-TMP-" + System.currentTimeMillis());
        Grievance saved = grievanceRepository.save(grievance);
        grievanceRepository.flush();
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String ticketId = "GRV-" + year + "-" + String.format("%05d", saved.getId());
        saved.setTicketId(ticketId);
        saved = grievanceRepository.save(saved);
        auditLogService.log("Grievance", saved.getId(), "CREATED",
                "New grievance submitted: " + ticketId, createdBy);
        return saved;
    }

    @Transactional
    public Grievance update(Long id, Grievance changes, String updatedBy) {
        Grievance grievance = grievanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grievance not found: " + id));
        applyEditableFields(grievance, changes, false);
        grievance.setUpdatedBy(updatedBy);
        Grievance saved = grievanceRepository.save(grievance);
        auditLogService.log("Grievance", saved.getId(), "UPDATED",
                "Grievance updated: " + saved.getTicketId(), updatedBy);
        return saved;
    }

    @Transactional
    public Grievance updateForVisitor(Long id, Long visitorId, Grievance changes, String updatedBy) {
        Grievance grievance = grievanceRepository.findByIdAndVisitorId(id, visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Grievance not found: " + id));
        ensureVisitorEditable(grievance);
        applyEditableFields(grievance, changes, true);
        grievance.setUpdatedBy(updatedBy);
        Grievance saved = grievanceRepository.save(grievance);
        auditLogService.log("Grievance", saved.getId(), "UPDATED",
                "Citizen updated grievance: " + saved.getTicketId(), updatedBy);
        return saved;
    }

    @Transactional
    public void delete(Long id, String deletedBy) {
        Grievance grievance = grievanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grievance not found: " + id));
        grievanceRepository.delete(grievance);
        auditLogService.log("Grievance", id, "DELETED",
                "Grievance deleted: " + grievance.getTicketId(), deletedBy);
    }

    @Transactional
    public void deleteForVisitor(Long id, Long visitorId, String deletedBy) {
        Grievance grievance = grievanceRepository.findByIdAndVisitorId(id, visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Grievance not found: " + id));
        ensureVisitorEditable(grievance);
        grievanceRepository.delete(grievance);
        auditLogService.log("Grievance", id, "DELETED",
                "Citizen deleted grievance: " + grievance.getTicketId(), deletedBy);
    }

    @Transactional
    public Grievance updateStatus(Long id, Map<String, String> body, String updatedBy) {
        GrievanceStatus status = validationService.requireEnum(
                body != null ? body.get(ValidationConstants.FIELD_STATUS) : null,
                GrievanceStatus.class,
                ValidationConstants.FIELD_STATUS
        );
        String remarks = body != null ? body.get("remarks") : null;
        return updateStatus(id, status, remarks, updatedBy);
    }

    @Transactional
    public Grievance updateStatus(Long id, GrievanceStatus newStatus, String remarks, String updatedBy) {
        Grievance g = grievanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grievance not found: " + id));
        g.setStatus(newStatus);
        if (remarks != null) g.setRemarks(remarks);
        if (newStatus == GrievanceStatus.RESOLVED) g.setResolvedAt(LocalDateTime.now());
        g.setUpdatedBy(updatedBy);
        Grievance saved = grievanceRepository.save(g);
        auditLogService.log("Grievance", saved.getId(), "STATUS_CHANGE",
                "Status changed to " + newStatus, updatedBy);
        return saved;
    }

    private void applyEditableFields(Grievance grievance, Grievance changes, boolean visitorOwned) {
        if (changes == null) {
            return;
        }
        if (changes.getSubject() != null) {
            grievance.setSubject(changes.getSubject().trim());
        }
        if (changes.getDescription() != null) {
            grievance.setDescription(changes.getDescription().trim());
        }
        if (!visitorOwned) {
            grievance.setAssignedDepartment(trimToNull(changes.getAssignedDepartment()));
            grievance.setRemarks(trimToNull(changes.getRemarks()));
        }
    }

    private void ensureVisitorEditable(Grievance grievance) {
        if (grievance.getStatus() == GrievanceStatus.RESOLVED || grievance.getStatus() == GrievanceStatus.CLOSED) {
            throw new IllegalStateException("Resolved or closed grievances cannot be changed by the visitor");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
