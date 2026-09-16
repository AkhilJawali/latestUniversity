package com.utms.masterdata.course;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class CourseDto {
    private Long id;
    private String name;
    private String code;
    private Long departmentId;
    private String departmentName;
    private Integer lectureHours;
    private Integer tutorialHours;
    private Integer practicalHours;
    private BigDecimal credits;
    private String courseType;
    private List<String> equipmentTags;
    private Boolean isCrossListed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
