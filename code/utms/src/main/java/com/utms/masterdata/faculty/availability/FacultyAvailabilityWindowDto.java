package com.utms.masterdata.faculty.availability;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class FacultyAvailabilityWindowDto {
    private Long id;
    private Long facultyId;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reasonCode;
    private String reasonNote;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
