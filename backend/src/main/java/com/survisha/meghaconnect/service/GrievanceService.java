package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Grievance;
import com.survisha.meghaconnect.entity.Grievance.GrievanceStatus;
import com.survisha.meghaconnect.repository.GrievanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final AuditLogService auditLogService;

    public Page<Grievance> findAll(Pageable pageable) {
        return grievanceRepository.findAll(pageable);
    }

    public Optional<Grievance> findById(Long id) {
        return grievanceRepository.findById(id);
    }

    public List<Grievance> findByPhone(String phoneNumber) {
        return grievanceRepository.findByPhoneNumber(phoneNumber);
    }

    @Transactional
    public Grievance create(Grievance grievance, String createdBy) {
        grievance.setStatus(GrievanceStatus.SUBMITTED);
        grievance.setSubmittedAt(LocalDateTime.now());
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
    public Grievance updateStatus(Long id, GrievanceStatus newStatus, String remarks, String updatedBy) {
        Grievance g = grievanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grievance not found: " + id));
        g.setStatus(newStatus);
        if (remarks != null) g.setRemarks(remarks);
        if (newStatus == GrievanceStatus.RESOLVED) g.setResolvedAt(LocalDateTime.now());
        Grievance saved = grievanceRepository.save(g);
        auditLogService.log("Grievance", saved.getId(), "STATUS_CHANGE",
                "Status changed to " + newStatus, updatedBy);
        return saved;
    }
}
