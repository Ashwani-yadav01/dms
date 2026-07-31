package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentDepartment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String departmentName;

    private String description;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<GovernmentOfficial> officials;
}