package com.dms.rescueService.rescue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_rescue_departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescueDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepartmentType type;

    @Column(nullable = false)
    private String jurisdictionCode; // e.g. "DISTRICT_DEHRADUN"

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String contactPhone;

    @Builder.Default
    @Column(nullable = false)
    private Integer totalCapacity = 10;

    @Builder.Default
    @Column(nullable = false)
    private Integer activeMissionsCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isAvailable = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}