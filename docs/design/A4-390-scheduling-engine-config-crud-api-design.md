# A4-390 — Scheduling Engine Configuration CRUD API — Design

| | |
|---|---|
| **Story** | A4-390 |
| **Design subtask** | A4-392 |
| **Requirement** | A4-391 (Approved) |
| **Epic** | A4-1 (UTMS) |
| **Role** | Backend |
| **Blocks** | A4-340 (Frontend Configuration Admin Panel) |

---

## 1. Overview

Adds a REST CRUD API layer over three **existing** scheduling-engine configuration tables (`session_derivation_rules`, `soft_constraint_weights`, `institution_common_slots`, migration V10). No schema or migration changes. The design follows the established master-data CRUD pattern in this codebase (see `AcademicCalendar*`): layered Controller → Service → Repository, MapStruct DTO mapping via `BaseMapperConfig`, soft-delete, and audit events published in the same transaction via `AuditEventPublisher`.

The entities, their repositories (`SessionDerivationRuleRepository`, `SoftConstraintWeightRepository`, `InstitutionCommonSlotRepository` — all already extend `JpaRepository` + `JpaSpecificationExecutor`), and `BaseEntity` already exist. This story adds: request/response DTOs, MapStruct mappers, three services, three controllers, and validation.

## 2. Architecture

```
A4-340 UI ──HTTP──> {Entity}Controller ──> {Entity}Service ──> {Entity}Repository ──> Postgres (V10 tables)
                          │                     │
                     @Valid DTO            AuditEventPublisher (same @Transactional)
                          │                     │
                     {Entity}Mapper        CampusRepository / SlotDefinitionRepository (FK existence checks)
```

Three parallel, near-identical CRUD slices (one per entity), placed in the existing `com.utms.scheduling.engine` module under a new `config` sub-package to keep them beside the tables they serve:
```
com.utms.scheduling.engine.config/
  SessionDerivationRuleController / ...Service / ...Mapper / SessionDerivationRuleDto / CreateSessionDerivationRuleRequest / UpdateSessionDerivationRuleRequest
  SoftConstraintWeightController / ...Service / ...Mapper / SoftConstraintWeightDto / Create... / Update...
  InstitutionCommonSlotController / ...Service / ...Mapper / InstitutionCommonSlotDto / Create... / Update...
  ComponentType (enum), DayOfWeek reuse
```
Repositories/entities remain in `...engine.entity` / `...engine.repository` (already there).

## 3. API Endpoints

All under `/api/v1`. Response envelope follows the established codebase convention `{"data": ...}` (see KD-1). Status codes per api-standards.

### Session-derivation rules — `/api/v1/session-derivation-rules`
| Method | Path | Body | Success | Errors |
|--------|------|------|---------|--------|
| GET | `?campusId={id}&page=&size=&sort=` | — | 200 `{data:[...], meta:{...}}` | 400 (bad param) |
| GET | `/{id}` | — | 200 `{data:{...}}` | 404 |
| POST | `/` | CreateSessionDerivationRuleRequest | 201 `{data:{...}}` | 400, 404 (campus), 409 (dup) |
| PUT | `/{id}` | UpdateSessionDerivationRuleRequest | 200 `{data:{...}}` | 400, 404, 409 |
| DELETE | `/{id}` | — | 204 | 404 |

### Soft-constraint weights — `/api/v1/soft-constraint-weights`
Same verb/shape matrix. Uniqueness: (campusId, constraintType).

### Institution common slots — `/api/v1/institution-common-slots`
Same verb/shape matrix. No uniqueness (PD-91). FK existence checks on campus + slotDefinition.

**Example error (409 duplicate):**
```json
{ "timestamp":"...","status":409,"error":"Conflict",
  "message":"Session derivation rule already exists for campus 1, componentType 'LECTURE'",
  "path":"/api/v1/session-derivation-rules","details":[] }
```
**Example error (400 bad enum):**
```json
{ "timestamp":"...","status":400,"error":"Bad Request","message":"Validation failed",
  "path":"/api/v1/soft-constraint-weights",
  "details":[{"field":"constraintType","message":"must be one of [FACULTY_TIME_PREFERENCE, ...]","rejectedValue":"FOO"}] }
```

## 4. DTOs and Validation

Request DTOs use Jakarta Validation (allowlist, server-side). `componentType` and `constraintType` validated as strict enums so an invalid value can never reach the engine's `valueOf`/filter (CR-1, CR-2).

### CreateSessionDerivationRuleRequest
| Field | Type | Constraints |
|-------|------|-------------|
| campusId | Long | `@NotNull` |
| componentType | `ComponentType` enum | `@NotNull`; LECTURE / TUTORIAL / PRACTICAL (CR-1) |
| slotDurationMinutes | Integer | `@NotNull @Min(1) @Max(600)` (PD-93) |
| hoursPerSession | BigDecimal | `@NotNull @DecimalMin("0.5") @DecimalMax("8.0")` (PD-93); DECIMAL(3,1) |
| description | String | `@Size(max=200)` |
| isActive | Boolean | optional, default true |

### CreateSoftConstraintWeightRequest
| Field | Type | Constraints |
|-------|------|-------------|
| campusId | Long | `@NotNull` |
| constraintType | `SoftConstraintType` enum (reused from engine) | `@NotNull`; one of the 6 values (CR-2) |
| weight | BigDecimal | `@NotNull @DecimalMin("0.00") @DecimalMax("99.99")` (PD-93); DECIMAL(4,2) |
| isActive | Boolean | optional, default true |

### CreateInstitutionCommonSlotRequest
| Field | Type | Constraints |
|-------|------|-------------|
| campusId | Long | `@NotNull` |
| name | String | `@NotBlank @Size(max=100)` |
| dayOfWeek | `DayOfWeek` enum | `@NotNull` (PD-94: strict MON..SUN) |
| slotDefinitionId | Long | `@NotNull` |
| appliesToAllBatches | Boolean | optional, default true |
| isActive | Boolean | optional, default true |

Update requests mirror Create; `campusId`, `componentType`/`constraintType` treated as immutable identity fields on update (changing them = create a new logical row) — see KD-2. Response DTOs (`{Entity}Dto`) expose all fields plus `id`, `isActive`, audit timestamps.

## 5. Service Logic (per entity, identical shape)

```
create(request):                          [@Transactional]
  1. verify campus exists (CampusRepository.findByIdAndDeletedAtIsNull) else EntityNotFoundException("Campus", id)
  2. (common-slot only) verify slotDefinition exists else EntityNotFoundException
  3. (derivation/weight) if existsByCampusId{ComponentType|ConstraintType}AndDeletedAtIsNull -> ConflictException (409)  [CO-1/CO-2]
  4. map request -> entity (MapStruct), set isActive
  5. save
  6. auditEventPublisher.publish(new AuditEvent("{Entity}", id, CREATED, null, entity, currentUser, Instant.now()))
  7. return mapper.toDto(entity)

findById(id):        [@Transactional(readOnly)] findByIdAndDeletedAtIsNull else 404 -> toDto
list(campusId, pageable): [@Transactional(readOnly)] Specification (campusId eq + deletedAt IS NULL) -> Page -> toDto page
update(id, request): [@Transactional] load active-or-404; re-check uniqueness if identity unchanged; apply mutable fields; save; audit UPDATED
delete(id):          [@Transactional] load active-or-404; setDeletedAt(now); setIsActive(false); save; audit DELETED
```

Current user comes from the security context; until the auth module lands, this falls back to `"system"` consistent with the existing `AcademicCalendarService` (KD-3).

## 6. Key Decisions

- **KD-1 Response envelope:** use the established `{"data": ...}` / `{"data":[...],"meta":{...}}` map envelope (as `AcademicCalendarController` does), not a new `PagedResponse<T>` type. `PagedResponse<T>` does not exist in the codebase; introducing it is out of scope. **Accepted deviation from api-standards' ideal wrapper** for codebase consistency. For lists, `meta` carries page/size/totalElements/totalPages from Spring `Page`.
- **KD-2 Identity fields immutable on update:** `campusId` + `componentType` (derivation) / `constraintType` (weight) form the unique business key; update does not change them (prevents accidental duplicate-key churn). Changing the type = delete + create.
- **KD-3 Actor source:** `"system"` placeholder until auth module (matches existing services).
- **KD-4 Reuse `SoftConstraintType` enum:** the weight API validates `constraintType` against the existing `com.utms.scheduling.engine.enums.SoftConstraintType` — single source of truth, guarantees the engine's `valueOf` never fails (CR-2).

## 7. Decision Interaction / Consistency (P6 Step 3)

- **Soft-delete vs. unique constraint (real collision).** `uq_derivation_campus_type` and `uq_weight_campus_type` are plain UNIQUE constraints (`UNIQUE (campus_id, component_type)`) with **no** `WHERE deleted_at IS NULL`. So a *soft-deleted* row still occupies the unique key: recreating the same (campus, componentType) after a soft-delete would violate the DB constraint even though the service-level `existsBy...AndDeletedAtIsNull` check passes.
  - **Resolution (KD-5):** the service catches `DataIntegrityViolationException` on save and maps it to a `409 ConflictException` with a clear message ("a previously deleted rule with this key exists; restore it or contact admin"). We do **not** silently hard-delete or alter the constraint (schema change is out of scope). Recreating after delete is an edge case; surfacing a clear 409 is correct and safe. Flagged to the lead as **DQ-1** (whether a future migration should make the unique index partial `WHERE deleted_at IS NULL` to allow re-create — out of scope here).
- **isActive vs deletedAt:** both exposed; the engine reads `deletedAt IS NULL AND isActive = TRUE`. `isActive=false` disables without deleting (AC#7). No conflict.

## 8. Provisional Decisions (pending stakeholder ratification — from requirement OQs)

| PD | Source OQ | Provisional decision |
|----|-----------|----------------------|
| PD-91 | OQ-1 | Institution common slots: **no uniqueness constraint** added (matches V10). Duplicates allowed; UI dedups if needed. Revisit only if stakeholder requires it (future migration). |
| PD-92 | OQ-2 | Delete/deactivate of a derivation rule the engine needs: **allow with a WARN log**, do not hard-block. Generation already throws a clear "No derivation rule for component type" if a required rule is missing. A hard-block guard is deferred (needs cross-checking active generation config). |
| PD-93 | OQ-3 | Numeric bounds: slotDurationMinutes `[1,600]`, hoursPerSession `[0.5,8.0]`, weight `[0.00,99.99]` (DECIMAL(4,2) ceiling). Enforced via Jakarta annotations. |
| PD-94 | OQ-4 | `dayOfWeek`: **strict enum** MONDAY..SUNDAY at the API layer, stored as the enum name (≤10 chars, fits VARCHAR(10)). Aligns with `scheduled_sessions.day_of_week`. |

All four are **provisional** — if the lead rules differently at design approval, the annotations/validation adjust accordingly. None require schema changes.

## 9. Mechanism Completeness (P6 Step 5)

- **Audit (CO-6):** every create/update/delete calls `auditEventPublisher.publish(new AuditEvent(...))` inside the `@Transactional` service method — same pattern and same transaction as `AcademicCalendarService`. Action = CREATED / UPDATED / DELETED. previousValue/newValue populated for update and delete.
- **Referential integrity (CO-4/CO-5):** explicit existence checks (`CampusRepository.findByIdAndDeletedAtIsNull`, slot-definition repository) before persist, throwing `EntityNotFoundException` → 404 mapped by `GlobalExceptionHandler`. DB FK is the backstop.
- **Safe errors (FR-8):** all exceptions flow through the existing `GlobalExceptionHandler` (validation → 400 with field details; EntityNotFound → 404; Conflict → 409); no stack traces exposed.

## 10. Testing Strategy

- **Unit (JUnit 5 + Mockito):** per service — create happy path, duplicate → ConflictException, missing campus/slot → EntityNotFoundException, invalid enum/range handled at DTO validation, soft-delete sets deletedAt+isActive, update immutability of identity fields, audit publish invoked. Target ≥ 80% line coverage.
- **Integration (Testcontainers + Postgres):** per controller — 201/200/204 happy paths, 400 field errors, 409 duplicate (incl. the soft-deleted-key collision → 409, KD-5), 404, list pagination + campus filter excludes soft-deleted. Verifies Flyway V10 tables and real constraints.
- **Security static (SpotBugs/FindSecBugs):** parameterized queries only (JPA/Specification), no injection.

## 11. Cross-Document Consistency (P6 Step 8)

- **Migration versions:** last applied is **V13**. This story adds **no** migration (no schema change). ✓
- **Audit columns / BaseEntity:** all three tables already carry the full BaseEntity column set (verified in V10). Entities already extend `BaseEntity`. ✓
- **Error envelope:** reuses the `{"data":...}` map + `GlobalExceptionHandler` error format established across the codebase. ✓
- **Enum single-source:** reuses `SoftConstraintType` (engine enum), no duplicate definition. ✓
- **PD numbering:** continues from existing backend PDs (…PD-74 seen in V10); new PDs numbered PD-91..PD-94 to avoid collision. ✓

## 12. Traceability

| Requirement | Design element |
|-------------|----------------|
| FR-1 (list) | GET list endpoints + Specification (campusId + deletedAt IS NULL) + Page/meta |
| FR-2 (get) | GET /{id} + findByIdAndDeletedAtIsNull |
| FR-3 (create) | POST + service create (validate→uniqueness→FK→save→audit) |
| FR-4 (update) | PUT + service update (KD-2 identity immutability, isActive toggle) |
| FR-5 (delete) | DELETE + soft-delete (deletedAt + isActive=false) |
| FR-6 (uniqueness) | existsBy...AndDeletedAtIsNull + KD-5 DB-constraint 409 mapping |
| FR-7 (isActive vs delete) | isActive mutable field; engine read filter unchanged; AC#7 |
| FR-8 (safe errors) | GlobalExceptionHandler; enum/range validation at DTO |
| CO-1..CO-6 | uniqueness checks, soft-delete, FK checks, audit-in-transaction |
| CR-1, CR-2 | ComponentType enum, SoftConstraintType enum validation |
| NFR performance | pagination on lists, indexed campus_id lookups (V10 partial indexes) |

## 13. Out of Scope

- Frontend UI (A4-340). Schema/migration changes. RBAC/@PreAuthorize (security module — flagged). Making the unique indexes partial (`WHERE deleted_at IS NULL`) — noted in KD-5/DQ-1 as a possible future migration.

## 14. Open Questions (carried forward)

- **DQ-1 (from OQ-1/KD-5):** Should a future migration make `uq_derivation_campus_type` / `uq_weight_campus_type` partial (`WHERE deleted_at IS NULL`) to cleanly allow recreate-after-delete? Out of scope for A4-390; needs lead decision.
- **DQ-2 (from OQ-2/PD-92):** Confirm allow-with-warning (vs hard-block) for deleting/deactivating a derivation rule an active generation config depends on.
- PD-91, PD-93, PD-94 are provisional pending ratification at design approval.
