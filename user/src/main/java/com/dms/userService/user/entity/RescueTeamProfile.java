package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "rescue_team_profiles", uniqueConstraints = @UniqueConstraint(name = "uk_rescue_team_code", columnNames = "team_code"))
@PrimaryKeyJoinColumn(name = "user_id")
@Getter @Setter @NoArgsConstructor
public class RescueTeamProfile extends UserProfile {
    @Column(name = "team_name", nullable = false) private String teamName;
    @Column(name = "team_code", nullable = false, unique = true) private String teamCode;
    @Column(name = "department_id") private UUID departmentId;
    @Column(name = "team_lead_name", nullable = false) private String teamLeadName;
    @Column(name = "team_size", nullable = false) private Integer teamSize;
    @Column(name = "is_verified", nullable = false) private Boolean isVerified = false;
}
