package com.survisha.meghaconnect.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "walkins",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_walkin_token_date", columnNames = {"token_date", "token_number"}),
                @UniqueConstraint(name = "uq_walkin_appointment", columnNames = "appointment_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalkIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "walkin_id")
    private Long walkinId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "token_number", nullable = false, length = 40)
    private String tokenNumber;

    @Column(name = "token_date", nullable = false)
    private LocalDate tokenDate;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 20)
    private String mobile;

    @Column(name = "id_type", length = 20)
    private String idType;

    @Column(name = "agenda_type", length = 200)
    private String agendaType;

    @Column(name = "brief_description", columnDefinition = "TEXT")
    private String briefDescription;

    @Column(name = "created_by_deo_id", length = 100)
    private String createdByDeoId;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
