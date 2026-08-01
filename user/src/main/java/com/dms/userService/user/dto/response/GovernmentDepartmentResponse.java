package com.dms.userService.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentDepartmentResponse {

    private UUID id;

    private String departmentName;

    private String description;
}