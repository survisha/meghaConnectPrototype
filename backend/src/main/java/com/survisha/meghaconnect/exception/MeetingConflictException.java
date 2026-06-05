package com.survisha.meghaconnect.exception;

import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.ScheduleEvent;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class MeetingConflictException extends MeghaConnectException {

    private final Map<String, Object> details;

    public MeetingConflictException(LocalDateTime requestedStart,
                                    LocalDateTime requestedEnd,
                                    Appointment conflictingAppointment) {
        super(ErrorCodeConstants.MEETING_CONFLICT,
            ErrorCodeConstants.MEETING_CONFLICT_MSG,
            409);
        this.details = buildDetails(requestedStart, requestedEnd);
        if (conflictingAppointment != null) {
            details.put("conflictingAppointmentId", conflictingAppointment.getId());
            details.put("conflictingStart", conflictingAppointment.getScheduledDateTime());
            details.put("conflictingEnd", conflictingAppointment.getScheduledDateTime() != null
                ? conflictingAppointment.getScheduledDateTime().plusMinutes(
                    conflictingAppointment.getScheduledDurationMinutes() != null
                        ? conflictingAppointment.getScheduledDurationMinutes()
                        : 30)
                : null);
            details.put("conflictingApplicationId", conflictingAppointment.getApplicationId());
        }
    }

    public MeetingConflictException(LocalDateTime requestedStart,
                                    LocalDateTime requestedEnd,
                                    ScheduleEvent conflictingEvent) {
        super(ErrorCodeConstants.MEETING_CONFLICT,
            ErrorCodeConstants.MEETING_CONFLICT_MSG,
            409);
        this.details = buildDetails(requestedStart, requestedEnd);
        if (conflictingEvent != null) {
            details.put("conflictingScheduleEventId", conflictingEvent.getId());
            details.put("conflictingTitle", conflictingEvent.getTitle());
            details.put("conflictingStart", conflictingEvent.getStartTime());
            details.put("conflictingEnd", conflictingEvent.getEndTime());
        }
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    private Map<String, Object> buildDetails(LocalDateTime requestedStart, LocalDateTime requestedEnd) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("requestedStart", requestedStart);
        value.put("requestedEnd", requestedEnd);
        return value;
    }
}
