# Code Review — Real-Time Conflict Detection Engine (A4-16)

- **Story**: A4-16 — Real-Time Conflict Detection Engine
- **Code Review Subtask**: A4-74
- **Date**: 2026-09-04
- **Reviewer (AI pre-check)**: semantic_reviewer (per squad-rules)
- **Scope**: `com.utms.scheduling.conflict` (13 classes + 2 test classes) + `spring-boot-starter-websocket` in pom.xml
- **Verdict**: **COMMENT** — no blocking defects

---

## 1. Summary

Behavioral AI review run before human review. The rule logic, the consecutive-hours
interval algorithm, and the recurrence gate are correct and faithfully mirror the
generation engine (daily/weekly `>=`, consecutive `>`, limits-absent → no violation —
all matching `CSPState`). Layering and standards are adhered to: constructor
injection, no entities past the service boundary, immutable DTOs, thin controller.
The intentional deferrals (TRAVEL_TIME, PREREQUISITE_SEQUENCE, faculty-limits behind
the provider seam) are correctly implemented.

**No blocking defects.** Nine findings, all non-blocking; the two documentation
mismatches, the observability gap, and one cleanup were fixed before human review
(see §3) and re-verified green.

---

## 2. Findings

| # | Severity | Finding | Disposition |
|---|----------|---------|-------------|
| 1 | Medium (doc) | `ROOM_HARD_BLOCK` javadoc/header listed it as "actively detected" but `check(...)` never emits it | **FIXED** — moved to the deferred group in docs |
| 2 | Low (doc) | `FACULTY_HARD_BLOCK` described as "wired" though no invocation exists and engine check is a stub | **FIXED** — reworded to deferred, no detection yet |
| 3 | Low | Silent 1.0h slot-duration fallback in `DraftOccupancyLoader` with no WARN log | **FIXED** — added WARN log on the fallback |
| 4 | Medium (security) | WebSocket `setAllowedOriginPatterns("*")` + SockJS | Accepted for dev; TODO present. Flagged for prod sign-off (mirrors SecurityConfig permitAll) |
| 5 | Medium (security) | No `@PreAuthorize` on REST/WS endpoints | Accepted — consistent with module posture; hardening tracked with the auth module |
| 6 | Low (security) | WS handler has no `@Valid` on the request | Accepted — REST is the authoritative validated channel (documented); service tolerates the fields |
| 7 | Medium (perf) | `checkDraft` is O(n²) with N+1 slot-start lookups; no 2s SLA test | Accepted for Phase-1 scale; performance/SLA test is a follow-up (needs realistic dataset) |
| 8 | Low (test) | Behavioral gaps: recurrence suppression e2e, `>=` boundary, sessionId self-exclusion on move, null-slot-start branch | Noted as follow-up test additions; core ACs covered |
| 9 | Low (cleanup) | Redundant `Math.max` in `computeConsecutiveHours` | **FIXED** — removed dead max |

---

## 3. Fixes applied before human review

- **#1 / #2 (documentation accuracy):** `ConflictType` javadoc and `PlacementRuleChecker`
  header updated so ROOM_HARD_BLOCK and FACULTY_HARD_BLOCK are grouped with the other
  deferred types (TRAVEL_TIME, PREREQUISITE_SEQUENCE) — no code now claims detection
  that isn't there. Directly serves the "reuse so it can't drift" premise.
- **#3 (observability):** `DraftOccupancyLoader.resolveSlotHours` now logs a WARN when a
  slot duration cannot be resolved and the 1.0h fallback is used.
- **#9 (clarity):** removed the redundant `Math.max(proposedHours, ...)` in the
  consecutive-hours calculation (bestRunHours already includes the proposed slot when
  the start resolves).

**Re-verified after fixes:** `mvn -Dtest="PlacementRuleCheckerTest,ConflictDetectionServiceTest" test`
→ 15 tests, 0 failures — BUILD SUCCESS. (The AC5 consecutive-hours test still passes,
confirming the `Math.max` removal is behavior-preserving.)

---

## 4. Accepted (non-blocking) — tracked as follow-ups

- **Security hardening (#4/#5/#6):** WebSocket origin restriction, `@PreAuthorize`, WS
  validation — to land with the auth module; consistent with the current dev-stage
  `permitAll` posture. No internals are leaked in responses (requirement holds).
- **Performance (#7):** full-draft O(n²) + N+1 and the 2s SLA test — to be validated
  with a realistic dataset / Gatling when a Docker-enabled environment is available.
- **Test additions (#8):** recurrence suppression e2e, boundary and self-exclusion
  cases, null-slot-start branch — extend the unit suite in a follow-up.

These are recorded here for the human reviewer's awareness; none block this story.

---

## 5. Verdict

**COMMENT — ready for human review.** No blocking defects; the doc-accuracy,
observability, and cleanup fixes were applied and re-verified green. The security and
performance items are accepted for Phase 1 and tracked as follow-ups consistent with
the module's current posture. Full AI review detail: `code/utms/docs/code-review/A4-16-ai-review.md`.
