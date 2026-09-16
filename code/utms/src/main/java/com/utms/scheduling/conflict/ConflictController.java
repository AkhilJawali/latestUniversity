package com.utms.scheduling.conflict;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for real-time conflict detection (A4-16, FR-1/FR-2, KD-65).
 *
 * <p>REST fallback for the same checks exposed over WebSocket ({@link ConflictWsHandler}).
 * Both delegate to {@link ConflictDetectionService} so there is a single code path.
 * A nonexistent draft yields 404 via the global handler; a malformed body yields 400
 * (Jakarta validation), never leaking internals.
 *
 * <p>Response shape follows the existing scheduling controllers (DTO/list returned
 * directly), not the {@code {data:[...]}} envelope illustrated in the design — kept
 * consistent with SchedulingController and the rest of the module.
 */
@RestController
@RequestMapping("/api/v1/drafts/{draftId}")
@RequiredArgsConstructor
public class ConflictController {

    private final ConflictDetectionService conflictDetectionService;

    /** FR-1: check a single proposed placement. */
    @PostMapping("/conflict-check")
    public ResponseEntity<List<ConflictDto>> checkPlacement(
            @PathVariable Long draftId,
            @Valid @RequestBody ProposedPlacementRequest request) {
        return ResponseEntity.ok(conflictDetectionService.checkPlacement(draftId, request));
    }

    /** FR-2: full-draft conflict list. */
    @GetMapping("/conflicts")
    public ResponseEntity<List<ConflictDto>> checkDraft(@PathVariable Long draftId) {
        return ResponseEntity.ok(conflictDetectionService.checkDraft(draftId));
    }
}
