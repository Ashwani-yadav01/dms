package com.dms.userService.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tbl_safety_check_ins", indexes = {
        @Index(name = "idx_checkin_user", columnList = "user_id"),
        @Index(name = "idx_checkin_client_ref", columnList = "client_reference_id", unique = true),
        @Index(name = "idx_checkin_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "client_reference_id", nullable = false, unique = true, length = 100)
    private String clientReferenceId;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "SAFE";

    @Column(length = 500)
    private String message;

    private Double latitude;

    private Double longitude;

    @Builder.Default
    private boolean notifyContacts = false;

    @Builder.Default
    private int contactsNotifiedCount = 0;

    @Column(length = 30)
    @Builder.Default
    private String deliveryStatus = "NONE"; // NONE, SENT, PARTIAL, FAILED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
