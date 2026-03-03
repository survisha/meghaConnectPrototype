package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.Appointment;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduleEventDto {
    private Long id;
    private String title;
    private Appointment.EventType eventType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Appointment.MeetingLocation location;
    private Integer travelTimeMinutes;
    private String description;
    private boolean isConflict;
}
