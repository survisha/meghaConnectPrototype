package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.DepartmentDto;
import com.survisha.meghaconnect.dto.DepartmentRequest;
import com.survisha.meghaconnect.entity.Department;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<DepartmentDto> findAll() {
        return departmentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public DepartmentDto findById(Long id) {
        return toDto(getRequired(id));
    }

    @Transactional
    public DepartmentDto create(DepartmentRequest request, String actor) {
        String code = normalizeCode(request.getDepartmentCode());
        if (departmentRepository.existsByDepartmentCodeIgnoreCase(code)) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.DUPLICATE_ENTRY,
                    ErrorCodeConstants.format(ErrorCodeConstants.DUPLICATE_ENTRY_MSG, "departmentCode"),
                    409);
        }
        Department department = Department.builder()
                .departmentCode(code)
                .departmentName(trimToNull(request.getDepartmentName()))
                .description(trimToNull(request.getDescription()))
                .contactEmail(trimToNull(request.getContactEmail()))
                .contactMobile(trimToNull(request.getContactMobile()))
                .address(trimToNull(request.getAddress()))
                .status(request.getStatus() == null ? Department.DepartmentStatus.ACTIVE : request.getStatus())
                .build();
        department.setCreatedBy(actor);
        department.setUpdatedBy(actor);
        return toDto(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentDto update(Long id, DepartmentRequest request, String actor) {
        Department department = getRequired(id);
        String code = normalizeCode(request.getDepartmentCode());
        departmentRepository.findByDepartmentCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new MeghaConnectException(
                            ErrorCodeConstants.DUPLICATE_ENTRY,
                            ErrorCodeConstants.format(ErrorCodeConstants.DUPLICATE_ENTRY_MSG, "departmentCode"),
                            409);
                });
        department.setDepartmentCode(code);
        department.setDepartmentName(trimToNull(request.getDepartmentName()));
        department.setDescription(trimToNull(request.getDescription()));
        department.setContactEmail(trimToNull(request.getContactEmail()));
        department.setContactMobile(trimToNull(request.getContactMobile()));
        department.setAddress(trimToNull(request.getAddress()));
        department.setStatus(request.getStatus() == null ? department.getStatus() : request.getStatus());
        department.setUpdatedBy(actor);
        return toDto(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentDto setStatus(Long id, Department.DepartmentStatus status, String actor) {
        Department department = getRequired(id);
        department.setStatus(status);
        department.setUpdatedBy(actor);
        return toDto(departmentRepository.save(department));
    }

    public Department getRequired(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new MeghaConnectException(
                        ErrorCodeConstants.CONTENT_NOT_FOUND,
                        "Department not found: " + id,
                        404));
    }

    public DepartmentDto toDto(Department department) {
        if (department == null) {
            return null;
        }
        return DepartmentDto.builder()
                .id(department.getId())
                .departmentCode(department.getDepartmentCode())
                .departmentName(department.getDepartmentName())
                .description(department.getDescription())
                .contactEmail(department.getContactEmail())
                .contactMobile(department.getContactMobile())
                .address(department.getAddress())
                .status(department.getStatus())
                .createdBy(department.getCreatedBy())
                .createdAt(department.getCreatedAt())
                .updatedBy(department.getUpdatedBy())
                .updatedAt(department.getUpdatedAt())
                .build();
    }

    private String normalizeCode(String value) {
        String code = trimToNull(value);
        if (code == null) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "departmentCode"),
                    400);
        }
        return code.toUpperCase();
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
