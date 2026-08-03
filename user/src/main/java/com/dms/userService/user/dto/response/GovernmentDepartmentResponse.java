package com.dms.userService.user.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentDepartmentResponse {

    private UUID id;
    private String departmentName;
    private String description;
}