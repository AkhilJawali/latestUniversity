package com.utms.scheduling.engine.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response for lock/unlock operations on a scheduled session (A4-14, FR-1).
 */
@Getter
@Builder
public class SessionLockDto {
    private final Long sessionId;
    private final Long draftId;
    private final Boolean isLocked;
}
