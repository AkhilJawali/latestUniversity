package com.utms.masterdata.course;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class UpdateCourseRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotNull(message = "Lecture hours is required")
    @Min(value = 0, message = "Lecture hours must be >= 0")
    private Integer lectureHours;

    @NotNull(message = "Tutorial hours is required")
    @Min(value = 0, message = "Tutorial hours must be >= 0")
    private Integer tutorialHours;

    @NotNull(message = "Practical hours is required")
    @Min(value = 0, message = "Practical hours must be >= 0")
    private Integer practicalHours;

    @NotNull(message = "Credits is required")
    @DecimalMin(value = "0.1", message = "Credits must be at least 0.1")
    private BigDecimal credits;

    @NotBlank(message = "Course type is required")
    @Pattern(regexp = "^(CORE|ELECTIVE|AUDIT)$", message = "Course type must be CORE, ELECTIVE, or AUDIT")
    private String courseType;

    private List<String> equipmentTags;
}
