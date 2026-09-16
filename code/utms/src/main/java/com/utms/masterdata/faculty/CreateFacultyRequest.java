package com.utms.masterdata.faculty;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class CreateFacultyRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Identifier is required")
    @Size(max = 50, message = "Identifier must not exceed 50 characters")
    private String identifier;

    @NotBlank(message = "Designation is required")
    @Size(max = 50, message = "Designation must not exceed 50 characters")
    private String designation;

    @NotBlank(message = "Qualification is required")
    @Size(max = 500, message = "Qualification must not exceed 500 characters")
    private String qualification;

    @NotNull(message = "Home department ID is required")
    private Long homeDepartmentId;

    @DecimalMin(value = "0.0", message = "Minimum weekly load must be >= 0.0")
    private BigDecimal minWeeklyLoad;

    @DecimalMin(value = "0.1", message = "Maximum weekly load must be >= 0.1")
    private BigDecimal maxWeeklyLoad;

    @NotEmpty(message = "At least one campus association is required")
    private List<Long> campusIds;

    private List<Long> competencyCourseIds;
}
