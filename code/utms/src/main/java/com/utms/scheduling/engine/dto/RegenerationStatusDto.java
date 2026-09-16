package com.utms.scheduling.engine.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 202 Accepted response for a partial re-generation trigger (A4-14, FR-3).
 */
@Getter
@Builder
public class RegenerationStatusDto {
    private final Long requestId;
    private final String status;
    private final Long sourceDraftId;
    private final List<Long> batchIds;
    private final List<Long> sectionIds;
    private final List<Long> courseIds;
    private final Integer fixedSessionCount;
    private final Integer regeneratingSessionCount;
    private final String statusUrl;
}
