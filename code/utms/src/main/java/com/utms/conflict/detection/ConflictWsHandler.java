package com.utms.conflict.detection;

import com.utms.scheduling.conflict.ConflictDto;
import com.utms.scheduling.conflict.ProposedPlacementRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * WebSocket/STOMP handler for real-time conflict detection (A4-16, design FR-6, KD-65).
 *
 * <p>Provides an interactive channel for the drag-and-drop editor (A4-15) to check
 * placements in real-time. Delegates to the same {@link ConflictDetectionService}
 * as the REST endpoints — one code path.
 *
 * <p>STOMP endpoints:
 * <ul>
 *   <li>SUBSCRIBE /topic/drafts/{draftId}/conflicts — receive conflict updates</li>
 *   <li>SEND /app/drafts/{draftId}/conflict-check — request a placement check</li>
 * </ul>
 *
 * <p>The handler receives a {@link ProposedPlacementRequest} via STOMP MESSAGE,
 * runs the conflict check, and broadcasts the result to all subscribers on the
 * draft's topic.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ConflictWsHandler {

    private final ConflictDetectionService conflictDetectionService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle a single placement conflict check request over WebSocket (FR-6).
     *
     * <p>Client sends: SEND /app/drafts/{draftId}/conflict-check
     * Server broadcasts to: /topic/drafts/{draftId}/conflicts
     *
     * @param draftId the timetable draft ID
     * @param request the proposed placement
     */
    @MessageMapping("/drafts/{draftId}/conflict-check")
    public void checkPlacement(
            @DestinationVariable Long draftId,
            @Payload ProposedPlacementRequest request) {
        
        log.debug("WebSocket conflict check for draft {}, session {}", 
            draftId, request.getSessionId());
        
        // Run conflict check (same code path as REST)
        List<ConflictDto> conflicts = conflictDetectionService.checkPlacement(draftId, request);
        
        // Broadcast result to all subscribers on this draft's topic
        ConflictCheckResponse response = new ConflictCheckResponse(conflicts);
        messagingTemplate.convertAndSend(
            "/topic/drafts/" + draftId + "/conflicts", 
            response
        );
        
        log.debug("Broadcast conflict check result: {} conflicts for draft {}", 
            conflicts.size(), draftId);
    }

    /**
     * Handle subscription to conflict updates for a draft.
     *
     * <p>Client subscribes: SUBSCRIBE /topic/drafts/{draftId}/conflicts
     * Optionally, server can send initial state on subscribe.
     *
     * @param draftId the timetable draft ID
     * @return initial conflict state (empty for now, can be enhanced)
     */
    @SubscribeMapping("/drafts/{draftId}/conflicts")
    public ConflictCheckResponse onSubscribe(@DestinationVariable Long draftId) {
        log.debug("Client subscribed to conflict updates for draft {}", draftId);
        // Return empty state on subscribe; actual conflicts are pushed on check requests
        return new ConflictCheckResponse(List.of());
    }

    /**
     * Response envelope for conflict check results.
     * Matches the REST response format for consistency.
     */
    public record ConflictCheckResponse(List<ConflictDto> data) {
    }
}
