# A4-390 — Scheduling Engine Configuration CRUD API — Requirements

| | |
|---|---|
| **Story** | A4-390 |
| **Requirement subtask** | A4-391 |
| **Epic** | A4-1 (UTMS) |
| **Role** | Backend |
| **Blocks** | A4-340 (Frontend Configuration Admin Panel) |
| **Depends on** | A4-11 (scheduling engine tables — migration V10, already applied) |

---

## 1. Introduction

This document specifies the REST CRUD API for the three scheduling-engine configuration entities that today are **read-only** to the engine: session-derivation rules, soft-constraint weights, and institution common slots. The tables already exist (migration V10); this story adds the API layer (controllers, services, DTOs, mappers, validation, audit) only. No schema or migration changes are in scope. The API is the prerequisite that unblocks the A4-340 frontend admin panel.

## 2. User Story

*As a* Department Coordinator / Administrator, *I want* REST CRUD endpoints for the engine's three configuration entities with validation, campus scoping, and soft-delete, *so that* the configuration admin panel can read and manage this data instead of it being editable only by direct database access.

## 3. Actors

- **Coordinator / Administrator** — manages configuration via the A4-340 UI, which calls this API.
- **Scheduling Engine (A4-11)** — an existing *consumer* that reads these tables. It is not modified by this story, but its read contract constrains what the API may store (see Section 7).

## 4. User Journeys

**J1 — View configuration.** Actor opens a config screen → API lists rows for the selected campus (paginated, soft-deleted excluded) → each row is shown.

**J2 — Add a rule/weight/slot.** Actor submits a create form → API validates (types, ranges, enum membership, uniqueness, FK existence) → on success persists, writes an audit event in the same transaction, returns 201 → row appears.

**J3 — Edit.** Actor edits a row → API validates the same rules → updates the row and audit trail → returns updated resource.

**J4 — Delete.** Actor deletes a row → API soft-deletes (sets `deleted_at`) → row disappears from list/get but is retained for audit.

**J5 — Error paths.** Invalid payload → 400 with field-level errors; duplicate uniqueness key → 409; non-existent campus/slot reference → 400/409; not-found id → 404. No stack traces or internals in any response.

## 5. Functional Requirements

Organized by behavior. All endpoints live under `/api/v1`.

### FR-1: List (per entity)
- FR-1.1 Return a paginated list filtered by `campusId`.
- FR-1.2 Exclude soft-deleted rows (`deleted_at IS NULL`) — consistent with the engine read path.
- FR-1.3 Support standard pagination parameters (page, size) and a stable sort.

### FR-2: Get by id (per entity)
- FR-2.1 Return a single non-soft-deleted row by id, or 404 if absent/soft-deleted.

### FR-3: Create (per entity)
- FR-3.1 Validate required fields, types, ranges, and enum membership server-side before persisting.
- FR-3.2 Enforce uniqueness where the table defines it (see FR-6).
- FR-3.3 Enforce referential integrity: reject references to a non-existent campus (all three) or slot definition (common slots).
- FR-3.4 On success, persist the row, write an audit event **in the same transaction**, and return 201 with the created resource.

### FR-4: Update (per entity)
- FR-4.1 Apply the same validation, uniqueness, and FK rules as create.
- FR-4.2 Update mutable fields; return the updated resource. 404 if the target row does not exist or is soft-deleted.
- FR-4.3 Allow toggling `isActive` (logical enable/disable) independently of soft-delete (see FR-7).

### FR-5: Delete (per entity)
- FR-5.1 Soft-delete only (set `deleted_at`); never hard-delete.
- FR-5.2 A soft-deleted row is excluded from subsequent list/get and is retained in the table for audit.

### FR-6: Uniqueness enforcement
- FR-6.1 Session-derivation rules: unique per (`campusId`, `componentType`) — reject duplicates with 409 (`uq_derivation_campus_type`).
- FR-6.2 Soft-constraint weights: unique per (`campusId`, `constraintType`) — reject duplicates with 409 (`uq_weight_campus_type`).
- FR-6.3 Institution common slots: the table defines **no** uniqueness constraint. This API does not invent one. See OQ-1.

### FR-7: Active flag vs soft-delete
- FR-7.1 `isActive` is a distinct concept from soft-delete. The engine reads only rows where `deleted_at IS NULL AND is_active = TRUE`.
- FR-7.2 The API exposes `isActive` as a mutable field so an admin can disable a rule/weight/slot without deleting it.

### FR-8: Safe error responses
- FR-8.1 All error responses use the project's standard error envelope, contain field-level detail for validation failures, and never expose stack traces, SQL, or internal paths (org security standard, GlobalExceptionHandler).

## 6. Constraints Owned by This Document

| ID | Constraint | Definition |
|----|-----------|------------|
| CO-1 | Derivation uniqueness | One active derivation rule per (campus, componentType). DB: `uq_derivation_campus_type`. Violation → 409. |
| CO-2 | Weight uniqueness | One active weight per (campus, constraintType). DB: `uq_weight_campus_type`. Violation → 409. |
| CO-3 | Soft-delete semantics | Delete sets `deleted_at`; excluded from reads; retained for audit. |
| CO-4 | Campus FK integrity | `campusId` must reference an existing campus (all three entities). |
| CO-5 | Slot-definition FK integrity | Common-slot `slotDefinitionId` must reference an existing slot definition. |
| CO-6 | Audit-in-transaction | Every create/update/delete writes an audit event in the same DB transaction as the mutation. |

## 7. Constraints Referenced from Other Documents

| ID | Owner | Contract |
|----|-------|----------|
| CR-1 | A4-11 SessionDeriver | `componentType` must be one of **LECTURE, TUTORIAL, PRACTICAL** (the engine filters derivation rules by these literal values; any other value is never read). The API MUST restrict `componentType` to this set. |
| CR-2 | A4-11 SchedulingDataLoader | `constraintType` must be a valid `SoftConstraintType` enum name: **FACULTY_TIME_PREFERENCE, FACULTY_DISTRIBUTION_PREFERENCE, ROOM_PROXIMITY, GAP_MINIMIZATION, SOFT_BLOCK_OVERRIDE, DAY_PATTERN_BALANCE**. The loader calls `SoftConstraintType.valueOf(constraintType)`; an unknown string would crash generation. The API MUST validate against this enum. |
| CR-3 | Master data (campus) | `campuses.id` — existence checked, not managed here. |
| CR-4 | Time-slot grid | `slot_definitions.id` — existence checked, not managed here. |

## 8. Validation Rules

| Entity | Field | Rule |
|--------|-------|------|
| Derivation rule | campusId | required; existing campus |
| | componentType | required; one of LECTURE / TUTORIAL / PRACTICAL (CR-1) |
| | slotDurationMinutes | required; positive integer [range bounds — see OQ-3] |
| | hoursPerSession | required; DECIMAL(3,1), positive [range bounds — see OQ-3] |
| | description | optional; ≤ 200 chars |
| | isActive | optional; boolean (default true) |
| Weight | campusId | required; existing campus |
| | constraintType | required; valid SoftConstraintType enum name (CR-2) |
| | weight | required; DECIMAL(4,2), default 1.00 [non-negative — see OQ-3] |
| | isActive | optional; boolean (default true) |
| Common slot | campusId | required; existing campus |
| | name | required; ≤ 100 chars |
| | dayOfWeek | required; ≤ 10 chars; valid day-of-week value [enum strictness — see OQ-4] |
| | slotDefinitionId | required; existing slot definition (CO-5) |
| | appliesToAllBatches | optional; boolean (default true) |
| | isActive | optional; boolean (default true) |

## 9. Non-Functional Requirements

- **Performance:** list/get/create/update/delete < 500ms (backend-standards read target). Pagination on all list endpoints — no unbounded queries.
- **Security:** parameterized queries (JPA), server-side allowlist validation, no internal detail in errors. RBAC/@PreAuthorize is **deferred to the security module**, consistent with the existing scheduling controllers (flagged, not implemented here).
- **Audit:** create/update/delete recorded via the existing audit mechanism in the same transaction (CO-6).
- **Consistency:** entities extend BaseEntity; DTOs via MapStruct with BaseMapperConfig; layered controller/service/repository per backend-standards.

## 10. Acceptance Criteria

1. **Given** config rows exist, **When** a client GETs an entity list filtered by campus, **Then** only non-soft-deleted rows for that campus are returned, paginated.
2. **Given** a valid create payload, **When** a client POSTs it, **Then** the row is persisted, an audit event is written in the same transaction, and 201 with the created resource is returned.
3. **Given** a duplicate uniqueness key (campus+componentType, or campus+constraintType), **When** a client POSTs it, **Then** the API returns 409 and no row is created.
4. **Given** a payload referencing a non-existent campus (any entity) or slot definition (common slot), **When** a client POSTs it, **Then** the API returns 400/409 with a field-level error and no row is created.
5. **Given** an existing row, **When** a client deletes it, **Then** it is soft-deleted (`deleted_at` set), excluded from subsequent list/get, and retained for audit.
6. **Given** an invalid payload (missing required field, bad `componentType`/`constraintType` enum, out-of-range value), **When** a client submits it, **Then** the API returns 400 with field-level errors and no stack trace or internal detail.
7. **Given** an active derivation rule, **When** a client sets `isActive=false` via update, **Then** the row is retained but excluded from the engine's active-rule read (deactivate without delete).

## 11. Data Model (conceptual — no new tables)

Three existing tables, each extending BaseEntity (id, is_active, created_at, updated_at, created_by, updated_by, deleted_at):
- **session_derivation_rules** (campus_id, component_type, slot_duration_minutes, hours_per_session, description) — UNIQUE (campus_id, component_type).
- **soft_constraint_weights** (campus_id, constraint_type, weight) — UNIQUE (campus_id, constraint_type).
- **institution_common_slots** (campus_id, name, day_of_week, slot_definition_id, applies_to_all_batches) — FK campus, FK slot_definition; no unique constraint.

## 12. Dependencies

- A4-11 tables (migration V10) — applied.
- Existing campus and slot_definition tables (master data / time-slot grid).
- Existing audit mechanism and GlobalExceptionHandler.

## 13. Assumptions

1. The three tables and their columns are final for this story; no schema change is needed to expose CRUD.
2. `componentType` and `constraintType` are effectively enums bounded by the engine's read contract (CR-1, CR-2), even though the DB stores them as VARCHAR.
3. Existing SoftConstraintViolation/generation flows are unaffected — this story only adds write access to configuration inputs.

## 14. Consistency Notes

- **Corrected during analysis:** the parent story text referenced component types as "L/T/P". The engine (`SessionDeriver`) actually filters by the literal strings **LECTURE / TUTORIAL / PRACTICAL**. This document uses the engine's actual values (CR-1). The design/implementation must follow LECTURE/TUTORIAL/PRACTICAL, not single letters.
- `isActive` (logical enable) and `deleted_at` (soft-delete) are separate and both exposed; the engine's read filter uses both (`deleted_at IS NULL AND is_active = TRUE`).
- Common slots have no uniqueness constraint in V10; this document deliberately does not assert one (OQ-1).

## 15. Out of Scope

- Frontend UI (A4-340).
- Schema/migration changes (tables already exist).
- RBAC enforcement (security module).
- Any change to engine generation logic or the read queries.

## 16. Open Questions

| # | Question | Impact | Proposed default |
|---|----------|--------|------------------|
| OQ-1 | Should institution common slots be unique per (campus, name) or (campus, day, slot_definition)? The DB has no such constraint today. | Duplicates allowed at DB level; UI may show apparent duplicates. | Confirm with stakeholder; if desired, a follow-up migration adds the constraint (out of scope here). |
| OQ-2 | On delete/deactivate of a derivation rule the engine requires (LECTURE/TUTORIAL/PRACTICAL), generation will throw "No derivation rule for component type". Should the API block delete/deactivate of the last active rule for a required component type? | Could make generation fail silently until noticed. | Recommend a guard/warning; confirm whether to hard-block or allow with warning. |
| OQ-3 | Numeric range bounds for `slotDurationMinutes`, `hoursPerSession`, and `weight` — the DB has no CHECK constraints. | Without bounds, nonsensical values (0, negative, huge) could be stored. | Propose: slotDurationMinutes 1–600, hoursPerSession 0.5–8.0, weight 0.00–99.99 (DECIMAL(4,2) max). Confirm. |
| OQ-4 | Should `dayOfWeek` be a strict enum (MONDAY…SUNDAY) at the API layer, matching how scheduled_sessions store day_of_week? | Free-text days could mismatch engine expectations. | Recommend strict enum aligned with the engine's day representation. |

## 17. Traceability

| BRD | Requirement | FR mapping |
|-----|-------------|-----------|
| 7.7 (mixed slot durations) | Session-derivation rules govern L-T-P→session mapping across durations | FR-1..FR-8 (derivation entity), CR-1 |
| 7.7 (institution common slots CCC/UWE) | Manage institution common slots | FR-1..FR-8 (common-slot entity), CO-5 |
| 6.10 (feasibility/quality scoring) | Soft-constraint weights configure the quality score | FR-1..FR-8 (weight entity), CR-2 |
| Org security standard | Safe errors, parameterized queries, audit | FR-8, NFR (Section 9), CO-6 |

*Note:* This story is the backend enabler for A4-340; every FR exists to let the A4-340 UI read/manage the three configuration inputs the engine already consumes.
