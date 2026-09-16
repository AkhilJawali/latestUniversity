package com.utms.masterdata.faculty.availability;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FacultyPreferenceDto {
    private Long id;
    private Long facultyId;
    private String preferredTimeOfDay;
    private String sessionDistribution;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
