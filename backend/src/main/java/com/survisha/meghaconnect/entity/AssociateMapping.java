package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;

import com.survisha.meghaconnect.entity.Visitor;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "person_id", nullable = true)
    private Visitor person;

    @Column(length = 200)
    private String relationship;

    @Column(length = 200)
    private String associateName;

    @Column(length = 20)
    private String associatePhone;

    @Column(length = 50)
    private String associateEpic;

    @Column(length = 100)
    private String associateDesignation;

    @Column(length = 500)
    private String associateAddress;
}
