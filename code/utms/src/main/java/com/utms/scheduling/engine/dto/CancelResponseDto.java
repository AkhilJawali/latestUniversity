package com.utms.scheduling.engine.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response for POST /generate/{requestId}/cancel endpoint.
 */
@Getter
@Builder
public class CancelResponseDto {
    private final Long requestId;
    private final String status;
    private final String message;
    private final boolean cancellable;
}
