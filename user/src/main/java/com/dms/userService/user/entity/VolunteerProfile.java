package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "volunteer_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "user_id")
public class VolunteerProfile extends UserProfile {

//    @Id
//    private UUID id;
//
//    @OneToOne(fetch = FetchType.LAZY, optional = false)
//    @MapsId
//    @JoinColumn(name = "user_id", nullable = false, unique = true)
//    private User user;

    @ElementCollection
    @CollectionTable(
            name = "volunteer_skills",
            joinColumns = @JoinColumn(name = "volunteer_id")
    )
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    private Integer experienceInYears;

    @Enumerated(EnumType.STRING)
    private Availability availability;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}