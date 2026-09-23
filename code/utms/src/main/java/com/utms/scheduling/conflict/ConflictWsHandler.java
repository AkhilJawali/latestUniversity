package com.utms.scheduling.conflict;

import java.util.List;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket handler for real-time conflict detection.
 * 
 * Clients subscribe to /topic/draft/{draftId}/conflicts to receive
 * conflict updates when placements are made.
 * 
 * KD-65: STOMP handler delegates to the same ConflictDetectionService as REST.
 * 
 * Design: A4-16 §6.3 ConflictWsHandler
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ConflictWsHandler {

    private final ConflictDetectionService detectionService;

    /**
     * Handles real-time conflict check requests via WebSocket.
     * 
     * Client sends to /app/drafts/{draftId}/conflict-check
     * Response is broadcast to /topic/draft/{draftId}/conflicts
     * 
     * @param draftId the draft ID from the destination path
     * @param request the placement request
     * @return list of conflicts (empty if valid)
     */
    @MessageMapping("/drafts/{draftId}/conflict-check")
    @SendTo("/topic/draft/{draftId}/conflicts")
    public List<ConflictDto> checkConflicts(
            @DestinationVariable Long draftId,
            ProposedPlacementRequest request) {
        
        log.debug("WebSocket conflict check for draft {}", draftId);
        
        // Ensure draftId from path is used
        request.setDraftId(draftId);
        
        return detectionService.checkPlacement(request);
    }
}

/**
 * Service for broadcasting conflict updates to subscribers.
 * 
 * Used by other components (e.g., session update handlers) to push
 * conflict notifications to connected clients.
 */
@Service
@RequiredArgsConstructor
class ConflictBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcasts a conflict update to all subscribers of a draft.
     */
    public void broadcastConflict(Long draftId, ConflictDto conflict) {
        messagingTemplate.convertAndSend(
                "/topic/draft/" + draftId + "/conflicts",
                conflict);
    }

    /**
     * Broadcasts a list of conflicts to all subscribers of a draft.
     */
    public void broadcastConflicts(Long draftId, List<ConflictDto> conflicts) {
        messagingTemplate.convertAndSend(
                "/topic/draft/" + draftId + "/conflicts",
                conflicts);
    }
}
