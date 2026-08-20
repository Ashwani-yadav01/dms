package com.dms.rescueService.rescue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_rescue_missions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescueMission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Standardize UUID generation
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private RescueDepartment department;

    @Column(name = "assigned_leader_id", nullable = true) // Changed to nullable for auto-dispatch
    private UUID assignedLeaderId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MissionStatus status = MissionStatus.DISPATCHED;

    @Builder.Default
    @Column(nullable = false)
    private Integer slaMinutes = 120;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isSlaBreached = false;

    private LocalDateTime dispatchedAt;
    private LocalDateTime completedAt;

    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onPersist() {
        if (this.dispatchedAt == null) {
            this.dispatchedAt = LocalDateTime.now();
        }
    }
}