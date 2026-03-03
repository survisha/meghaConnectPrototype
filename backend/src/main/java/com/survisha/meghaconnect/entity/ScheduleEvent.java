package com.survisha.meghaconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "schedule_events",
    indexes = { @Index(name = "idx_event_start", columnList = "startTime") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduleEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Appointment.EventType eventType;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Appointment.MeetingLocation location;

    private Integer travelTimeMinutes;

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean isConflict = false;

    @OneToMany(mappedBy = "scheduleEvent", fetch = FetchType.LAZY)
    private List<Appointment> appointments;
}
