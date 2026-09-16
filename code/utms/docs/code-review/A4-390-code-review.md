# Scheduling Engine Configuration CRUD API (A4-390)

Three near-identical REST CRUD slices — derivation rules, soft-constraint weights, and institution common slots — layered over the read-only V10 scheduling-engine config tables. Each slice follows the same controller → service → repository → MapStruct-mapper → DTO shape, mirrors the master-data Asset CRUD pattern (campus existence check, campus-scoped uniqueness, soft-delete, audit events per mutation), and reuses the shared `GlobalExceptionHandler`. The work predates the A4-390 story and is assessed here as the shipped implementation for it.

Watch for: the soft-delete + non-partial unique constraint interaction (KD-5) surfaces a `DataIntegrityViolationException` that is **not** mapped to 409 — it currently returns 500 (confirmed). RBAC is stubbed as `// TODO @PreAuthorize` and deferred to the security module (documented gap, not a blocker). The three slices are copy-paste duplicated with no shared abstraction (confirmed, acceptable for now but worth noting).

**Verdict**: COMMENT

The implementation is standards-clean, correctly wired to the engine's contracts (componentType strings and `SoftConstraintType.valueOf` are both honored), and well tested at the service layer. One real defect (the 409 gap on recreate-after-soft-delete) should be fixed or explicitly ticketed, but it is a narrow edge case behind an admin-only endpoint, so it does not block.

## High-level view

The engine contract alignment is correct. `SessionDeriver.findRule` filters derivation rules by the literal strings `LECTURE`/`TUTORIAL`/`PRACTICAL`, and the `@Pattern` on both create and update requests was moved to `^(LECTURE|TUTORIAL|PRACTICAL)$` — so API-created rules are now visible to the engine. No production code still assumes the old `L`/`T`/`P` form; the only surviving `"L"` literals are in two test fixtures on paths where validation never runs. Separately, `SchedulingDataLoader` does `SoftConstraintType.valueOf(w.getConstraintType())`, which throws on an unknown value; the `SoftConstraintTypeValidator` allowlists against exactly `SoftConstraintType.values()`, so an invalid type is rejected at the API with a 400 before it can ever poison that read.

The soft-delete/unique-constraint seam is the one behavioral gap. The DB constraints `uq_derivation_campus_type` and `uq_weight_campus_type` are plain `UNIQUE (campus_id, <type>)` — not partial on `deleted_at` (contrast with `uq_genreq_dept_semester_inprogress` in the same migration, which is explicitly partial). The services guard inserts with `existsBy...AndDeletedAtIsNull`, so after a soft-delete that guard passes, the insert reaches the DB, and the still-present row for the deleted key triggers a unique violation. `GlobalExceptionHandler` has no `DataIntegrityViolationException` handler, so it falls through to the generic `Exception` branch and returns 500 with "An unexpected error occurred" instead of a clean 409. This is the documented KD-5 design point, and it is confirmed by reading the code.

The API surface is complete and consistent across all three resources: POST/GET-list/GET-by-id/PUT/DELETE, `/api/v1/scheduling-config/*`, paginated list with a `meta` block, 201 on create, 204 on delete, campus filter via query param. Security posture is sound on the data-safety axis (allowlist validation, parameterized JPA/Specification queries only, no secrets, safe error envelope) but has no authorization — every mutating endpoint carries a `// TODO @PreAuthorize` comment, so anyone who can reach the API can write config. That is an accepted deferral to the security module.

Audit coverage is complete: every create/update/delete publishes an `AuditEvent` inside the `@Transactional` service method, so the audit write shares the transaction with the mutation. The audit actor is hardcoded to `"system"` because there is no security context yet — fine for now, but it must become the real principal when auth lands.

Test quality is good for the service layer: 22 unit tests covering happy path, campus-missing, duplicate-type conflict, uniqueness-recheck-on-change vs skip-on-same, not-found, and soft-delete assertions. What's missing is any test for the KD-5 recreate-after-delete path and any controller/validation-layer test proving the `@Pattern` and `@ValidSoftConstraintType` actually reject bad input at 400 (the allowlist is only exercised indirectly).

<details>
<summary>Issues (7)</summary>

1. **Soft-delete recreate returns 500 not 409** (confirmed, medium) — Recreating a `(campus, type)` key after a soft-delete passes the `existsBy...DeletedAtIsNull` guard, then hits the non-partial DB unique constraint; `DataIntegrityViolationException` is unhandled and falls to the generic 500. Either add a `@ExceptionHandler(DataIntegrityViolationException.class)` mapping to 409, or make the two unique constraints partial (`WHERE deleted_at IS NULL`) so recreate succeeds. Pick one and record the decision.
2. **No authorization on any mutating endpoint** (confirmed, documented gap) — All create/update/delete carry `// TODO @PreAuthorize`. Accepted deferral to the security module; ensure a tracking ticket exists so config writes are not left open when the API is exposed.
3. **Audit actor hardcoded to "system"** (confirmed, low) — Every `AuditEvent` uses `"system"` as the actor. Correct today (no security context), but must be wired to the authenticated principal when auth lands, or the audit trail is unattributable.
4. **KD-5 path is untested** (confirmed, low) — No unit test exercises recreate-after-soft-delete. Add one that asserts the chosen behavior (409 or successful recreate) so a future regression is caught.
5. **No controller/validation-layer tests** (confirmed, low) — The `@Pattern` allowlist and `@ValidSoftConstraintType` are only exercised indirectly. Add a lightweight `@WebMvcTest` (or validator unit test) proving bad `componentType`/`constraintType` yields 400 with field-level detail.
6. **Stale `"L"` literals in test fixtures** (confirmed, trivial) — `DerivationRuleServiceTest.delete_existing_softDeletes` builds an entity with `"L"` and `update_notFound` uses `componentType("L")`. Harmless (validation doesn't run on these paths), but misleading now that the contract is `LECTURE`. Update to a valid value to avoid implying `L` is still accepted.
7. **Three-way code duplication** (confirmed, low) — The service CRUD skeleton (findActiveOrThrow, requireCampus, notDeleted/byCampusId specs, audit publish) and the controllers are near-identical across slices. Acceptable at three copies; if a fourth config resource appears, extract an abstract base service / generic controller to stop the drift.

</details>

<details>
<summary>Details</summary>

### Engine contract alignment: componentType and constraintType

Two separate string-typed contracts cross from this API into the engine, and both are honored.

For derivation rules, the engine never uses an enum — `SessionDeriver.derive` calls `findRule("LECTURE", ...)`, `findRule("TUTORIAL", ...)`, `findRule("PRACTICAL", ...)`, and `findRule` matches with `r.getComponentType().equals(componentType)`. If the API persisted `L`/`T`/`P`, those rules would be invisible and the deriver would throw `BusinessRuleViolationException("No derivation rule for component type: ...")`. The `@Pattern` on `CreateDerivationRuleRequest` and `UpdateDerivationRuleRequest` is now `^(LECTURE|TUTORIAL|PRACTICAL)$`, which matches the engine exactly. A search across the scheduling packages confirms no production code still expects the single-letter form; the only `"L"` occurrences are two test fixtures (see issue 6) on the not-found and delete paths where `@Pattern` is never evaluated.

For soft-constraint weights, `SchedulingDataLoader` builds its weight map with `SoftConstraintType.valueOf(w.getConstraintType())`. `valueOf` throws `IllegalArgumentException` on an unknown name, which would break generation at load time. `SoftConstraintTypeValidator` computes its allowlist directly from `SoftConstraintType.values()` (`FACULTY_TIME_PREFERENCE`, `FACULTY_DISTRIBUTION_PREFERENCE`, `ROOM_PROXIMITY`, `GAP_MINIMIZATION`, `SOFT_BLOCK_OVERRIDE`, `DAY_PATTERN_BALANCE`), so the set can never drift from the enum. An invalid value is rejected at the controller as a 400 before persistence. The validator returns `true` for `null` and defers to `@NotBlank` to avoid a duplicate message — a clean split.

### The soft-delete / unique-constraint 409 gap (KD-5)

This is the one confirmed behavioral defect. The relevant migration lines:

```sql
CONSTRAINT uq_derivation_campus_type UNIQUE (campus_id, component_type)
CONSTRAINT uq_weight_campus_type     UNIQUE (campus_id, constraint_type)
```

Neither is partial on `deleted_at`. The same migration shows the authors know how to write a partial unique index when they want one:

```sql
CREATE UNIQUE INDEX uq_genreq_dept_semester_inprogress
    ON generation_requests(department_id, semester)
    WHERE status = 'IN_PROGRESS' AND deleted_at IS NULL;
```

So a soft-deleted row keeps occupying its `(campus, type)` key. The service pre-check is `existsByCampusIdAndComponentTypeAndDeletedAtIsNull` — it only sees live rows, so after a delete it returns `false` and the service proceeds to `repository.save`, which violates the DB constraint. `GlobalExceptionHandler` maps `EntityNotFoundException`→404, `ConflictException`→409, `BusinessRuleViolationException`→422, `MethodArgumentNotValidException`→400, and everything else→500. There is no `DataIntegrityViolationException` branch, so the recreate attempt surfaces as a generic 500. The error envelope stays safe (no stack trace leaked), but a client that legitimately wants to recreate a previously-deleted config type gets an opaque server error instead of an actionable 409 (or a success).

Two clean resolutions: add a `DataIntegrityViolationException` handler returning 409, or make the two constraints partial so recreate simply succeeds and the historical soft-deleted row is retained for audit. The choice depends on whether recreate should resurrect or conflict — a product decision worth recording under KD-5 rather than leaving implicit.

### API surface and standards

All three controllers expose the full CRUD set under `/api/v1/scheduling-config/{derivation-rules|soft-constraint-weights|common-slots}`, use plural kebab-case paths, return 201/204/200 appropriately, and wrap list responses in the `{ data, meta }` envelope with `page/size/totalElements/totalPages`. Pagination flows through Spring `Pageable`, so there are no unbounded queries. The list filter (`campusId`) is applied via typed `Specification` variables assigned before chaining, matching the backend standard that avoids generic-inference failures. All data access is Spring Data JPA / Specification — no string-concatenated queries, no injection surface. DTOs are returned everywhere; entities never leave the service layer.

`CommonSlotService` defaults `appliesToAllBatches` to `true` on both create and update when null, which is a small business rule embedded in the service — reasonable, and it matches the "no uniqueness in core scope (PD-82)" note for that resource (it has no unique constraint and correctly has no uniqueness pre-check).

### Audit completeness

Each mutating method publishes exactly one `AuditEvent` (`CREATED` with new state, `UPDATED` with new state, `DELETED` with prior state) inside the `@Transactional` boundary, so the audit record commits atomically with the mutation. The actor is `"system"` pending the security context (issue 3). Read methods are `@Transactional(readOnly = true)` and publish nothing, as expected.

Note on the delete path: `delete` sets both `deletedAt` and `isActive=false`. The engine loads derivation rules via `findByCampusIdAndIsActiveTrue` and reads weights from live rows, so a soft-deleted config row is correctly excluded from engine loads — the two exclusion mechanisms (`isActive` for the engine, `deletedAt` for the CRUD reads) stay consistent for deletes. They only diverge on the recreate edge case above.

### Test quality vs the ACs

Service-layer coverage is solid and behavior-focused: create happy-path asserts the returned DTO and verifies both `repository.save` and `auditEventPublisher.publish`; campus-missing and slot-definition-missing assert `EntityNotFoundException` and `never().save`; duplicate-type asserts `ConflictException` and `never().save`; update covers the recheck-on-change and skip-on-same branches with `verify(...).existsBy...` and `verify(repository, never()).existsBy...` respectively; delete asserts `deletedAt` set, `isActive=false`, save, and audit. Naming follows `method_scenario_result`. The gaps are the two noted above: no test for the KD-5 recreate path, and nothing proving the validation annotations reject bad input at the HTTP boundary (all current tests drive the service directly, bypassing `@Valid`).

</details>

<details>
<summary>Files reviewed</summary>

- `scheduling/config/DerivationRuleController|Service|Mapper|Dto.java`, `Create|UpdateDerivationRuleRequest.java` — derivation-rule slice
- `scheduling/config/SoftConstraintWeightController|Service|Mapper|Dto.java`, `Create|UpdateSoftConstraintWeightRequest.java` — weight slice
- `scheduling/config/CommonSlotController|Service|Mapper|Dto.java`, `Create|UpdateCommonSlotRequest.java` — common-slot slice
- `scheduling/config/validation/ValidSoftConstraintType.java`, `SoftConstraintTypeValidator.java` — constraint-type allowlist
- `scheduling/engine/service/SessionDeriver.java`, `SchedulingDataLoader.java`, `enums/SoftConstraintType.java` — engine contracts (read for cross-checks)
- `common/exception/GlobalExceptionHandler.java` — error mapping
- `db/migration/V10__create_scheduling_engine_tables.sql` — constraint definitions
- `scheduling/config/*ServiceTest.java` (3 files, 22 tests) — test suite

No git repository present in the workspace, so this review is based on direct reading of the current file state rather than a diff.

</details>


---

## Resolution (author response — akhil jawali)

AI review verdict: **COMMENT** — no blockers. Findings triaged below before human review.

### Fixed in this story

| # | Finding | Severity | Resolution |
|---|---------|----------|------------|
| 1 | Soft-delete recreate returned 500 not 409 (KD-5) | Medium | Added `@ExceptionHandler(DataIntegrityViolationException.class)` to `GlobalExceptionHandler` mapping DB unique/FK violations to a clean **409** with a safe message (no internals leaked). Centralized fix — benefits all three slices without per-service try/catch. Chosen over making the constraints partial (would need a migration; recreate-as-conflict is the safer default, matches KD-5 intent). Verified: 22 tests still pass. |
| 6 | Stale `"L"` literals in two test fixtures | Trivial | Updated `DerivationRuleServiceTest` delete + not-found fixtures to `LECTURE`. |
| (CR-1) | componentType L/T/P → engine-invisible | from requirement | Already fixed earlier this story: `@Pattern` now `^(LECTURE|TUTORIAL|PRACTICAL)$` in both Create/Update requests. Review confirmed no other production code assumes L/T/P. |

### Accepted decisions / deferrals

| # | Finding | Severity | Decision |
|---|---------|----------|----------|
| 2 | No `@PreAuthorize` on mutating endpoints | Documented gap | Accepted deferral to the security module, consistent with all other controllers. Close when auth lands. |
| 3 | Audit actor hardcoded `"system"` | Low | Accepted — correct today; wire to authenticated principal when auth lands. |
| 4 | KD-5 recreate path untested | Low | Deferred to Testing phase (A4-394) integration tests — the real DB constraint only fires against Postgres, not a mocked unit test. The 409 handler is what that test asserts. |
| 5 | No controller/validation (400) tests | Low | Deferred to A4-394 integration tests — the correct layer for `@Valid`/`@Pattern`/`@ValidSoftConstraintType`. |
| 7 | Three-way duplication | Low | Accepted at three copies; extract a base only if a fourth config resource appears. |
| — | componentType mutable on update (vs design KD-2 immutable) | Variance | Accepted variance — functionally safe; `campusId` immutable (PD-84) matches design. Flagged for reviewer awareness. |

### Post-fix verification
- `mvn -Dtest='DerivationRuleServiceTest,SoftConstraintWeightServiceTest,CommonSlotServiceTest' test` → 22 pass, 0 failures/errors.
- `mvn compile` clean. No new migration. Safe error envelope preserved.

**Author verdict after fixes: ready for human review (COMMENT — no blockers).**
