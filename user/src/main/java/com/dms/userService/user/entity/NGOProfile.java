package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ngo_profiles")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NGOProfile extends UserProfile {

    @Column(nullable = false)
    private String ngoName;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private String ownerName;

    private String website;

    @Column(length = 1000)
    private String description;
}