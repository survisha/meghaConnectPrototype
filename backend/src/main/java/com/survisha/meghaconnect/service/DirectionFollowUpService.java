package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.CreateDirectionFollowUpRequest;
import com.survisha.meghaconnect.dto.DirectionFollowUpDto;
import com.survisha.meghaconnect.dto.FollowUpEvidenceDto;
import com.survisha.meghaconnect.entity.*;
import com.survisha.meghaconnect.exception.ResourceNotFoundException;
import com.survisha.meghaconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectionFollowUpService {
    private final DirectionFollowUpRepository repository;
    private final AppointmentRepository appointmentRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final DocumentUploadRepository documentUploadRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public DirectionFollowUpDto create(CreateDirectionFollowUpRequest request, String actor) {
        if (request == null || request.getAppointmentId() == null || request.getDepartmentId() == null
                || request.getInstruction() == null || request.getInstruction().isBlank()) {
            throw new IllegalArgumentException("Appointment, department and instruction are required.");
        }
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        if (!Boolean.TRUE.equals(appointment.getFollowUpRequired())) {
            throw new IllegalStateException("Appointment is not marked as requiring follow-up.");
        }
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        DirectionFollowUp item = DirectionFollowUp.builder()
                .appointment(appointment).visitor(appointment.getApplicant()).department(department)
                .responsibleOfficerName(trim(request.getResponsibleOfficerName()))
                .instruction(request.getInstruction().trim()).dueDate(request.getDueDate())
                .priority(request.getPriority() != null ? request.getPriority() : DirectionFollowUp.Priority.NORMAL)
                .evidenceRequired(Boolean.TRUE.equals(request.getEvidenceRequired())).build();
        item.setCreatedBy(actor); item.setUpdatedBy(actor);
        item = repository.saveAndFlush(item);
        item.setDirectionId("DIR-" + Year.now().getValue() + "-" + String.format("%06d", item.getId()));
        item = repository.save(item);
        auditLogService.log("DirectionFollowUp", item.getId(), "FOLLOW_UP_CREATED",
                "Direction " + item.getDirectionId() + " assigned to " + department.getDepartmentName(), actor);
        return toDto(item);
    }

    public Page<DirectionFollowUpDto> find(String actor, String status, Long departmentId,
                                            Boolean overdue, Pageable pageable) {
        Long enforcedDepartment = scopedDepartmentId(actor);
        Long effectiveDepartment = enforcedDepartment != null ? enforcedDepartment : departmentId;
        Specification<DirectionFollowUp> spec = (root, query, cb) -> cb.conjunction();
        if (effectiveDepartment != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), effectiveDepartment));
        }
        if (status != null && !status.isBlank() && !"OVERDUE".equalsIgnoreCase(status)) {
            DirectionFollowUp.FollowUpStatus parsed = DirectionFollowUp.FollowUpStatus.valueOf(status.trim().toUpperCase());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), parsed));
        }
        if (Boolean.TRUE.equals(overdue) || "OVERDUE".equalsIgnoreCase(status)) {
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.notEqual(root.get("status"), DirectionFollowUp.FollowUpStatus.COMPLETED),
                    cb.lessThan(root.get("dueDate"), LocalDate.now())));
        }
        return repository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional
    public DirectionFollowUpDto updateStatus(Long id, DirectionFollowUp.FollowUpStatus target,
                                              String remarks, String actor) {
        DirectionFollowUp item = accessible(id, actor);
        DirectionFollowUp.FollowUpStatus old = item.getStatus();
        boolean valid = (old == DirectionFollowUp.FollowUpStatus.PENDING && target == DirectionFollowUp.FollowUpStatus.IN_PROGRESS)
                || (old == DirectionFollowUp.FollowUpStatus.IN_PROGRESS && target == DirectionFollowUp.FollowUpStatus.COMPLETED)
                || (old == DirectionFollowUp.FollowUpStatus.PENDING && target == DirectionFollowUp.FollowUpStatus.COMPLETED);
        if (!valid) throw new IllegalStateException("Invalid follow-up transition: " + old + " -> " + target);
        item.setStatus(target); item.setUpdatedBy(actor);
        if (target == DirectionFollowUp.FollowUpStatus.COMPLETED) {
            item.setCompletedDate(LocalDateTime.now()); item.setCompletionRemarks(trim(remarks));
        }
        item = repository.save(item);
        auditLogService.log("DirectionFollowUp", id, "FOLLOW_UP_UPDATED",
                old + " -> " + target, actor);
        return toDto(item);
    }

    @Transactional
    public FollowUpEvidenceDto uploadEvidence(Long id, MultipartFile file, String documentType, String actor) {
        DirectionFollowUp item = accessible(id, actor);
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Evidence file is required.");
        try {
            FileStorageService.StoredFileMetadata stored = fileStorageService.storeFileSecure(
                    file, item.getVisitor().getId(), item.getAppointment().getApplicationId() + "-" + item.getDirectionId());
            LocalDateTime now = LocalDateTime.now();
            DocumentUpload document = DocumentUpload.builder()
                    .followUp(item).appointment(item.getAppointment()).visitor(item.getVisitor())
                    .documentType(trim(documentType) != null ? documentType.trim() : "FOLLOW_UP_EVIDENCE")
                    .originalFilename(stored.getOriginalFileName()).storedFileName(stored.getStoredFileName())
                    .filePath(stored.getEncryptedFilePath()).encryptedFilePath(stored.getEncryptedFilePath())
                    .secureHash(stored.getSecureHash()).fileSizeBytes(stored.getFileSize())
                    .mimeType(stored.getContentType()).contentType(stored.getContentType())
                    .uploadedBy(actor).uploadedDate(now).createdAt(now).updatedAt(now).build();
            document = documentUploadRepository.save(document);
            auditLogService.log("DirectionFollowUp", id, "FOLLOW_UP_EVIDENCE_UPLOADED",
                    "Evidence metadata saved: " + document.getOriginalFilename(), actor);
            return evidenceDto(document);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to store follow-up evidence.", exception);
        }
    }

    public List<FollowUpEvidenceDto> evidence(Long id, String actor) {
        accessible(id, actor);
        return documentUploadRepository.findByFollowUp_IdOrderByUploadedDateDesc(id).stream()
                .map(this::evidenceDto).toList();
    }

    private FollowUpEvidenceDto evidenceDto(DocumentUpload document) {
        return FollowUpEvidenceDto.builder().id(document.getId())
                .followUpId(document.getFollowUp().getId()).filename(document.getOriginalFilename())
                .documentType(document.getDocumentType()).contentType(document.getContentType())
                .fileSizeBytes(document.getFileSizeBytes()).uploadedBy(document.getUploadedBy())
                .uploadedDate(document.getUploadedDate()).build();
    }

    private DirectionFollowUp accessible(Long id, String actor) {
        DirectionFollowUp item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found"));
        Long scope = scopedDepartmentId(actor);
        if (scope != null && !scope.equals(item.getDepartment().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Follow-up belongs to another department.");
        }
        return item;
    }

    private Long scopedDepartmentId(String actor) {
        return userRepository.findByNormalizedUsername(actor == null ? "" : actor)
                .filter(user -> user.getRole() == User.UserRole.DEPARTMENT_ADMIN
                        || user.getRole() == User.UserRole.DEPARTMENT_PA)
                .map(User::getDepartment).map(Department::getId).orElse(null);
    }

    public DirectionFollowUpDto toDto(DirectionFollowUp item) {
        LocalDate today = LocalDate.now();
        boolean overdue = item.isOverdue(today);
        return DirectionFollowUpDto.builder().id(item.getId()).directionId(item.getDirectionId())
                .appointmentId(item.getAppointment().getId()).visitorId(item.getVisitor().getId())
                .departmentId(item.getDepartment().getId()).departmentName(item.getDepartment().getDepartmentName())
                .responsibleOfficerName(item.getResponsibleOfficerName()).instruction(item.getInstruction())
                .dueDate(item.getDueDate()).status(overdue ? "OVERDUE" : item.getStatus().name())
                .priority(item.getPriority()).evidenceRequired(item.getEvidenceRequired())
                .daysOverdue(overdue ? ChronoUnit.DAYS.between(item.getDueDate(), today) : 0)
                .completedDate(item.getCompletedDate()).completionRemarks(item.getCompletionRemarks())
                .createdAt(item.getCreatedAt()).createdBy(item.getCreatedBy()).build();
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
