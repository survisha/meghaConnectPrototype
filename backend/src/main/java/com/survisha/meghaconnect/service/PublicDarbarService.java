package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.PublicDarbarRequest;
import com.survisha.meghaconnect.dto.PublicDarbarResponse;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.PublicDarbar;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.PublicDarbarRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PublicDarbarService {

    private final PublicDarbarRepository publicDarbarRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public PublicDarbarResponse create(PublicDarbarRequest request, String actor) {
        validateDarbarDate(request.getDarbarDate());
        validateMaxSlots(request.getMaxSlots());
        String location = normalizeLocation(request.getLocation());

        if (hasOpenDarbarForDateAndLocation(request.getDarbarDate(), location, null)) {
            throw workflowException(
                    ErrorCodeConstants.APPT_INVALID_STATUS,
                    "Public Darbar date already exists for this date and location",
                    HttpStatus.CONFLICT
            );
        }

        PublicDarbar darbar = PublicDarbar.builder()
                .darbarDate(request.getDarbarDate())
                .location(location)
                .maxSlots(request.getMaxSlots())
                .status(PublicDarbar.DarbarStatus.CREATED)
                .remarks(RequestContextUtil.sanitizeForLog(request.getRemarks()))
                .build();
        darbar.setCreatedBy(actor);
        PublicDarbar saved = publicDarbarRepository.save(darbar);
        log.info("Public Darbar created publicDarbarId={} darbarDate={} location={}",
                saved.getId(), saved.getDarbarDate(), saved.getLocation());
        return toResponse(saved);
    }

    @Transactional
    public PublicDarbarResponse activate(Long id, String actor) {
        PublicDarbar darbar = findDarbar(id);
        validateDarbarDate(darbar.getDarbarDate());
        ensureNotCancelled(darbar);

        if (hasOpenDarbarForDateAndLocation(darbar.getDarbarDate(), darbar.getLocation(), darbar.getId())) {
            throw workflowException(
                    ErrorCodeConstants.APPT_INVALID_STATUS,
                    "Another active Public Darbar already exists for this date and location",
                    HttpStatus.CONFLICT
            );
        }

        darbar.setStatus(PublicDarbar.DarbarStatus.ACTIVE);
        darbar.setActivatedBy(actor);
        darbar.setActivatedAt(DateTimeUtil.nowIST());
        darbar.setUpdatedBy(actor);
        PublicDarbar saved = publicDarbarRepository.save(darbar);
        log.info("Public Darbar activated publicDarbarId={} darbarDate={} location={}",
                saved.getId(), saved.getDarbarDate(), saved.getLocation());
        return toResponse(saved);
    }

    @Transactional
    public PublicDarbarResponse reschedule(Long id, PublicDarbarRequest request, String actor) {
        PublicDarbar darbar = findDarbar(id);
        ensureNoCompletedAppointments(id);
        validateDarbarDate(request.getDarbarDate());
        validateMaxSlots(request.getMaxSlots());
        String location = normalizeLocation(request.getLocation());

        if (hasOpenDarbarForDateAndLocation(request.getDarbarDate(), location, id)) {
            throw workflowException(
                    ErrorCodeConstants.APPT_INVALID_STATUS,
                    "Public Darbar date already exists for this date and location",
                    HttpStatus.CONFLICT
            );
        }

        darbar.setDarbarDate(request.getDarbarDate());
        darbar.setLocation(location);
        darbar.setMaxSlots(request.getMaxSlots());
        darbar.setRemarks(RequestContextUtil.sanitizeForLog(request.getRemarks()));
        darbar.setUpdatedBy(actor);
        PublicDarbar saved = publicDarbarRepository.save(darbar);
        log.info("Public Darbar rescheduled publicDarbarId={} darbarDate={} location={}",
                saved.getId(), saved.getDarbarDate(), saved.getLocation());
        return toResponse(saved);
    }

    @Transactional
    public PublicDarbarResponse cancel(Long id, String actor, String remarks) {
        PublicDarbar darbar = findDarbar(id);
        ensureNoCompletedAppointments(id);
        darbar.setStatus(PublicDarbar.DarbarStatus.CANCELLED);
        darbar.setRemarks(RequestContextUtil.sanitizeForLog(remarks));
        darbar.setUpdatedBy(actor);
        PublicDarbar saved = publicDarbarRepository.save(darbar);
        log.info("Public Darbar cancelled publicDarbarId={}", saved.getId());
        return toResponse(saved);
    }

    public PublicDarbar findActiveDarbar(Long id) {
        PublicDarbar darbar = findDarbar(id);
        if (darbar.getStatus() != PublicDarbar.DarbarStatus.ACTIVE) {
            throw workflowException(
                    ErrorCodeConstants.APPT_INVALID_STATUS,
                    "Public Darbar date must be active before scheduling",
                    HttpStatus.CONFLICT
            );
        }
        return darbar;
    }

    public List<PublicDarbarResponse> findActiveDarbars() {
        return publicDarbarRepository.findByStatusOrderByDarbarDateAsc(PublicDarbar.DarbarStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PublicDarbar findDarbar(Long id) {
        return publicDarbarRepository.findById(id)
                .orElseThrow(() -> workflowException(
                        ErrorCodeConstants.PUBLIC_DARBAR_NOT_FOUND,
                        ErrorCodeConstants.PUBLIC_DARBAR_NOT_FOUND_MSG,
                        HttpStatus.NOT_FOUND
                ));
    }

    private void validateDarbarDate(LocalDate darbarDate) {
        if (darbarDate == null || darbarDate.isBefore(DateTimeUtil.currentDateIST())) {
            throw workflowException(
                    ErrorCodeConstants.APPT_INVALID_SCHEDULE_DATE_TIME,
                    "Public Darbar date must be today or a future date",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateMaxSlots(Integer maxSlots) {
        if (maxSlots == null || maxSlots < 1) {
            throw workflowException(
                    ErrorCodeConstants.APPT_INVALID_SCHEDULE_DATE_TIME,
                    "maxSlots must be at least 1",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void ensureNotCancelled(PublicDarbar darbar) {
        if (darbar.getStatus() == PublicDarbar.DarbarStatus.CANCELLED) {
            throw workflowException(
                    ErrorCodeConstants.APPT_INVALID_STATUS,
                    "Cancelled Public Darbar date cannot be changed",
                    HttpStatus.CONFLICT
            );
        }
    }

    private void ensureNoCompletedAppointments(Long publicDarbarId) {
        boolean hasCompleted = appointmentRepository.existsByPublicDarbar_IdAndStatusIn(
                publicDarbarId,
                Collections.singleton(Appointment.AppointmentStatus.COMPLETED)
        );
        if (hasCompleted) {
            throw workflowException(
                    ErrorCodeConstants.APPT_INVALID_STATUS,
                    "Public Darbar date has completed appointments and cannot be changed",
                    HttpStatus.CONFLICT
            );
        }
    }

    private boolean hasOpenDarbarForDateAndLocation(LocalDate darbarDate, String location, Long currentId) {
        List<PublicDarbar> matches = publicDarbarRepository.findByDarbarDateAndLocationAndStatusIn(
                darbarDate,
                location,
                Arrays.asList(PublicDarbar.DarbarStatus.CREATED, PublicDarbar.DarbarStatus.ACTIVE)
        );
        return matches.stream().anyMatch(match -> !match.getId().equals(currentId));
    }

    private String normalizeLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw workflowException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "location"),
                    HttpStatus.BAD_REQUEST
            );
        }
        return location.trim();
    }

    private PublicDarbarResponse toResponse(PublicDarbar darbar) {
        return PublicDarbarResponse.builder()
                .id(darbar.getId())
                .darbarDate(darbar.getDarbarDate())
                .location(darbar.getLocation())
                .maxSlots(darbar.getMaxSlots())
                .status(darbar.getStatus())
                .activatedBy(darbar.getActivatedBy())
                .activatedAt(darbar.getActivatedAt())
                .remarks(darbar.getRemarks())
                .createdAt(darbar.getCreatedAt())
                .updatedAt(darbar.getUpdatedAt())
                .build();
    }

    private MeghaConnectException workflowException(String code, String message, HttpStatus status) {
        return new MeghaConnectException(code, message, status.value());
    }
}
