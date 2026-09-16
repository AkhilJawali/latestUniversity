package com.utms.masterdata.academiccalendar;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class WorkingDayPatternDto {
    private Long id;
    private Long campusId;
    private PatternType patternType;
    private String workingSaturdays;
    private String customDefinition;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
