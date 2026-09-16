package com.utms.masterdata.program;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpdateProgramRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Min(value = 1, message = "Duration must be at least 1 semester")
    private Integer durationSemesters;

    @Size(max = 50, message = "Degree type must not exceed 50 characters")
    private String degreeType;
}
