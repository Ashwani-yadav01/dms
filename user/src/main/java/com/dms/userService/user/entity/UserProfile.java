package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.geo.Point;

import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class UserProfile {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String name;
    private String addressLine;
    private String city;
    private String state;
    private String district;
    private String pincode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;
    private String profilePhotoUrl;
}