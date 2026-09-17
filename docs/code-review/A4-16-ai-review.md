# Real-Time Conflict Detection Engine — Package Reorganization

Package restructuring that moves the delivery layer (controller, WebSocket handler, service) from `com.utms.scheduling.conflict` to `com.utms.conflict.detection` while keeping shared types (DTOs, enums, interfaces) in the original location. The shared types receive enhanced Javadoc documentation.

**Verdict**: NEEDS_CHANGES

---

## High-Level View

The change separates delivery concerns from shared domain types. The delivery layer now lives in `com.utms.conflict.detection` and imports the shared types from `com.utms.scheduling.conflict`. This is a reasonable architecture: the delivery package depends on the scheduling module's conflict types, but not the reverse.

The shared types (`ConflictDto`, `ConflictType`, `DraftOccupancyIndex`, `FacultyLimitProvider`, `ProposedPlacementRequest`) remain in `com.utms.scheduling.conflict` with enhanced Javadoc. The documentation additions are thorough — each enum constant, field, and method now has proper Javadoc with `@param` tags.

The delivery layer implementation is functionally equivalent to the deleted code, but there's a critical omission: the draft existence validation (`requireDraft()`) that was present in the old `ConflictDetectionService` is missing from the new one. This is a regression that breaks AC8.

Test imports weren't updated, so the tests won't compile against the new code. The `PlacementRuleCheckerTest` instantiates the checker with a constructor signature that no longer exists (4 args vs 2 in the new implementation).

---

<details>
<summary>Issues (3)</summary>

1. **Missing draft existence validation** — The old `ConflictDetectionService` had a `requireDraft()` method that called `draftRepository.findByIdAndDeletedAtIsNull(draftId).orElseThrow(...)`. The new implementation in `com.utms.conflict.detection.ConflictDetectionService` has no such check — it calls `occupancyLoader.load(draftId)` directly without validating the draft exists. AC8 requires: "Nonexistent/deleted draft → EntityNotFoundException". The test `ConflictDetectionServiceTest.checkPlacement_draftNotFound_throwsAndDoesNotLoadOccupancy` expects this behavior, but the new code doesn't implement it. (confirmed)

2. **Test imports reference old package** — `PlacementRuleCheckerTest` and `ConflictDetectionServiceTest` in `src/test/java/com/utms/scheduling/conflict/` import `PlacementRuleChecker` and related classes from the old package. The tests also instantiate `PlacementRuleChecker` with 4 constructor arguments (roomRepository, batchRepository, slotDefinitionRepository, facultyLimitProvider), but the new implementation only takes 2 arguments (facultyLimitProvider, recurrenceOverlapEvaluator). The tests won't compile. (confirmed)

3. **WebSocket allows all origins** — `WebSocketConfig.registerStompEndpoints()` uses `setAllowedOriginPatterns("*")` with a TODO comment. This is a known dev-stage gap but should be flagged for production hardening. (confirmed — carried from prior review)

</details>

---

<details>
<summary>Details</summary>

### Package reorganization rationale

The split between `com.utms.conflict.detection` (delivery) and `com.utms.scheduling.conflict` (shared types) is sound. The delivery layer can depend on the scheduling module's types without creating circular dependencies. This follows the layered architecture pattern from the backend standards.

However, the tests weren't updated to match. The test files remain in `com.utms.scheduling.conflict` and their imports reference the old package locations. The test constructor calls also reflect the old `PlacementRuleChecker` signature.

### Missing draft validation

The deleted `ConflictDetectionService` in `com.utms.scheduling.conflict` had:

```java
private void requireDraft(Long draftId) {
    draftRepository.findByIdAndDeletedAtIsNull(draftId)
            .orElseThrow(() -> new EntityNotFoundException("TimetableDraft", draftId));
}
```

Both `checkPlacement()` and `checkDraft()` called this before loading occupancy. The new implementation in `com.utms.conflict.detection.ConflictDetectionService` has no `draftRepository` dependency at all — it only injects `ruleChecker` and `occupancyLoader`. The `requireDraft()` guard is gone.

This breaks AC8 and means:
- Conflict checks can run against non-existent draft IDs
- Soft-deleted drafts would still have their sessions loaded
- The test expectations won't match the actual behavior

### Constructor signature change

The old `PlacementRuleChecker` injected `RoomRepository`, `BatchRepository`, and `SlotDefinitionRepository` directly (for capacity lookups and consecutive-hours slot time resolution). The new implementation removes these repository dependencies and only takes:
- `FacultyLimitProvider` — for workload limits
- `RecurrenceOverlapEvaluator` — for fortnightly overlap checks

The room capacity check comment says "deferred to service layer", but the `PlacementRuleChecker` still contains the logic — it just can't execute it without the repositories. The `checkPlacement()` method has a comment: "HC-ENG-4: Room capacity (requires room master data lookup - deferred to service layer)".

This is a functional change: room capacity checks are no longer performed by `PlacementRuleChecker`. The old tests (`check_roomCapacityLessThanBatchStrength_returnsCapacityConflict`) will fail because the repositories are no longer injected.

### Javadoc improvements

The modifications to `ConflictDto`, `ConflictType`, `DraftOccupancyIndex`, `FacultyLimitProvider`, and `ProposedPlacementRequest` are documentation-only. Each field now has a proper Javadoc comment explaining its purpose. The `ConflictType` enum constants now document their constraint IDs (HC-ENG-1 through HC-ENG-12). The `DraftOccupancyIndex.Occupant` record has a full `@param` block. These are quality improvements with no behavioral impact.

### Security and error handling

The security posture is unchanged from the prior review:
- WebSocket allows all origins with a TODO
- No `@PreAuthorize` on endpoints (consistent with module's dev-stage `permitAll`)
- REST path has Jakarta validation; WebSocket path does not

The missing draft validation is the new security-relevant issue — without it, any draft ID (including ones that don't exist or are deleted) will be processed, potentially leaking information about whether sessions exist for that draft ID.

### Test coverage gaps

Beyond the compilation issues:
- Tests expect draft validation behavior that no longer exists
- Tests expect room capacity checking that was removed from the rule checker
- No tests were added for the new package structure

---

## File Map

| File | Change |
|------|--------|
| `src/main/java/com/utms/conflict/detection/ConflictController.java` | New (moved from scheduling.conflict) |
| `src/main/java/com/utms/conflict/detection/ConflictDetectionService.java` | New (moved, draft validation removed) |
| `src/main/java/com/utms/conflict/detection/PlacementRuleChecker.java` | New (moved, repository deps removed) |
| `src/main/java/com/utms/conflict/detection/ConflictWsHandler.java` | New (moved from scheduling.conflict) |
| `src/main/java/com/utms/conflict/detection/WebSocketConfig.java` | New (moved from scheduling.conflict) |
| `src/main/java/com/utms/scheduling/conflict/ConflictDto.java` | Modified (Javadoc added) |
| `src/main/java/com/utms/scheduling/conflict/ConflictType.java` | Modified (Javadoc added) |
| `src/main/java/com/utms/scheduling/conflict/DraftOccupancyIndex.java` | Modified (Javadoc added) |
| `src/main/java/com/utms/scheduling/conflict/DraftOccupancyLoader.java` | Modified (minor logging format) |
| `src/main/java/com/utms/scheduling/conflict/FacultyLimitProvider.java` | Modified (Javadoc added) |
| `src/main/java/com/utms/scheduling/conflict/ProposedPlacementRequest.java` | Modified (minor) |
| `src/test/java/com/utms/scheduling/conflict/PlacementRuleCheckerTest.java` | Unchanged (imports broken) |
| `src/test/java/com/utms/scheduling/conflict/ConflictDetectionServiceTest.java` | Unchanged (imports broken) |

**Full diff**: `git diff main` (deletions) + untracked files in `src/main/java/com/utms/conflict/`

</details>
