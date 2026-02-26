package gov.meghalaya.meghaconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "directions",
    indexes = { @Index(name = "idx_dir_appt", columnList = "appointment_id") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Direction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirectionColor color;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String directionText;

    @Column(length = 200)
    private String assignedDepartment;

    @Column(length = 200)
    private String assignedOfficer;

    private LocalDate deadline;

    @Column(columnDefinition = "TEXT")
    private String currentStatus;

    private boolean completed = false;

    public enum DirectionColor { GREEN, YELLOW, BLUE }
}
