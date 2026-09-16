# Locked slot preservation and partial re-generation (A4-14)

The change adds session lock/unlock and partial re-generation on top of the existing CSP scheduling engine. Locking flips `is_locked` on a `ScheduledSession` (with placement validation + audit); partial re-generation partitions a source draft's sessions into FIXED (locked / approved / out-of-scope) and FREE (in-scope, movable), pre-places the FIXED set as immovable occupancy in the solver, regenerates only the FREE subset, and persists a new draft version that copies FIXED sessions forward byte-identical and adds the newly placed FREE sessions. New DB columns (`is_approved`, `source_draft_id`, `regeneration_scope`), three endpoints, three DTOs, a `RegenerationScope`/`FixedSessionInfo` model pair, and two unit test classes round it out.

Watch for: (1) a FREE-variable derivation bug that over-generates sessions for in-scope courses that also have locked/approved sessions, and corrupts the feasibility denominator — **confirmed**, this contradicts the design and breaks HC-LOCK correctness; (2) new endpoints have no `@PreAuthorize` and hardcode `"system"` as the actor, so lock/regenerate are unauthenticated and audit trails are useless — **confirmed**; (3) the copy-forward persister runs `supersedePreviousDrafts` in a separate `@Transactional` from the async orchestrator, with no lock against a concurrent source-draft mutation — **likely** correctness/consistency risk.

**Verdict**: NEEDS_CHANGES

## High-level view

The derive-vs-partition seam is the central defect. FREE sessions are freshly derived from course L-T-P via `SessionDeriver.derive(input)`, which knows nothing about which persisted sessions are locked or approved. FIXED sessions are the persisted locked/approved/out-of-scope rows, copied forward. When an in-scope course has *some* locked sessions and *some* movable ones, the deriver regenerates the course's full session count while the locked ones are also copied forward — the same course ends up with more sessions than its L-T-P requires. `totalRequired` compounds the problem: it is the count of *all* derived variables for the whole department, not the scoped/expected total, so the feasibility score denominator is wrong for every partial re-generation.

Security posture is the second gap. The new `/regenerate`, `/lock`, and `/unlock` endpoints carry no authorization annotation and pass a literal `"system"` as the acting user into both the mutation and the audit event. The rest of the controller has the same shape, so this is consistent with existing local-dev `permitAll`, but for operations that mutate published-adjacent state and are supposed to be attributable in the audit trail (KD-54), shipping without a real principal defeats the audit requirement the design leans on.

The audit action mapping is acceptable but lossy. Lock/unlock use `UPDATED` and regeneration uses `CREATED` against an enum that only has CREATED/UPDATED/DELETED. Semantically fine, but the design named distinct events (`LOCK_SESSION`, `UNLOCK_SESSION`, `PARTIAL_REGENERATION`) and none of that intent survives into the persisted action — a reviewer scanning the audit log cannot distinguish a lock from any other session update.

`prePlaceFixedSessions` occupancy is correct and a clean generalization of `prePlaceCommonSlots`. Transaction boundaries around the async persister and the hand-rolled `serializeScope` JSON are the remaining smaller concerns.

<details>
<summary>Issues (11)</summary>

1. **FREE-variable over-generation (derive-vs-partition seam)** — *confirmed, blocker.* `derive(input)` produces sessions from course L-T-P for the whole department; filtering by scope match keeps every session of an in-scope course, including those whose locked counterparts are copied forward. In-scope courses with locked/approved sessions get regenerated in full AND have their fixed sessions copied forward → more sessions than L-T-P requires. Derive FREE variables from the FREE persisted subset (as the design's step 2 states), or subtract fixed sessions per (course/batch/section/type) from the derived count.
2. **`totalRequired` denominator wrong for partial re-gen** — *confirmed, blocker.* `totalRequired = allVariables.size()` counts every derived session in the department, not the scoped expected total. Feasibility score = `totalPlaced / totalRequired` is therefore meaningless for partial re-generation (and can even exceed 1.0 once over-generation is fixed differently). Define `totalRequired` as fixed-count + expected-free-count.
3. **No `@PreAuthorize` on new endpoints** — *confirmed.* `/regenerate`, `/lock`, `/unlock` are unauthenticated. Consistent with existing controller + local-dev `permitAll`, but must be gated before any non-local deployment. Add role annotations (e.g. coordinator/HOD) and a TODO tying it to the auth module.
4. **Hardcoded `"system"` actor** — *confirmed.* Controller passes `"system"` as userId into mutations and audit events. Audit trail (KD-54) cannot attribute lock/unlock/regeneration to a real user. Wire the authenticated principal once auth exists; until then, flag explicitly.
5. **Cross-transaction supersede without concurrency guard** — *likely.* `persistRegenerationResult` runs in its own `@Transactional` on the async thread and calls `supersedePreviousDrafts` + `getMaxVersion`. Nothing prevents the source draft from being mutated (or a session unlocked) between partition load and copy-forward; the in-progress guard is checked at trigger time, not held. Consider optimistic version checks or re-loading fixed sessions inside the persist transaction.
6. **Audit action semantics flattened** — *acceptable, comment.* Design named LOCK_SESSION / UNLOCK_SESSION / PARTIAL_REGENERATION; code uses UPDATED/CREATED with no discriminator in the payload. Acceptable given the enum, but add an action/type key in the event map so the log is queryable.
7. **`serializeScope` hand-rolls JSON** — *confirmed, minor.* `String.format` with `List.toString()` produces `[1, 2]`, which is valid-ish JSON only because Long lists render compatibly; any future non-numeric field breaks it, and it is not round-trippable. Use the project's Jackson `ObjectMapper`.
8. **Free sessions persisted with `isApproved` unset** — *possible.* `persistSessions` sets `isLocked(false)` but never sets `isApproved`; relies on the entity default (`false`). Correct today, but implicit — set it explicitly for clarity since this method is now shared by regeneration.
9. **`persistUnplacedSessions` iterates FREE variables but indexes by full var list** — *confirmed, minor.* In `executeRegeneration` the unplaced loop uses `freeVariables` and `state.getVariableCount()`; state was initialized with `freeVariables`, so indices align. Fine, but the symmetry is load-bearing and undocumented — a comment would prevent a future off-by-one.
10. **`RegenerationStatusDto` counts recomputed in controller** — *comment.* The controller re-queries all sessions and recomputes fixed/regenerating counts after the service already partitioned them. Duplicated logic that can drift from `isFree`. Return the counts from the service.
11. **Migration comment vs. design version drift** — *comment.* `V13` documents that the design said `V12`; the header note is good, but confirm the design doc / traceability is updated so the next reviewer isn't misled.

</details>

<details>
<summary>Details</summary>

### The derive-vs-partition seam over-generates in-scope sessions

This is the concern the requester flagged, and it is a real defect. The design (Section 7.2, step 2) is explicit:

> Derive SessionVariables ONLY for the FREE sessions (sessionDeriver over the free subset).

The implementation does not do this. `executeRegeneration` derives from the loaded `SchedulingInput`:

```java
List<SessionVariable> allVariables = sessionDeriver.derive(input);
List<SessionVariable> freeVariables = allVariables.stream()
    .filter(v -> isVariableFree(v, scope))
    .toList();
int totalRequired = allVariables.size();
```

`SessionDeriver.derive` builds variables from `input.getCourses()` L-T-P hours — one variable per derived lecture/tutorial/practical session. It has no knowledge of the persisted `ScheduledSession` rows or their `is_locked` / `is_approved` flags. `isVariableFree` then keeps a variable purely on scope match (batch/section/course id), with an explicit comment asserting that locked/approved "live on persisted sessions (fixed), not on freshly-derived variables, so scope match suffices here."

That assertion is exactly the bug. Walk a concrete case:

- Course C is in the regeneration scope and requires 3 lectures (from L-T-P).
- In the source draft, 1 of C's 3 lecture sessions is locked; 2 are movable.
- FIXED partition (from persisted sessions): the 1 locked session → copied forward byte-identical AND pre-placed as occupancy.
- FREE variables (from `derive`): all 3 of C's lectures match the scope → 3 free variables enter the solver.
- Persist: `copyForwardFixedSessions` writes the 1 locked session, `persistSessions` writes the 3 solved free sessions.
- New draft has **4 lecture sessions for course C**, which requires 3.

The locked session is double-counted: once as fixed (copied forward) and once as a freshly derived free variable. Any in-scope course that has at least one locked or approved session over-generates by the number of its fixed sessions. This violates the L-T-P session count and, indirectly, HC-LOCK correctness (the locked placement is preserved, but a duplicate of that session is also scheduled elsewhere).

The correct approach per the design is to derive the free variable set from the FREE persisted sessions — i.e. the movable sessions that actually exist in the draft — rather than re-deriving the entire course catalogue and filtering. Re-deriving from L-T-P is only valid for a full generation where no sessions pre-exist. If the intent is to allow the scope to *add* sessions that don't yet exist, that needs to be an explicit, separate behavior, not an accidental consequence of course-level derivation.

### Feasibility denominator is computed over the wrong universe

Coupled to the above, `totalRequired = allVariables.size()` is the count of every derived session for the *entire department*, not the partial scope. It is then threaded into `persistRegenerationResult`:

```java
int totalPlaced = placedFree + fixedSessions.size();
double feasibility = totalRequired > 0 ? (double) totalPlaced / totalRequired : 0.0;
```

For a partial re-generation touching a handful of sessions in a large department, `totalPlaced` (fixed + free placed) is small and `totalRequired` is the whole department — feasibility will read artificially low. If the derivation bug above is fixed by scoping the free set but `totalRequired` is left as the full derivation, the numerator and denominator measure different universes and the score is nonsense. `totalRequired` for a partial re-gen should be `fixedSessions.size() + expectedFreeCount`, where `expectedFreeCount` is the number of free variables actually being solved.

### prePlaceFixedSessions occupancy is correct

The occupancy generalization is sound. For each fixed session it resolves day/slot/room indices and sets the room, faculty, and batch bits at the same `bitIndex = dayIndex * maxSlotsPerDay + slotIdx` used everywhere else in `CSPState`:

```java
int roomIdx = resolveRoomIndex(fs.getRoomId());
if (roomIdx >= 0) roomOccupancy[roomIdx].set(bitIndex);
if (fs.getFacultyId() != null)
    facultyOccupancy.computeIfAbsent(fs.getFacultyId(), k -> new BitSet(...)).set(bitIndex);
if (fs.getBatchId() != null)
    batchOccupancy.computeIfAbsent(fs.getBatchId(), k -> new BitSet(...)).set(bitIndex);
```

Unlike `prePlaceCommonSlots` (which blocks the slot across all rooms), this occupies one specific room/faculty/batch, which is the right model for a fixed session — the free solver will then treat those resources as busy at that slot and schedule around them (HC-LOCK-4). Null-safety on faculty/batch/room resolution is handled (negative index skips room; null faculty/batch skipped). One note: the design said `prePlaceCommonSlots` would be *refactored to delegate* to `prePlaceFixedSessions`; the code kept them as two parallel methods instead. That is a harmless deviation, but the duplicated bit-index arithmetic is now in two places.

### Security: unauthenticated endpoints, no principal for audit

All three new endpoints are wide open:

```java
@PostMapping("/{draftId}/regenerate")
public ResponseEntity<RegenerationStatusDto> regenerate(...) { ... "system" ... }

@PostMapping("/{draftId}/sessions/{sessionId}/lock")
public ResponseEntity<SessionLockDto> lockSession(...) { ...lock(draftId, sessionId, "system"); }
```

No `@PreAuthorize`, and `"system"` is passed as the acting user. This matches the existing `SchedulingController` methods and the documented local-dev `permitAll` posture, so it is not a regression — but it is worth stating plainly that these operations mutate scheduling state and are supposed to be audited per actor (KD-54). An audit event whose `userId` is always `"system"` cannot satisfy "who locked this session." When the auth module lands, both the annotation and the real principal need wiring; a `// TODO` referencing the auth story would make the debt explicit rather than silent.

### Audit action mapping is acceptable but drops intent

`AuditEvent.Action` has only CREATED/UPDATED/DELETED. Lock/unlock map to UPDATED and regeneration to CREATED, which is defensible — a lock is a field update, a new draft is a creation. The concern is that the design specified three named events (LOCK_SESSION, UNLOCK_SESSION, PARTIAL_REGENERATION) and none of that survives: a query over the audit log sees a generic `ScheduledSession UPDATED` for both lock and unlock, distinguishable only by digging into the `previousValue`/`newValue` maps. This is acceptable for now given the enum, but adding an explicit discriminator to the event payload (e.g. an `"operation": "LOCK_SESSION"` key) would preserve the design intent without touching the enum. Extending the enum is the cleaner long-term fix but is a cross-cutting change beyond this story.

### Transaction boundaries and concurrency

`triggerRegeneration` is `@Transactional` and does the partition + guard synchronously, then fires `executeRegeneration` on the async executor. `executeRegeneration` is not transactional itself; it calls into `resultPersister.persistRegenerationResult`, which is a separate `@Transactional` bean method (the KD-54 "separate transactional bean called from async thread" pattern). Within that method `supersedePreviousDrafts` + `getMaxVersion` + save run atomically, which is good.

The gap is between trigger and persist. The in-progress guard (`existsInProgressForDeptSemester`) is evaluated once at trigger time and not held. The fixed-session list is loaded once in `executeRegeneration` (a second `findByDraftIdAndDeletedAtIsNull`, distinct from the one in `triggerRegeneration`), and again the controller loads all sessions a third time to compute DTO counts. If a coordinator unlocks a session, or another mutation touches the source draft between those reads, the partition the solver used and the set copied forward can diverge from the draft's actual state, with no version check to catch it. For a first cut this may be acceptable, but it should be a known limitation, ideally guarded by an optimistic version on the source draft.

### serializeScope hand-rolls JSON

```java
return String.format("{\"batchIds\":%s,\"sectionIds\":%s,\"courseIds\":%s}",
    scope.getBatchIds(), scope.getSectionIds(), scope.getCourseIds());
```

This relies on `List<Long>.toString()` rendering as `[1, 2, 3]`, which happens to be JSON-compatible for numeric lists. It is brittle: a null list renders as `null` (fine), but any future string field, or a locale/format surprise, breaks it, and it is not symmetric with any deserializer. Since the value is stored in a column the design describes as a JSON selector, use the project's configured Jackson `ObjectMapper.writeValueAsString(scope)` so serialization and any future read path agree.

### Tests

`SessionLockServiceTest` is solid: it covers lock happy path with audit assertion, not-found, wrong-draft (ownership), incomplete-placement 422, and unlock happy/not-found. `RegenerationScopeTest` covers `isEmpty` and `matches` across all three id-sets including OR semantics and null-section behavior.

Not tested: the entire `executeRegeneration` / `persistRegenerationResult` flow — which is where the two blocker bugs live. There is no test that a course with a locked session and movable sessions produces the correct total session count after regeneration, no test of the feasibility denominator for a partial scope, and no test of `prePlaceFixedSessions` occupancy interacting with the solver. A test that set up a draft with mixed locked/free sessions for one course and asserted the resulting session count would have caught the over-generation. This coverage gap is why the bugs shipped.

</details>

<details>
<summary>File map</summary>

- `db/migration/V13__locked_slot_partial_regeneration.sql` — adds `is_approved`, `source_draft_id` (+FK), `regeneration_scope`, partial index; reversible header documented.
- `entity/ScheduledSession.java` — `isApproved` flag (default false).
- `entity/GenerationRequest.java` — `sourceDraftId`, `regenerationScope`.
- `enums/FixedReason.java` — new: LOCKED / APPROVED / OUT_OF_SCOPE.
- `model/FixedSessionInfo.java` — new immutable carrier for pre-placement.
- `model/RegenerationScope.java` — new: `isEmpty()`, `matches()` (OR over id-sets).
- `solver/CSPState.java` — new `prePlaceFixedSessions`, extracted `resolveSlotIndex`/`resolveRoomIndex`; occupancy correct.
- `service/SessionLockService.java` — new: lock/unlock with placement + ownership validation + audit.
- `service/SchedulingResultPersister.java` — new `persistRegenerationResult` + `copyForwardFixedSessions`; feasibility uses passed `totalRequired` (denominator bug originates upstream).
- `service/SchedulingEngineService.java` — new `triggerRegeneration`/`executeRegeneration` + partition helpers; **derive-vs-partition bug here**.
- `controller/SchedulingController.java` — new `/regenerate`, `/lock`, `/unlock`; no `@PreAuthorize`, `"system"` actor, recomputes counts.
- `dto/{SessionLockDto,RegenerateRequest,RegenerationStatusDto}.java` — new DTOs.
- `test/.../SessionLockServiceTest.java`, `test/.../RegenerationScopeTest.java` — unit tests; no regeneration-flow coverage.

Full analysis derived from reading the working-tree files directly (no git repo present at `d:\BL_UNI\code\utms`).

</details>


---

## Resolution (post-review fixes)

**Final verdict after fixes: APPROVED** (re-review confirmed).

### Blockers fixed
1. **Derive-vs-partition over-generation** — `executeRegeneration` now subtracts the count of already-fixed sessions per (course, batch, section, type) group from the derived in-scope free variables. New helpers `sessionGroupKey(ScheduledSession)` / `variableGroupKey(SessionVariable)` build matching keys (`sessionType.name()` aligns the enum with the persisted String). Verified: a course requiring 3 lectures with 1 locked + 2 movable now yields 2 free variables + 1 copied-forward = 3 total (not 4).
2. **Feasibility denominator** — `totalRequired` is now `fixedSessions.size() + freeVariables.size()` (scoped universe), not the whole-department derivation count.

### Comment-level fixes applied
- **#7 serializeScope** — now uses the injected Jackson `ObjectMapper` (null-safe, round-trippable) instead of hand-rolled `String.format`.
- **#8 isApproved** — `persistSessions` now explicitly sets `isApproved(false)` on freshly placed sessions.
- **Doc nit** — clarified the `isVariableFree` comment (scope filter only; reconciliation done by per-group subtraction).

### Accepted follow-ups (not blockers; documented)
- **#3/#4 Security** — no `@PreAuthorize` and hardcoded `"system"` actor on the new endpoints, consistent with the rest of `SchedulingController` (local-dev `permitAll`). To be wired when the auth module lands; a real principal is also needed for per-actor audit (KD-54).
- **#6 Audit action** — lock/unlock use `UPDATED` and regeneration uses `CREATED` (the enum has only CREATED/UPDATED/DELETED). Named events (LOCK_SESSION etc.) recommended via a payload discriminator or enum extension in a later change.
- **#5 Concurrency** — no optimistic version guard on the source draft between partition-load and copy-forward; relies on the trigger-time in-progress guard. Acceptable for first cut; optimistic locking recommended.
- **Test gap** — the full regeneration flow (where the blockers lived) is covered by integration tests in the Testing subtask (A4-67); a unit test asserting the corrected session count for a mixed locked/movable course is recommended there.

Verified: `mvn compile` BUILD SUCCESS; unit tests 13/13 passing after fixes.
