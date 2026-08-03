package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.geo.Point;

@Getter
@Setter
@MappedSuperclass
public abstract class UserProfile {

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