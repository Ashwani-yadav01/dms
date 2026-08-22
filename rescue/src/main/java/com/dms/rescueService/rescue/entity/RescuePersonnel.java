package com.dms.rescueService.rescue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tbl_rescue_personnel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescuePersonnel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column(name = "badge_number", unique = true, nullable = false)
    private String badgeNumber;

    @Column(nullable = false)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private RescueDepartment department;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isChief = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isAvailable = true;
}