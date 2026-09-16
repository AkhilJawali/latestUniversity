package com.utms.masterdata.program;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateProgramRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Code must be alphanumeric (hyphens and underscores allowed)")
    private String code;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Duration in semesters is required")
    @Min(value = 1, message = "Duration must be at least 1 semester")
    private Integer durationSemesters;

    @NotBlank(message = "Degree type is required")
    @Size(max = 50, message = "Degree type must not exceed 50 characters")
    private String degreeType;
}
