# Session recurrence patterns (fortnightly / alternate-week) for A4-13 — re-review (v2)

Re-review of A4-13 after a fix pass against the prior NEEDS_CHANGES review (10 issues). The change set adds a per-session recurrence pattern (`recurrence_type`, `week_group`) to `scheduled_sessions`, a pure `WeekParityResolver` (date → WEEK_A/WEEK_B anchored on the semester-start Monday), a pure `RecurrenceOverlapEvaluator` owning the alternate-week non-conflict rule (HC-FN-1/HC-FN-2), and a `SessionRecurrenceService` + controller exposing set/revert/occurrences. This pass verifies the four Group A fixes (#4, #7, #9, #10) and re-confirms the six deferred items remain genuinely gated on unbuilt modules.

Watch for: the three high/medium access-control and data-resolution gaps (#1 RBAC department scoping, #2 `resolveCampusId` hardcoded `1L`, #3 audit actor `"system"`) are unchanged and remain real behavioral gaps against the design — they are correctly gated on the not-yet-built auth/master-data modules and now carry explicit in-code TODOs tying them to tracked follow-ups [confirmed]; the new #9 guard (semester-end-before-start → 422) has no unit test exercising it, so the added branch is unverified by the suite [confirmed]; integration coverage (#5) is still absent [confirmed, deferred].

**Verdict**: COMMENT

The four Group A fixes are correctly applied. The remaining open items are either genuinely blocked on unbuilt auth/master-data modules (the same state as the existing `SchedulingController`) and tracked as blocking follow-ups, or are cross-story confirmations. Given that, this change set is acceptable to advance to human review — the reviewer should acknowledge the gated follow-ups (#1/#2/#3/#5) as accepted debt rather than in-scope blockers, and may reasonably ask for the one cheap test that covers the new #9 guard.

## High-level view

All four fixes verified by reading the current source. #4: `SessionRecurrenceServiceTest` now contains `setRecurrence_doesNotChangeDayOrSlot_FR15` and `revertToWeekly_doesNotChangeDayOrSlot_FR15`, both capturing `dayOfWeek`/`slotDefinitionId` before and after and asserting equality, and the first also asserts the DTO carries day/slot — closing the FR-1.5 invariant gap. The A4-13 unit count is 20 (5 + 4 + 11), matching the reported figure. #7: the controller has `@Tag` plus `@Operation`/`@ApiResponses`/`@Parameter` on all three endpoints, and the three DTOs each carry `@Schema` at type and field level; springdoc 2.6.0 is on the classpath so the annotations resolve. #9: `getOccurrenceDates` now guards `semesterEnd.isBefore(semesterStart)` and throws `BusinessRuleViolationException` with a `CALENDAR_ANCHOR` detail (422) before the enumeration loop. #10: DELETE `/recurrence` now returns 204 via `@ResponseStatus(HttpStatus.NO_CONTENT)` + `ResponseEntity.noContent()`, and its `@ApiResponse` documents 204.

The three access-control/data gaps are unchanged by design. #1 (no `@PreAuthorize`, no own-department check), #2 (`resolveCampusId` returns constant `1L`), and #3 (controller passes literal `"system"` as actor) all remain, each with an in-code TODO explicitly attributing the gap to the auth/master-data modules and referencing the tracked follow-up. This is consistent with the rest of the codebase — `SchedulingController` is in the same state — so they are codebase-wide known debt, not regressions introduced here. They still change observable behavior against this story's design (any caller can mutate any department's sessions; occurrence dates are wrong for any campus id ≠ 1; audit records are non-attributable), which is why they stay on the ledger even though they are correctly gated.

The one new observation this pass is that the #9 guard, while correctly implemented, is not exercised by any test — the suite has no inverted-calendar case. It is a cheap addition and the only concrete, in-scope thing a reviewer might push back on. Everything else (#5 integration test, #6 lifecycle survival, #8 semester/semesterIdentifier vocabulary) was already deferred with sound rationale and remains so.

<details>
<summary>Issues (6)</summary>

1. **Department-scoping RBAC absent** (high — gated/deferred) — controller still has no `@PreAuthorize` and the service performs no own-department check; any caller reaching the endpoint can change or read any session's recurrence. Correctly gated on the unbuilt auth module with an in-code TODO and tracked as a blocking follow-up; must be wired before these endpoints are exposed to real traffic. Not a blocker for advancing to human review given the gating.
2. **`resolveCampusId` hardcoded to `1L`** (high — gated/deferred) — occurrence dates resolve the academic-calendar anchor for campus 1 regardless of the session's real campus, so any campus id ≠ 1 yields wrong dates or a spurious 422. Gated on the shared master-data (department→campus) wiring, same as A4-11. Must be resolved before A4-39/A4-15 rely on this endpoint across multiple campuses.
3. **Audit actor hardcoded `"system"`** (medium — gated/deferred) — every recurrence change audits as `"system"`, so the NFR-audit "who changed the pattern" is not met. The service already threads an `actor` parameter correctly; only the authenticated principal is missing, which arrives with the auth module.
4. **New #9 guard has no test** (low — in scope) — `getOccurrenceDates` now throws 422 when `semesterEnd` precedes `semesterStart`, but no unit test covers the inverted-calendar branch. Add one `getOccurrenceDates_invertedCalendar_throwsBusinessRule` case asserting the 422 / CALENDAR_ANCHOR. Cheap and fully mockable with the existing `calendar(start, end)` helper.
5. **No integration test** (medium — deferred) — no Testcontainers harness exists in the project yet; the DB CHECK (HC-FN-4), the migration, and the 422 paths remain unverified by an automated end-to-end test. Deferred as a tracked, pattern-setting follow-up needing Docker infra — reasonable.
6. **FR-7.1 lifecycle survival + `semester`/`semesterIdentifier` vocabulary** (low — cross-story confirmation) — pattern survival through draft→publish is structural (columns live on the session row, KD-59) but not exercised here; and the draft `semester` string is passed as the calendar `semesterIdentifier` (coupling shared with A4-11). Both are confirmations owed by the consuming/lifecycle story, not defects in this change set.

</details>

<details>
<summary>Details</summary>

## Fix #4 — FR-1.5 day/slot invariant now tested (verified)

`SessionRecurrenceServiceTest` gains two tests. `setRecurrence_doesNotChangeDayOrSlot_FR15` captures `s.getDayOfWeek()` and `s.getSlotDefinitionId()` before a fortnightly set, then asserts both the entity and the returned DTO are unchanged after — covering both the "session not mutated" and "DTO carries day/slot" halves of the original gap. `revertToWeekly_doesNotChangeDayOrSlot_FR15` does the same across a revert. The assertions read the entity state directly (the `save` mock returns its argument), so they genuinely lock the invariant rather than asserting on a stub. The A4-13 unit total is now 20 (WeekParityResolver 5, RecurrenceOverlapEvaluator 4, SessionRecurrenceService 11), matching the reported count. I could not execute `mvn test` in this environment (no Maven/wrapper on PATH), so "all passing" is taken from the fix report and corroborated by static reading — the tests compile against the current service/DTO signatures and use only already-present mocks.

## Fix #7 — OpenAPI annotations present (verified)

The controller carries `@Tag(name = "Session Recurrence", ...)` at type level and `@Operation` + `@ApiResponses` on all three methods, with `@Parameter` on the `sessionId` path variable. The documented response codes match the actual behavior: PUT documents 200/404/422, DELETE documents 204/404 (aligned with fix #10), GET documents 200/404/422. The three DTOs (`SessionRecurrenceDto`, `OccurrenceDatesDto`, `UpdateRecurrenceRequest`) each have a type-level `@Schema` description and field-level `@Schema` with examples; `UpdateRecurrenceRequest.recurrenceType` correctly marks `requiredMode = REQUIRED`. springdoc-openapi-starter-webmvc-ui 2.6.0 is declared in `pom.xml`, so `io.swagger.v3.oas.annotations.*` resolves. This satisfies the api-standards documentation requirement.

## Fix #9 — start-≤-end guard (verified, untested)

`getOccurrenceDates` now checks `semesterEnd.isBefore(semesterStart)` immediately after resolving the calendar and, if inverted, throws `BusinessRuleViolationException` with a `CALENDAR_ANCHOR` detail — surfacing a misconfigured calendar as a 422 instead of silently returning an empty occurrence list. The guard is placed correctly (after anchor resolution, before the enumeration loop) and reuses the same exception/detail shape as the existing no-anchor path, so it maps through `GlobalExceptionHandler` to 422 consistently. The only gap is test coverage: no unit test drives an inverted calendar, so the branch is logically correct but unverified by the suite. Adding the case is trivial given the existing `calendar(start, end)` helper.

## Fix #10 — DELETE returns 204 (verified)

`revertToWeekly` is annotated `@ResponseStatus(HttpStatus.NO_CONTENT)`, returns `ResponseEntity.noContent().build()`, and its return type is now `ResponseEntity<Void>` — a clean 204 with no body, matching the api-standards DELETE convention. The prior 200-with-DTO behavior is gone. The service-layer `revertToWeekly` still returns a DTO (used by tests and any future caller), which is fine; the controller simply discards it.

## Deferred items — gating re-confirmed

#1/#2/#3 are the substantive open behaviors. Reading the current controller and service:

- The controller has an explicit block comment where `@PreAuthorize` + own-department enforcement will go, tying it to the auth/RBAC module and citing code-review #1 as the tracked follow-up. No enforcement exists today, so cross-department mutation is possible — a real gap against the design's COORDINATOR-own-dept / 403 model, but a known, codebase-wide state (parity with `SchedulingController`).
- `resolveCampusId(Long departmentId)` still `return 1L;` with a TODO pointing at the shared master-data wiring (`SchedulingDataLoader.getCampusIdForDepartment`, shared with A4-11). Occurrence dates are therefore correct only for campus 1.
- Both mutators thread `actor` into the `AuditEvent` correctly; the controller supplies the literal `"system"` with a class-level Javadoc note that this is consistent with `SchedulingController` until auth lands.

These are all genuinely blocked on modules that do not yet exist in the codebase — not deferrable-in-principle work that was skipped. Treating them as blocking follow-ups gated on auth/master-data is the right call, and the endpoints should not be exposed to real multi-department/multi-campus traffic until #1 and #2 are wired.

#5 (integration test) is deferred because the project has no Testcontainers harness at all; introducing one is pattern-setting work with a Docker dependency, reasonably tracked separately. #6/#8 are cross-story confirmations (lifecycle survival is structural via KD-59; the `semester`/`semesterIdentifier` coupling is inherited from A4-11) and are not defects in this change set.

## Unchanged core (re-confirmed sound)

The week-parity algorithm (`floorMod`-based, preceding-Monday normalisation) and the `everCoOccur` non-conflict predicate are unchanged and remain faithful encodings of KD-61/PD-81 and HC-FN-1/HC-FN-2 respectively, with full branch coverage in their pure-unit tests. The service write path ordering (load-or-404 → validate → snapshot → mutate two columns only → save → audit, all `@Transactional`) is unchanged. Migration V12 (two columns, WEEKLY backfill default, HC-FN-4 CHECK, partial index on non-weekly live rows, documented rollback) is unchanged and correct.

</details>

<details>
<summary>File map</summary>

- `controller/SessionRecurrenceController.java` — +OpenAPI annotations (#7); DELETE now 204 (#10); RBAC still a TODO (#1, gated)
- `service/SessionRecurrenceService.java` — +semester start≤end guard → 422 (#9); `resolveCampusId` still `1L` (#2, gated); actor still from caller (`"system"` passed by controller, #3 gated)
- `service/WeekParityResolver.java` — unchanged (KD-61)
- `service/RecurrenceOverlapEvaluator.java` — unchanged (HC-FN-1/HC-FN-2)
- `entity/ScheduledSession.java` — unchanged (recurrenceType default WEEKLY, weekGroup nullable, @Enumerated STRING)
- `enums/RecurrenceType.java`, `enums/WeekGroup.java` — unchanged
- `dto/SessionRecurrenceDto.java` — +@Schema (#7); carries dayOfWeek/slotDefinitionId (verified for #4)
- `dto/OccurrenceDatesDto.java`, `dto/UpdateRecurrenceRequest.java` — +@Schema (#7)
- `repository/ScheduledSessionRepository.java` — unchanged (findByIdAndDeletedAtIsNull)
- `db/migration/V12__add_recurrence_to_scheduled_sessions.sql` — unchanged
- Tests: `SessionRecurrenceServiceTest` (+2 FR-1.5 tests, now 11), `WeekParityResolverTest` (5), `RecurrenceOverlapEvaluatorTest` (4) — 20 total

Note: build not executed in this environment (no Maven/wrapper on PATH); fixes verified by static reading of current source. "20 tests passing" taken from the fix report and corroborated by inspection.

</details>

---

## Fix-loop resolution (post-review)

After the v2 re-review (verdict COMMENT), the one remaining in-scope note was closed:

- **#4 (#9 guard test):** added `getOccurrenceDates_invertedCalendar_throwsBusinessRule` — asserts a 422 when the calendar's semester end precedes its start. A4-13 unit suite is now **21 tests, all passing** (WeekParityResolver 5, RecurrenceOverlapEvaluator 4, SessionRecurrenceService 12). Build verified on Java 21.

### Tracked follow-ups (gated on unbuilt modules — for the human reviewer to accept as debt)

| # | Item | Gated on |
|---|---|---|
| 1 | Department-scoping RBAC (@PreAuthorize + own-dept check; 403 path) | Auth/RBAC module (not built; same state as SchedulingController) |
| 2 | resolveCampusId hardcoded to 1L — occurrence dates wrong for campus id != 1 | Department->campus master-data wiring (shared TODO with A4-11) |
| 3 | Audit actor hardcoded "system" — non-attributable audit | Auth module (authenticated principal); service already threads actor |
| 5 | Integration test (Testcontainers): PUT->GET, DB CHECK, 422 paths, migration | No integration-test harness exists yet; pattern-setting + Docker infra |
| 6/8 | FR-7.1 lifecycle survival; semester/semesterIdentifier coupling | Consuming/lifecycle story; inherited from A4-11 |

These do not block advancing to human review; the endpoints must not be exposed to real multi-department/multi-campus traffic until #1 and #2 are wired.

**Final AI verdict: COMMENT — advanced to human review.**
