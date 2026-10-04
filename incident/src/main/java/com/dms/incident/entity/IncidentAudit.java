package com.dms.incident.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "incident_audit_log", indexes = @Index(name = "idx_incident_audit_incident", columnList = "incident_id"))
@Getter @Setter @NoArgsConstructor
public class IncidentAudit {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "incident_id", nullable = false) private UUID incidentId;
    @Column(name = "performed_by") private UUID performedBy;
    @Column(name = "actor_type", nullable = false, length = 16) private String actorType = "USER";
    @Column(nullable = false, length = 32) private String action;
    @Column(name = "previous_status", length = 20) private String previousStatus;
    @Column(name = "new_status", length = 20) private String newStatus;
    @Column(nullable = false) private Instant occurredAt = Instant.now();
    public IncidentAudit(UUID incidentId, UUID performedBy, String action, String previousStatus, String newStatus) {
        this.incidentId = incidentId; this.performedBy = performedBy; this.actorType = performedBy == null ? "SYSTEM" : "USER"; this.action = action;
        this.previousStatus = previousStatus; this.newStatus = newStatus;
    }
}
