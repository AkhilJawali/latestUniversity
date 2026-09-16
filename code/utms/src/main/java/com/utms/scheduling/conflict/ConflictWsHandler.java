package com.utms.scheduling.conflict;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * STOMP handler for real-time conflict checks (A4-16, FR-6, KD-65).
 *
 * <p>Client sends a {@link ProposedPlacementRequest} to
 * {@code /app/drafts/{draftId}/conflict-check}; the resulting conflict list is
 * published to {@code /topic/drafts/{draftId}/conflicts}. Delegates to the same
 * {@link ConflictDetectionService} as the REST controller — one code path.
 *
 * <p>Note: structured 400/404 error responses are provided by the REST endpoint
 * ({@link ConflictController} via the global handler). Error propagation over STOMP
 * is coarser; the REST path is the authoritative validated channel for malformed /
 * nonexistent-draft cases.
 */
@Controller
@RequiredArgsConstructor
public class ConflictWsHandler {

    private final ConflictDetectionService conflictDetectionService;

    @MessageMapping("/drafts/{draftId}/conflict-check")
    @SendTo("/topic/drafts/{draftId}/conflicts")
    public List<ConflictDto> checkPlacement(@DestinationVariable Long draftId,
                                            ProposedPlacementRequest request) {
        return conflictDetectionService.checkPlacement(draftId, request);
    }
}
