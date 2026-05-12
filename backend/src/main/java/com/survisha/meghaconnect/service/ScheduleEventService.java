package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.AppointmentDto;
import com.survisha.meghaconnect.dto.ScheduleEventDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.ScheduleEvent;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.ScheduleEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleEventService {

    private final ScheduleEventRepository scheduleEventRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;

    public List<ScheduleEvent> findAll() {
        return scheduleEventRepository.findAll();
    }

    public Optional<ScheduleEvent> findById(Long id) {
        return scheduleEventRepository.findById(id);
    }

    public List<ScheduleEventDto> findAllDtos(LocalDateTime start, LocalDateTime end) {
        List<ScheduleEvent> scheduleEvents = start != null && end != null
            ? scheduleEventRepository.findByRangeWithAppointments(start, end)
            : scheduleEventRepository.findAllWithAppointments();

        List<ScheduleEventDto> results = new ArrayList<>();
        Set<Long> appointmentIdsFromScheduleEvents = new HashSet<>();

        for (ScheduleEvent event : scheduleEvents) {
            ScheduleEventDto dto = toDto(event);
            if (dto.getAppointments() != null) {
                dto.getAppointments().stream()
                    .map(AppointmentDto::getId)
                    .filter(id -> id != null)
                    .forEach(appointmentIdsFromScheduleEvents::add);
            }
            results.add(dto);
        }

        List<Appointment> scheduledAppointments = start != null && end != null
            ? appointmentRepository.findScheduledWithApplicantInRange(start, end)
            : appointmentRepository.findScheduledWithApplicant();

        scheduledAppointments.stream()
            .filter(appointment -> appointment.getId() != null)
            .filter(appointment -> !appointmentIdsFromScheduleEvents.contains(appointment.getId()))
            .map(this::toAppointmentScheduleEventDto)
            .forEach(results::add);

        results.sort(Comparator.comparing(ScheduleEventDto::getStartTime));
        return results;
    }

    public Optional<ScheduleEventDto> findDtoById(Long id) {
        return scheduleEventRepository.findById(id).map(this::toDto);
    }

    @Transactional
    public ScheduleEvent create(ScheduleEvent event) {
        // Check for conflicts
        boolean conflict = scheduleEventRepository.findAll().stream()
            .anyMatch(e -> !e.getId().equals(event.getId()) &&
                e.getStartTime().isBefore(event.getEndTime()) &&
                event.getStartTime().isBefore(e.getEndTime()));
        event.setConflict(conflict);
        return scheduleEventRepository.save(event);
    }

    @Transactional
    public ScheduleEvent update(ScheduleEvent event) {
        return scheduleEventRepository.save(event);
    }

    @Transactional
    public void delete(Long id) {
        scheduleEventRepository.deleteById(id);
    }

    public ScheduleEventDto toDto(ScheduleEvent event) {
        if (event == null) {
            return null;
        }

        List<AppointmentDto> appointments = event.getAppointments() == null
            ? List.of()
            : event.getAppointments().stream()
                .map(appointmentService::toDto)
                .toList();

        AppointmentDto appointment = appointments.isEmpty() ? null : appointments.get(0);

        return ScheduleEventDto.builder()
            .id(event.getId())
            .title(event.getTitle())
            .eventType(event.getEventType())
            .startTime(event.getStartTime())
            .endTime(event.getEndTime())
            .location(event.getLocation())
            .travelTimeMinutes(event.getTravelTimeMinutes())
            .description(event.getDescription())
            .shortNotes(event.getShortNotes())
            .isConflict(event.isConflict())
            .sourceType("SCHEDULE_EVENT")
            .sourceId(event.getId())
            .appointmentId(appointment != null ? appointment.getId() : null)
            .appointment(appointment)
            .appointments(appointments)
            .appointmentCount(appointments.size())
            .build();
    }

    private ScheduleEventDto toAppointmentScheduleEventDto(Appointment appointment) {
        AppointmentDto appointmentDto = appointmentService.toDto(appointment);
        LocalDateTime start = appointment.getScheduledDateTime();
        int durationMinutes = appointment.getScheduledDurationMinutes() != null
            ? appointment.getScheduledDurationMinutes()
            : 30;

        return ScheduleEventDto.builder()
            .id(appointment.getId() != null ? -appointment.getId() : null)
            .title(buildAppointmentTitle(appointment))
            .eventType(appointment.getEventType())
            .startTime(start)
            .endTime(start != null ? start.plusMinutes(durationMinutes) : null)
            .location(appointment.getRequestedLocation())
            .description(firstNonBlank(appointment.getAgendaBrief(), appointment.getAgendaType()))
            .shortNotes(firstNonBlank(appointment.getShortNotes(), appointment.getAiSummary(),
                appointment.getHcmRemarks(), appointment.getApproverRemarks(), appointment.getCmoRemarks()))
            .isConflict(false)
            .sourceType("APPOINTMENT")
            .sourceId(appointment.getId())
            .appointmentId(appointment.getId())
            .appointment(appointmentDto)
            .appointments(List.of(appointmentDto))
            .appointmentCount(1)
            .build();
    }

    private String buildAppointmentTitle(Appointment appointment) {
        String subject = firstNonBlank(appointment.getSubject(), appointment.getAgendaType());
        String applicantName = appointment.getApplicant() != null
            ? appointment.getApplicant().getFullName()
            : null;
        if (applicantName == null || applicantName.isBlank()) {
            return firstNonBlank(subject, appointment.getApplicationId(), "Appointment");
        }
        return firstNonBlank(subject, "Appointment") + " - " + applicantName;
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
}
