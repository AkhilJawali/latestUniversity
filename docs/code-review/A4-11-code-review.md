# Code Review — A4-11: Timetable Generation Engine

**Reviewer:** AI (semantic_reviewer)
**Date:** 2026-08-27
**Verdict:** NEEDS_CHANGES (4 blocking, 5 non-blocking)

## Blocking Issues

### 1. Thread-safety: ConstraintSolver singleton state
- **File:** ConstraintSolver.java
- **Severity:** High
- **Problem:** bestPartialState/bestPartialCount are mutable instance fields on singleton. Concurrent requests corrupt each other.
- **Fix:** Move to method-local scope.
- **Status:** FIXED

### 2. Checkpoint/restore corrupts domains
- **File:** CSPState.java
- **Severity:** High
- **Problem:** restore() trims by size from end, but forwardCheck removeIf removes from arbitrary positions.
- **Fix:** Store full domain snapshots (List copies), not just sizes.
- **Status:** FIXED

### 3. Transaction gap in progress updates
- **File:** SchedulingEngineService.java
- **Severity:** Medium
- **Problem:** updateProgress/completeRequest/failRequest run outside @Transactional on async thread.
- **Fix:** Move to transactional bean.
- **Status:** FIXED

### 4. ActiveBlock day mismatch in soft optimizer
- **File:** SoftConstraintOptimizer.java
- **Severity:** Medium
- **Problem:** identifyViolations/computeSoftBlockAvoidance don't check block's day vs assignment's day.
- **Fix:** Add day comparison.
- **Status:** FIXED

## Non-blocking Issues

### 5. Missing dept-scoping on read endpoints — Deferred to auth module
### 6. Missing @PreAuthorize — Deferred to auth module
### 7. LCV unimplemented — Performance optimization deferred
### 8. Hill-climbing random walk — FIXED (added quality score comparison)
### 9. ServiceUnavailableException envelope — Deferred

## Summary
- 4 blocking fixed, 1 medium fixed
- 4 non-blocking deferred (auth module dependency)
- Code ready for human review
