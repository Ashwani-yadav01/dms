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
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID incidentId; // Linked incident from Incident Service

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private RescueDepartment department;

    @Column(nullable = false)
    private UUID assignedLeaderId; // Station Chief / Leader User ID from User Service

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionStatus status = MissionStatus.DISPATCHED;

    @Column(nullable = false)
    private Integer slaMinutes = 120; // Time threshold in minutes before escalation

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