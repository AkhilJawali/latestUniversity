package com.utms.scheduling.conflict;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for conflict detection.
 * 
 * KD-65: REST fallback for the same checks (keeps the check testable and usable without a live socket).
 * 
 * Design: A4-16  6 ConflictController
 */
@RestController
@RequestMapping("/api/v1/drafts")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class ConflictController {

    private final ConflictDetectionService detectionService;

    /**
     * Checks a proposed placement for conflicts.
     * POST /api/v1/drafts/{draftId}/conflict-check
     * 
     * FR-1: Check a proposed placement against the draft's current occupancy.
     */
    @PostMapping("/{draftId}/conflict-check")
    public ResponseEntity<List<ConflictDto>> checkPlacement(
            @PathVariable Long draftId,
            @Valid @RequestBody ProposedPlacementRequest request) {
        
        log.info("Checking placement for draft {}", draftId);
        
        // Ensure draftId in path matches request
        request.setDraftId(draftId);
        
        List<ConflictDto> conflicts = detectionService.checkPlacement(request);
        return ResponseEntity.ok(conflicts);
    }

    /**
     * Checks an entire draft for conflicts.
     * GET /api/v1/drafts/{draftId}/conflicts
     * 
     * FR-2: Full-draft conflict list.
     */
    @GetMapping("/{draftId}/conflicts")
    public ResponseEntity<List<ConflictDto>> checkDraft(@PathVariable Long draftId) {
        log.info("Checking all conflicts for draft {}", draftId);
        
        List<ConflictDto> conflicts = detectionService.checkDraft(draftId);
        return ResponseEntity.ok(conflicts);
    }
}