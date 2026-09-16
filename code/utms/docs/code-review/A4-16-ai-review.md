# Real-Time conflict detection engine (A4-16)

A new `com.utms.scheduling.conflict` package adds interactive conflict checking for the drag-and-drop editor. A proposed placement (or every session in a draft) is evaluated against a draft-scoped occupancy index, reusing the generation engine's hard-constraint semantics (A4-11) so the real-time catalogue can't drift from generation-time rules. The same `ConflictDetectionService` backs both a REST controller (`POST /conflict-check`, `GET /conflicts`) and a STOMP WebSocket handler, so there is one code path. Faculty/room/batch double-booking, room capacity, and recurrence-aware (fortnightly) gating are detected against real data; workload rules (daily/weekly/consecutive) are fully implemented but sit behind a `FacultyLimitProvider` seam whose default returns empty, so they don't fire until faculty-limits master data lands. Travel-time, prerequisite-sequence, and faculty-hard-block are intentionally deferred.

Watch for: `ROOM_HARD_BLOCK` is listed as "actively detected" in the enum doc but is never emitted by the checker (confirmed — doc/behavior mismatch); WebSocket `setAllowedOriginPatterns("*")` with SockJS (confirmed, dev-gated by TODO but worth a conscious sign-off); the WebSocket path has no validation or structured error handling, unlike REST (confirmed, documented as intentional); `checkDraft` is O(n²) over all sessions with a slot-start repository lookup inside the consecutive-hours inner loop (likely a performance concern at scale, currently unbounded by the SLA).

**Verdict**: COMMENT — no blocking defects. The rule logic, consecutive-hours algorithm, and recurrence gate are correct and faithfully mirror the engine. Concerns are a documentation/behavior mismatch on `ROOM_HARD_BLOCK`, security hardening (already TODO'd), a potential N+1 in the full-draft path, a silent slot-duration fallback, and a few behavioral test gaps.

## High-level view

The design decision that carries the most weight is reuse of the engine's rule semantics rather than re-implementation. The checker mirrors `CSPState`'s comparison operators exactly — daily/weekly use `>=`, consecutive uses `>` — and routes fortnightly co-occurrence through the same `RecurrenceOverlapEvaluator` the generation engine uses. This is the right call and the code documents it well. The consecutive-hours algorithm is re-expressed (start-time-ordered touching intervals instead of the engine's slot-index scan) because the real-time index is keyed differently, but it computes the same value.

The one place documentation outruns behavior is `ROOM_HARD_BLOCK`. The `ConflictType` javadoc groups it under "actively detected," and the `PlacementRuleChecker` header comment lists room hard-block among detected rules — but `check(...)` never emits it, and the engine's `CSPState.isRoomHardBlocked` (which does exist) is not called from here. Functionally this behaves like the other deferred types; the issue is that the doc claims otherwise, which is exactly the kind of drift the reuse strategy is meant to avoid. `FACULTY_HARD_BLOCK` is described more honestly ("wired, but under-reports") though it too has no invocation in the checker.

The occupancy index is a deliberate, well-justified refinement of the planned BitSet approach (KD-61): persisted sessions carry a `slotDefinitionId` rather than a contiguous slot index, and the rule checker needs per-occupant data (recurrence, section, duration) a bare BitSet can't hold. Lookups stay O(1) per slot. It's a request-scoped, single-threaded object, so its use of plain `HashMap`/`ArrayList` is fine — the "thread-safety" question resolves to "not shared, no concurrency."

The security posture is uniformly dev-stage: WebSocket allows all origins, neither endpoint carries authorization annotations, and the WebSocket channel does no input validation. All of this matches the module's existing `permitAll` SecurityConfig and is marked with a production TODO. It's internally consistent, but the WebSocket origin wildcard plus SockJS is the one item worth a conscious risk sign-off rather than a silent carry-forward.

The full-draft check re-runs the single-placement checker once per session, which is quadratic and does per-occupant repository lookups for slot start times. At Phase-1 scale this is likely acceptable, but nothing bounds it and the 2s conflict SLA isn't exercised by any test.

<details>
<summary>Issues (9)</summary>

1. **`ROOM_HARD_BLOCK` doc/behavior mismatch** — the `ConflictType` javadoc and `PlacementRuleChecker` header list room hard-block as actively detected, but `check(...)` never emits it and `CSPState.isRoomHardBlocked` is not called. Either detect it or move it to the deferred group in the docs. (confirmed)
2. **`FACULTY_HARD_BLOCK` described as "wired"** — no invocation exists in the checker; the engine's `isFacultyHardBlocked` is itself a `return false` stub. "Wired" overstates the current state; align the wording with the deferred reality. (confirmed)
3. **Silent slot-duration fallback to 1.0h** — `DraftOccupancyLoader.resolveSlotHours` defaults an unresolved/deleted `SlotDefinition` to `1.0` hours, and `computeConsecutiveHours` silently skips occupants with an unresolvable start. Workload math can be computed on fabricated durations with no warning log. (confirmed)
4. **WebSocket origin wildcard** — `setAllowedOriginPatterns("*")` with SockJS accepts any origin; dev-gated by a TODO but should get an explicit risk sign-off since it's a cross-origin trust boundary. (confirmed)
5. **No authorization on endpoints** — neither the REST controller nor the WS handler carries `@PreAuthorize`; any caller can probe any draft's conflicts by sequential ID. Consistent with module posture; track it as a hardening item. (likely)
6. **WebSocket input has no validation** — the STOMP handler takes a raw `ProposedPlacementRequest` with no `@Valid`; a null `slotDefinitionId`/`facultyId` reaches the service and flows into repository lookups. Documented as intentional (REST is authoritative), but worth validating or asserting null-tolerance. (likely)
7. **Full-draft check is O(n²) + N+1** — `checkDraft` calls the checker per session, and `computeConsecutiveHours` calls `slotDefinitionRepository` per occupant per call; no caching across the draft loop. No test asserts the 2s SLA. (likely)
8. **Test gaps: recurrence suppression, `>=` boundary, self-exclusion** — no end-to-end test that a same-slot faculty clash is *suppressed* through `check(...)` for opposite fortnightly groups; none for the daily/weekly `>=` boundary; none for `sessionId` self-exclusion on a move; none for the null-slot-start branch. (confirmed)
9. **Redundant `Math.max` in consecutive calc** — `computeConsecutiveHours` returns `Math.max(proposedHours, bestRunHours)`; when `proposedStart` resolves, `bestRunHours` already includes the proposed slot, so the max is dead. Harmless, minor clarity. (confirmed)

</details>

<details>
<summary>Details</summary>

### Rule logic faithfully mirrors the engine — with one doc mismatch

The three double-booking rules and capacity are straightforward equality/`<` checks against same-slot occupants and the master-data repos, and they match `HardConstraintValidator`/`CSPState` intent. The workload rules are where drift would be easy, and the code gets the operators right: daily and weekly use `>=` (`dailyHours >= maxDailyHours`), consecutive uses `>` (`consecutiveHours > maxConsecutiveHours`) — identical to `CSPState.wouldExceedDailyLoad`/`wouldExceedWeeklyLoad` (`>=`) and `wouldExceedConsecutive` (`>`). The degrade-to-no-violation behavior when limits are absent also mirrors `CSPState` (`limits == null -> false`), so the seam is semantically honest rather than a silent skip.

The mismatch is `ROOM_HARD_BLOCK`. The `ConflictType` javadoc lists it under "Actively detected," and the `PlacementRuleChecker` class comment says the detected set includes "room hard-block." But `check(...)` emits only the two double-bookings, batch clash, capacity, and the three workload types — there is no room-hard-block predicate, and `CSPState.isRoomHardBlocked` (a real, working method that scans `ActiveBlock`s) is never called from the conflict package. The behavior is fine; the documentation is wrong, and it's the sort of claim that erodes trust in the "reuse so it can't drift" premise. `FACULTY_HARD_BLOCK` has a softer but still optimistic description ("wired, but under-reports") — there's nothing wired in the checker, and the engine's own `isFacultyHardBlocked` is a `return false` TODO. Both should be described the way `TRAVEL_TIME`/`PREREQUISITE_SEQUENCE` are: deferred, no detection yet.

One caveat on the daily/weekly math: the checker adds `proposedHours` to the sum of existing hours, whereas the engine's `CSPState` checks the running total *after* assignment. These converge for a fresh placement, and for a move the `sessionId` self-exclusion in `occupantsOnDay`/`allOccupants` prevents double-counting the moved session's own hours. That exclusion is correct — but no test covers the move case for workload.

### Consecutive-hours interval algorithm is correct

The engine scans slot indices left and right of the target while occupancy bits are set. The checker can't do that (it's keyed by `slotDefinitionId`, not a contiguous index), so it reconstructs intervals from slot start/end times, sorts by start, and walks them tracking a "touching" run (`iv[0].equals(prevEnd)`). Traced against the middle-of-run case (faculty 08–09 and 10–11, proposed 09–10): the walk accumulates 60 → 120 (proposed, flag set, best=2.0) → 180 (best=3.0), matching the engine's bidirectional sum of 3h. A disconnected later run correctly resets `runIncludesProposed` at the gap, so an unrelated longer afternoon block doesn't leak into the result.

The final `Math.max(proposedHours, bestRunHours)` is redundant whenever `proposedStart` resolved — `bestRunHours` already includes the proposed interval — but it's harmless and arguably guards the null-start early-return contract.

### Silent slot-duration fallback

`DraftOccupancyLoader.resolveSlotHours` maps an unresolved or soft-deleted `SlotDefinition` to `1.0` hours (`.orElse(1.0)`), and `computeConsecutiveHours` skips occupants whose start time can't be resolved. Both are "conservative" in the sense of not throwing, but the 1.0h default silently feeds fabricated durations into daily/weekly/consecutive sums once limits are live — a slot that's actually 3h counted as 1h could hide a real workload violation. There's no `WARN` log when this fallback fires, so it would be invisible in operation. Given the standards call for logging near-limit and recoverable-anomaly conditions, a `WARN` on the fallback (and on a null proposed start) would make the degrade observable. Behaviorally under-reporting is the safe direction for a real-time hint, so this is a quality/observability note, not a correctness blocker.

### Recurrence gate is used correctly

Before any resource comparison, `check` calls `recurrenceOverlapEvaluator.everCoOccur(proposedRecurrence, toRecurrence(o))` and `continue`s when they can never share a week. The proposed placement is treated as WEEKLY, which co-occurs with everything — so the gate only ever *suppresses* when both the occupant and the proposal are fortnightly on opposite groups. That's the intended KD-64 behavior and it reuses the engine's evaluator rather than re-deriving week-group algebra. `toRecurrence` maps a fortnightly occupant to its `WeekGroup` and defaults everything else to weekly, which is safe.

### Occupancy index: single-threaded, so no thread-safety issue

`DraftOccupancyIndex` uses plain `HashMap`/`ArrayList` and is built fresh per request by `DraftOccupancyLoader.load` inside a `@Transactional(readOnly = true)` service call. It's never shared across threads or cached, so the absence of synchronization is correct, not a gap. Accessor methods defensively copy (`new ArrayList<>(raw)`) and never return null, which keeps callers simple. `EmptyFacultyLimitProvider` is a stateless singleton returning `Optional.empty()` — trivially thread-safe.

### Layering and standards adherence

Constructor injection via `@RequiredArgsConstructor` throughout, no field `@Autowired`, no entities crossing the service boundary (the checker maps `Room`/`Batch`/`SlotDefinition` to primitives internally and emits `ConflictDto`), and DTOs use `@Builder`/`@Getter`. The controller is thin and delegates; the service owns the transaction boundary and the not-found check. `ConflictDto` is immutable (`final` fields, no setter), appropriate for a transient result. This lines up with the backend standards.

Two small deviations, neither blocking. The controller intentionally returns bare `List<ConflictDto>` rather than the `{data, meta}` envelope the API standard illustrates — the doc calls this out as matching the existing `SchedulingController`, so it's a consistent-with-neighbors choice. And there are no Springdoc `@Operation`/`@Schema` annotations on the endpoints or DTOs, which the API standard asks for; the rest of the module may be equally sparse, but it's a documentation gap.

### Security posture is uniformly dev-stage

`WebSocketConfig` registers `/ws` with `setAllowedOriginPatterns("*")` and SockJS. The wildcard is the notable item: combined with SockJS's HTTP fallback transports, any origin can open a session and drive `/app/drafts/{id}/conflict-check`. It's marked with a production TODO mirroring the `SecurityConfig` `permitAll` TODO, so it's internally consistent with where the project is — but a cross-origin trust boundary opened by a wildcard deserves a conscious sign-off rather than riding along on the general permitAll. Neither the REST nor WS entry point has `@PreAuthorize`, so once auth lands there's nothing here restricting which role or which department's draft a caller may inspect; draft IDs are sequential Longs, so conflict data for any draft is enumerable. The human-readable `description` strings contain only IDs the caller already supplied plus capacities/strengths — no stack traces or internal paths — so the "no internals leaked" requirement holds.

### Error handling

The REST path is clean: missing draft → `EntityNotFoundException` → 404 via the global handler; malformed body → Jakarta validation → 400. The service's `requireDraft` runs before any occupancy load, and a test confirms the loader is never touched on a missing draft. The WebSocket path is deliberately thinner — no `@Valid`, coarser error propagation — with the documented stance that REST is the authoritative validated channel. That's a defensible split, but a null `slotDefinitionId` or `facultyId` arriving over STOMP would flow into the checker; `slotKey` would build `"MONDAY#null"` and equality checks compare against nulls (tolerated), while capacity/consecutive lookups pass the null to repositories. Worth either validating in the handler or asserting the service tolerates nulls.

### Test coverage

Fifteen tests cover the happy paths and the main rule types well: each double-booking type, capacity, multiple-conflicts-at-once, the weekly-vs-fortnightly co-occurrence, the consecutive-hours violation with limits supplied, and the data-pending no-limits case. Service tests cover not-found (both entry points), empty result, propagation, and full-draft aggregation.

The gaps are behavioral. There's no end-to-end test that a same-slot faculty clash is *suppressed* through `check(...)` when both sides are opposite fortnightly groups — the suppression is only asserted at the evaluator level, so a regression in `toRecurrence` or the gate wiring wouldn't be caught. No test exercises the `>=` boundary for daily/weekly (hours landing exactly on the max), the `sessionId` self-exclusion during a move (which prevents a session clashing with itself and prevents double-counting its own hours), or the conservative null-slot-start / 1.0h-fallback branches. And nothing asserts the 2s SLA against a realistically sized draft, which is the one NFR this feature exists to satisfy.

</details>

<details>
<summary>Files reviewed</summary>

- `ConflictType.java` — 11-value conflict catalogue; `ROOM_HARD_BLOCK`/`FACULTY_HARD_BLOCK` doc overstates detection.
- `ConflictDto.java` — immutable transient result DTO.
- `ProposedPlacementRequest.java` — validated request DTO (allowlist, `@Positive`/`@NotNull`/`@NotBlank`).
- `DraftOccupancyIndex.java` — request-scoped occupancy, keyed `day#slot` → `Occupant` records.
- `DraftOccupancyLoader.java` — `@Transactional(readOnly)` hydration; caches slot hours per load; 1.0h fallback.
- `PlacementRuleChecker.java` — core rule logic, consecutive-hours interval algorithm, recurrence gate.
- `FacultyLimitProvider.java` / `EmptyFacultyLimitProvider.java` — seam + no-op default.
- `ConflictDetectionService.java` — `checkPlacement` + `checkDraft`, draft validation.
- `ConflictController.java` — REST endpoints.
- `WebSocketConfig.java` — STOMP config, origin wildcard (dev TODO).
- `ConflictWsHandler.java` — STOMP delegate.
- `PlacementRuleCheckerTest.java` (10), `ConflictDetectionServiceTest.java` (5).
- Cross-referenced: `CSPState.java`, `RecurrenceOverlapEvaluator.java`, `FacultyWorkloadLimits.java`.

No git repository present; review performed against the working-tree files directly.

</details>
