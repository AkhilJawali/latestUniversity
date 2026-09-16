# Code Review — A4-12: Timeout and Infeasibility Handling

## Review Summary

| Metric | Value |
|---|---|
| Reviewer | AI (semantic_reviewer) |
| Date | 2026-08-27 |
| Verdict | APPROVED (after fixes) |
| Files Reviewed | 20+ (new + modified) |
| Blocking Issues Found | 3 |
| Blocking Issues Fixed | 3 |
| Non-blocking Issues | 3 (documented) |

## Blocking Issues (Fixed)

### 1. Migration nullability mismatch
- **Severity:** High
- **File:** V11__timeout_infeasibility.sql
- **Problem:** `created_by` and `updated_by` declared nullable for all three new tables, diverging from project convention (NOT NULL in all other migrations) and BaseEntity's `nullable = false`.
- **Fix:** Changed to `VARCHAR(100) NOT NULL` in all three tables.

### 2. Empty denormalized fields on UnplacedSession
- **Severity:** High
- **File:** SchedulingEngineService.java -> persistUnplacedSessions()
- **Problem:** Set courseCode, courseName, facultyName, batchName to empty strings despite the entity's purpose being "immediate readability without joins." SchedulingInput with resolved names was available in scope.
- **Fix:** Method now accepts SchedulingInput parameter and resolves names via `input.getCourseCode()`, `input.getCourseName()`, `input.getFacultyName()`, `input.getBatchName()`.

### 3. Imprecise constraint identification
- **Severity:** Medium
- **File:** ConstraintSolver.java -> identifyBlockingConstraints()
- **Problem:** Unconditionally added FACULTY_AVAILABILITY and SLOT_DURATION_MISMATCH for every empty domain regardless of actual cause, producing misleading infeasibility reports.
- **Fix:** Now checks actual conditions: equipment match against room inventory, batch strength against room capacities, duration match against slot grid. Only reports constraints that actually contributed to the empty domain.

## Non-blocking Issues (Documented, not fixed)

### 4. Cancel returns stale status
- **File:** SchedulingEngineService.cancelGeneration()
- **Note:** The response shows IN_PROGRESS because the solver hasn't reacted yet. The message says "Cancellation requested" which mitigates confusion. Documented as intentional - client should poll /status for final state.

### 5. updateBestSoFar transaction semantics
- **File:** SchedulingEngineService -> lambda passed as ProgressCallback
- **Note:** Runs as auto-committed individual saves (Spring proxy won't intercept private method). Acceptable for progress updates - documented as intentional.

### 6. No per-request timeout override in API
- **File:** GenerateRequest DTO
- **Note:** Timeout is config-only (SchedulingEngineProperties), not per-request. Column exists on GenerationRequest for future use. Deferred to a future story if per-request override is needed.

## Positive Observations

- Infeasibility detection correctly avoids false positives on mid-search dead-ends
- Thread safety is sound: volatile single-writer/single-reader, ConcurrentHashMap, cleanup in finally
- Cancel endpoint properly idempotent (200 always)
- Stale request reaper prevents department bricking on JVM crash
- Entity-migration column alignment correct
- is_partial + partial_acknowledged columns provide explicit partial draft marking
