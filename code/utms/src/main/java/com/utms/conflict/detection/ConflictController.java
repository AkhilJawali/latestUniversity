package com.utms.conflict.detection;

import com.utms.scheduling.conflict.ConflictDto;
import com.utms.scheduling.conflict.ProposedPlacementRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint for real-time conflict detection (A4-16, design FR-1/FR-2).
 *
 * <p>Provides a REST fallback for the conflict check, complementing the WebSocket
 * channel (A4-15). Both REST and WebSocket delegate to the same
 * {@link ConflictDetectionService} — one code path.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/drafts/{draftId}/conflict-check — single placement check (FR-1)</li>
 *   <li>GET /api/v1/drafts/{draftId}/conflicts — full-draft check (FR-2)</li>
 * </ul>
 *
 * <p>Success response: {@code { "data": [ ConflictDto ] }} (empty array = no conflict).
 * <p>Error response: uses existing {@code GlobalExceptionHandler} shape
 * (status, error, message, path, details) — 400 for malformed/nonexistent draft,
 * no stack traces (AC8).
 */
@RestController
@RequestMapping("/api/v1/drafts")
@RequiredArgsConstructor
@Slf4j
public class ConflictController {

    private final ConflictDetectionService conflictDetectionService;

    /**
     * Check a single proposed placement for conflicts (FR-1).
     *
     * @param draftId the timetable draft ID
     * @param request the proposed placement
     * @return list of conflicts (empty if valid)
     */
    @PostMapping("/{draftId}/conflict-check")
    public ResponseEntity<ConflictCheckResponse> checkPlacement(
            @PathVariable Long draftId,
            @Valid @RequestBody ProposedPlacementRequest request) {
        
        log.debug("REST conflict check for draft {}, session {}", draftId, request.getSessionId());
        
        List<ConflictDto> conflicts = conflictDetectionService.checkPlacement(draftId, request);
        
        return ResponseEntity.ok(new ConflictCheckResponse(conflicts));
    }

    /**
     * Check all placed sessions in a draft for conflicts (FR-2).
     *
     * @param draftId the timetable draft ID
     * @return list of all conflicts found
     */
    @GetMapping("/{draftId}/conflicts")
    public ResponseEntity<ConflictCheckResponse> checkDraft(@PathVariable Long draftId) {
        log.debug("REST full-draft conflict check for draft {}", draftId);
        
        List<ConflictDto> conflicts = conflictDetectionService.checkDraft(draftId);
        
        return ResponseEntity.ok(new ConflictCheckResponse(conflicts));
    }

    /**
     * Response envelope for conflict check results.
     * Matches the existing API response pattern with a "data" field.
     */
    public record ConflictCheckResponse(List<ConflictDto> data) {
    }
}
