package gov.meghalaya.meghaconnect.dto;

import gov.meghalaya.meghaconnect.entity.Appointment;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentDto {
    private Long id;
    private String applicationId;
    private Long applicantId;
    private String applicantName;
    private String applicantPhone;
    private Appointment.EventType eventType;
    private String agendaType;
    private String agendaBrief;
    private Appointment.AppointmentStatus status;
    private Appointment.MeetingLocation requestedLocation;
    private LocalDateTime scheduledDateTime;
    private Integer scheduledDurationMinutes;
    private Boolean mlaMdcApproved;
    private String cmoRemarks;
    private String hcmRemarks;
    private Boolean isWalkIn;
    private Integer meetingCountLast6Months;
}
