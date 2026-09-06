package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "volunteer_profiles")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerProfile extends UserProfile {

    @ElementCollection
    @CollectionTable(name = "volunteer_skills", joinColumns = @JoinColumn(name = "volunteer_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    private Integer experienceInYears;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Availability availability = Availability.AVAILABLE;
}