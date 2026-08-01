package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.DepartmentAccessRequestDto;
import com.survisha.meghaconnect.dto.CreateDepartmentAccessRequest;
import com.survisha.meghaconnect.entity.Department;
import com.survisha.meghaconnect.entity.DepartmentAccessRequest;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.DepartmentAccessRequestRepository;
import com.survisha.meghaconnect.repository.DepartmentRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import com.survisha.meghaconnect.dto.CreateUserRequest;
import com.survisha.meghaconnect.dto.DepartmentApprovalResult;
import com.survisha.meghaconnect.entity.User;
import java.security.SecureRandom;
import com.survisha.meghaconnect.entity.ReferenceData;
import com.survisha.meghaconnect.repository.ReferenceDataRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;

@Service @RequiredArgsConstructor
public class DepartmentAccessRequestService {
    private final DepartmentAccessRequestRepository requestRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;
    private final ReferenceDataRepository referenceDataRepository;

    @Transactional
    public DepartmentAccessRequestDto submit(CreateDepartmentAccessRequest dto) {
        String code = dto.getDepartmentCode().trim().toUpperCase(Locale.ROOT);
        ReferenceData departmentReference = referenceDataRepository
                .findByTypeCodeAndCodeAndIsActive("DEPARTMENT", code, true)
                .orElseThrow(() -> new MeghaConnectException("INVALID_DEPARTMENT",
                        "Please select an active department", 400));
        if (requestRepository.existsByDepartmentCodeIgnoreCaseAndRequestStatus(code, DepartmentAccessRequest.Status.PENDING)) {
            throw new MeghaConnectException("DUPLICATE_DEPARTMENT_REQUEST", "A pending request already exists", 409);
        }
        DepartmentAccessRequest entity = DepartmentAccessRequest.builder()
                .departmentName(departmentReference.getValue()).departmentCode(code)
                .nodalOfficerName(dto.getNodalOfficerName().trim())
                .officialEmail(dto.getOfficialEmail().trim().toLowerCase(Locale.ROOT))
                .officialMobile(dto.getOfficialMobile().trim())
                .requestPurpose(dto.getRequestPurpose().trim()).expectedUserCount(dto.getExpectedUserCount())
                .remarks(trim(dto.getRemarks())).requestStatus(DepartmentAccessRequest.Status.PENDING)
                .submittedAt(DateTimeUtil.nowIST()).build();
        entity.setCreatedBy("public-department-request");
        DepartmentAccessRequest saved;
        try {
            saved = requestRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException duplicate) {
            throw new MeghaConnectException("DUPLICATE_DEPARTMENT_REQUEST",
                    "A request for the selected department is already pending", 409);
        }
        auditLogService.log("DEPARTMENT_REQUEST", saved.getId(), "DEPARTMENT_REQUEST_SUBMITTED",
                "Department access request submitted for code " + code, "system");
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<DepartmentAccessRequestDto> list(DepartmentAccessRequest.Status status, Pageable pageable) {
        Page<DepartmentAccessRequest> rows = status == null ? requestRepository.findAllByOrderBySubmittedAtDesc(pageable)
                : requestRepository.findByRequestStatusOrderBySubmittedAtDesc(status, pageable);
        return rows.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public DepartmentAccessRequestDto get(Long id) {
        return requestRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new MeghaConnectException("DEPARTMENT_REQUEST_NOT_FOUND",
                        "Department request not found", 404));
    }

    @Transactional
    public DepartmentApprovalResult approve(Long id, String actor) {
        DepartmentAccessRequest request = pending(id);
        Department department = departmentRepository.findByDepartmentCodeIgnoreCase(request.getDepartmentCode())
                .orElseGet(() -> Department.builder().departmentCode(request.getDepartmentCode())
                        .departmentName(request.getDepartmentName()).contactEmail(request.getOfficialEmail())
                        .contactMobile(request.getOfficialMobile()).status(Department.DepartmentStatus.ACTIVE).build());
        department.setStatus(Department.DepartmentStatus.ACTIVE);
        department.setUpdatedBy(actor);
        if (department.getCreatedBy() == null) department.setCreatedBy(actor);
        request.setDepartment(departmentRepository.save(department));
        request.setRequestStatus(DepartmentAccessRequest.Status.APPROVED);
        review(request, actor);
        DepartmentAccessRequest saved = requestRepository.save(request);
        String temporaryPassword = temporaryPassword();
        String username = uniqueAdminUsername(saved);
        CreateUserRequest adminRequest = CreateUserRequest.builder()
                .username(username).password(temporaryPassword).fullName(saved.getNodalOfficerName())
                .email(saved.getOfficialEmail()).phoneNumber(saved.getOfficialMobile())
                .role(User.UserRole.DEPARTMENT_ADMIN).departmentId(saved.getDepartment().getId())
                .active(true).offlineAccess(false).build();
        com.survisha.meghaconnect.dto.UserResponse admin = userService.createUser(adminRequest, actor);
        auditLogService.log("DEPARTMENT_REQUEST", id, "DEPARTMENT_REQUEST_APPROVED",
                "Department request approved", actor);
        return DepartmentApprovalResult.builder().request(toDto(saved)).departmentAdmin(admin)
                .oneTimeTemporaryPassword(temporaryPassword).build();
    }

    @Transactional
    public DepartmentAccessRequestDto reject(Long id, String reason, String actor) {
        DepartmentAccessRequest request = pending(id);
        request.setRequestStatus(DepartmentAccessRequest.Status.REJECTED);
        request.setRejectionReason(reason.trim());
        review(request, actor);
        DepartmentAccessRequest saved = requestRepository.save(request);
        auditLogService.log("DEPARTMENT_REQUEST", id, "DEPARTMENT_REQUEST_REJECTED",
                "Department request rejected", actor);
        return toDto(saved);
    }

    private DepartmentAccessRequest pending(Long id) {
        DepartmentAccessRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new MeghaConnectException("DEPARTMENT_REQUEST_NOT_FOUND", "Department request not found", 404));
        if (request.getRequestStatus() != DepartmentAccessRequest.Status.PENDING)
            throw new MeghaConnectException("DEPARTMENT_REQUEST_ALREADY_REVIEWED", "Department request is already reviewed", 409);
        return request;
    }
    private void review(DepartmentAccessRequest request, String actor) {
        request.setReviewedAt(DateTimeUtil.nowIST()); request.setReviewedBy(actor); request.setUpdatedBy(actor);
    }
    private DepartmentAccessRequestDto toDto(DepartmentAccessRequest e) {
        return DepartmentAccessRequestDto.builder().id(e.getId())
                .departmentId(e.getDepartment() == null ? null : e.getDepartment().getId())
                .departmentName(e.getDepartmentName()).departmentCode(e.getDepartmentCode())
                .nodalOfficerName(e.getNodalOfficerName()).officialEmail(e.getOfficialEmail())
                .officialMobile(e.getOfficialMobile()).requestPurpose(e.getRequestPurpose())
                .expectedUserCount(e.getExpectedUserCount()).remarks(e.getRemarks())
                .requestStatus(e.getRequestStatus()).submittedAt(e.getSubmittedAt())
                .reviewedAt(e.getReviewedAt()).reviewedBy(e.getReviewedBy())
                .rejectionReason(e.getRejectionReason()).build();
    }
    private String trim(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private String uniqueAdminUsername(DepartmentAccessRequest request) {
        String prefix = request.getDepartmentCode().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return prefix + ".admin." + request.getId();
    }
    private String temporaryPassword() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder("Aa1!");
        for (int i = 0; i < 12; i++) password.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return password.toString();
    }
}
