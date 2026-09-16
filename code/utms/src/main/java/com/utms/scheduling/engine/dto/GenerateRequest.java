package com.utms.scheduling.engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateRequest {
    @NotNull(message = "departmentId is required")
    private Long departmentId;

    @NotBlank(message = "semester is required")
    private String semester;

    @NotBlank(message = "academicYear is required")
    private String academicYear;

    private Long seed;
}
