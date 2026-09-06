package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "citizen_profiles")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CitizenProfile extends UserProfile {

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    private LocalDate dateOfBirth;

    private String occupation;

    private String bloodGroup; // e.g. "O_POSITIVE", "A_NEGATIVE"

    private String emergencyContactNumber;
}