package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "associate_mappings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssociateMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(length = 200)
    private String relationship;
}
