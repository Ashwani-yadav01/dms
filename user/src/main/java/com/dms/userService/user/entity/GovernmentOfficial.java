package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GovernmentOfficial {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @MapsId
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private GovernmentDepartment department;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false, unique = true)
    private String employeeId;

    private String officialPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HierarchyLevel hierarchyLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to")
    private GovernmentOfficial reportsTo;

    @OneToMany(mappedBy = "reportsTo", fetch = FetchType.LAZY)
    private List<GovernmentOfficial> subordinates;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}