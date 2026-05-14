package com.survisha.meghaconnect.entity;

import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reference_type")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "type", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReferenceData> referenceData;

    public enum Status {
        ACTIVE, INACTIVE
    }

    @PrePersist
    protected void onCreate() {
        createdAt = DateTimeUtil.nowIST();
        updatedAt = DateTimeUtil.nowIST();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = DateTimeUtil.nowIST();
    }
}
