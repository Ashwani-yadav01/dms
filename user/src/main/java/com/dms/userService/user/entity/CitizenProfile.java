package com.dms.userService.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenProfile {
    @Id
    private UUID citizenId;
    @OneToOne
    private User user;
    private String gender;
    private String dateOfBirth;
    private String occupation;
}
