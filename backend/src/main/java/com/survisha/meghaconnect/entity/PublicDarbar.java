package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "public_darbar",
    indexes = {
        @Index(name = "idx_public_darbar_date", columnList = "darbarDate"),
        @Index(name = "idx_public_darbar_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicDarbar extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate darbarDate;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(nullable = false)
    private Integer maxSlots;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DarbarStatus status;

    @Column(length = 100)
    private String activatedBy;

    private LocalDateTime activatedAt;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @OneToMany(mappedBy = "publicDarbar", fetch = FetchType.LAZY)
    private List<Appointment> appointments;

    public enum DarbarStatus {
        CREATED, ACTIVE, SCHEDULED, CANCELLED, COMPLETED
    }
}
