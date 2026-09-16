package com.utms.masterdata.faculty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FacultyDto {
    private Long id;
    private String name;
    private String identifier;
    private String designation;
    private String qualification;
    private Long homeDepartmentId;
    private String homeDepartmentName;
    private BigDecimal minWeeklyLoad;
    private BigDecimal maxWeeklyLoad;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
