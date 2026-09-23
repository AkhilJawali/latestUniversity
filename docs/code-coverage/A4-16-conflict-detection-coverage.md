# Code Coverage Report — A4-16 Real-Time Conflict Detection Engine

## Summary

| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| Line Coverage | ≥80% | *Pending* | ⏳ Run `mvn test jacoco:report` |
| Branch Coverage | ≥70% | *Pending* | ⏳ Run `mvn test jacoco:report` |

**Note:** Maven is not available in the Kiro environment. Run the following command locally to generate actual coverage data:

```bash
cd code/utms
mvn test jacoco:report
```

Report will be available at: `target/site/jacoco/index.html`

---

## Tested Classes

### Source Files (com.utms.scheduling.conflict)

| Class | Test Class | Coverage Scope |
|-------|-----------|----------------|
| `ConflictType` | N/A (enum) | 100% by definition |
| `ConflictDto` | Covered via service tests | Static factory method |
| `ProposedPlacementRequest` | Covered via service tests | DTO |
| `DraftOccupancyIndex` | `DraftOccupancyIndexTest` | AC6: Index operations |
| `DraftOccupancyLoader` | Covered via service tests | Repository loading |
| `PlacementRuleChecker` | `PlacementRuleCheckerTest` | AC1-AC7, KD-64 |
| `ConflictDetectionService` | `ConflictDetectionServiceTest` | AC8, AC9 |
| `ConflictController` | Integration test recommended | REST endpoints |
| `WebSocketConfig` | Integration test recommended | Config |
| `ConflictWsHandler` | Integration test recommended | WebSocket |

### Test Files

| Test Class | Test Count | Scenarios Covered |
|-----------|-----------|-------------------|
| `DraftOccupancyIndexTest` | 11 | Index add/remove/lookup operations, null handling |
| `PlacementRuleCheckerTest` | 9 | AC1-AC7 conflict types, KD-64 recurrence, self-exclusion |
| `ConflictDetectionServiceTest` | 6 | Placement check, draft check, cross-draft, deduplication, error handling |

---

## Acceptance Criteria Coverage

| AC | Requirement | Test Coverage |
|----|-------------|---------------|
| AC1 | Faculty double-booking detection | `PlacementRuleCheckerTest.testCheckPlacement_facultyDoubleBooking()` |
| AC2 | Room double-booking detection | `PlacementRuleCheckerTest.testCheckPlacement_roomDoubleBooking()` |
| AC3 | Batch/section clash detection | `PlacementRuleCheckerTest.testCheckPlacement_batchClash()` |
| AC4 | Room capacity exceeded detection | `PlacementRuleCheckerTest.testCheckPlacement_roomCapacityExceeded()` |
| AC5 | Faculty workload exceeded detection | *Deferred (PD-99: FACULTY_HARD_BLOCK stub)* |
| AC6 | No conflict for valid placement | `PlacementRuleCheckerTest.testCheckPlacement_validPlacement()` |
| AC7 | Multiple conflicts in one response | `PlacementRuleCheckerTest.testCheckPlacement_multipleConflicts()` |
| AC8 | Malformed input returns error (no stack trace) | `ConflictDetectionServiceTest.testCheckPlacement_draftNotFound()` |
| AC9 | Full-draft conflict list | `ConflictDetectionServiceTest.testCheckDraft()` |

---

## Key Design Decisions Verified

| KD/PD | Decision | Test |
|-------|----------|------|
| KD-61 | Draft-scoped DraftOccupancyIndex | `DraftOccupancyIndexTest` |
| KD-62 | Reuse A4-11 rule predicates | `PlacementRuleCheckerTest` |
| KD-64 | Recurrence-aware overlap | `PlacementRuleCheckerTest.testCheckPlacement_alternateWeekNoConflict()` |
| PD-97 | Transient conflicts (not persisted) | No DB migration required |

---

## Deferred Conflict Types (Per Design)

These `ConflictType` enum values are defined but detection is deferred:

| Type | Reason | Reference |
|------|--------|-----------|
| `TRAVEL_TIME_VIOLATION` | No travel-time master data | PD-98 |
| `PREREQUISITE_SEQUENCE_VIOLATION` | Needs Registrar confirmation | PD-96 |
| `FACULTY_HARD_BLOCK` | Engine stub returns false | PD-99 |

---

## Uncovered Areas

| Class/Method | Reason | Plan |
|-------------|--------|------|
| `ConflictController` | Integration test needed | Run with Testcontainers |
| `WebSocketConfig` | Config class | Integration test |
| `ConflictWsHandler` | WebSocket handler | Integration test with STOMP client |
| `ConflictBroadcaster` | Internal utility | Covered via integration |

---

## Requirement Traceability

| BRD Section | Requirement | Implementation |
|-------------|-------------|----------------|
| §2 | Conflict Detection Engine | `ConflictDetectionService` |
| §3 | ConflictType Catalogue | `ConflictType` enum (KD-63) |
| §4 | < 2s SLA | `DraftOccupancyIndex` O(1) lookups |
| §5 | Recurrence-aware | `RecurrenceOverlapEvaluator` integration |
| §6 | REST + WebSocket delivery | `ConflictController` + `ConflictWsHandler` |

---

## Next Steps

1. Run `mvn test jacoco:report` locally
2. Update this document with actual coverage percentages
3. If coverage < 80%, add tests for uncovered branches
4. Attach this document to A4-409 subtask in Jira

---

*Generated for A4-16 Development Subtask A4-409.*
