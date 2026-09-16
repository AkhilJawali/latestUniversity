package com.utms.scheduling.engine.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduledSessionDto {
    private final Long id;
    private final Long courseId;
    private final Long facultyId;
    private final Long batchId;
    private final Long sectionId;
    private final Long roomId;
    private final String dayOfWeek;
    private final Long slotDefinitionId;
    private final String sessionType;
    private final Boolean isLocked;
}
