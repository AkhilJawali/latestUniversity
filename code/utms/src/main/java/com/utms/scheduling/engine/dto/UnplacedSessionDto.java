package com.utms.scheduling.engine.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO for unplaced sessions returned by GET /{draftId}/unplaced.
 */
@Getter
@Builder
public class UnplacedSessionDto {
    private final Long id;
    private final Long draftId;
    private final Long courseId;
    private final String courseCode;
    private final String courseName;
    private final Long facultyId;
    private final String facultyName;
    private final Long batchId;
    private final String batchName;
    private final String sessionType;
    private final Integer requiredDurationMinutes;
    private final String reason;
}
