package com.utms.scheduling.engine.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GenerationStatusDto {
    private final Long requestId;
    private final String status;
    private final Integer progress;
    private final String phase;
    private final Long elapsedSeconds;
    private final Long departmentId;
    private final String semester;
    private final Long draftId;
    private final Double feasibilityScore;
    private final Double qualityScore;
    private final Integer totalSessions;
    private final Integer placedSessions;
    private final Integer violationCount;
    private final Integer bestSoFarCount;
    private final Boolean cancellable;
    private final String draftUrl;
    private final String statusUrl;
}
