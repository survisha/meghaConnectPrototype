package gov.meghalaya.meghaconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "persons",
    indexes = {
        @Index(name = "idx_person_phone", columnList = "phoneNumber"),
        @Index(name = "idx_person_epic",  columnList = "epicNumber"),
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Person extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 50)
    private String epicNumber;

    @Column(length = 200)
    private String photoPath;

    @Column(length = 100)
    private String designation;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String constituency;

    @Column(length = 100)
    private String booth;

    @Column(length = 100)
    private String village;

    @Column(columnDefinition = "TEXT")
    private String briefProfile;

    private LocalDate dateOfBirth;

    @Column(length = 500)
    private String address;

    // Facial recognition embedding reference
    @Column(length = 500)
    private String faceEmbeddingRef;

    @OneToMany(mappedBy = "applicant", fetch = FetchType.LAZY)
    private List<Appointment> appointments;
}
