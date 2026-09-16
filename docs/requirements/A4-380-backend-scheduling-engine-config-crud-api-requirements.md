# A4-380 — Backend: Scheduling Engine Configuration CRUD API — Requirements

**Jira:** A4-380 (Story) · **Type:** Backend · **Story Points:** 5
**Epic:** A4-1 (UTMS) · **Requirement Generation Subtask:** A4-381
**Blocks:** A4-340 (frontend admin panel) · **Closes:** A4-11 OQ-D3
**Status:** Draft for Lead review

---

## 1. Introduction

This document specifies the requirements for REST CRUD APIs to manage the scheduling engine's three configuration data sets: **session-derivation rules**, **soft-constraint weights**, and **institution common slots (CCC/UWE)**. The three tables already exist (Flyway migration V10) and are today **read-only** inputs to the scheduling engine — only `SchedulingDataLoader` reads them, and there is no CRUD REST API. This story adds that API so the data can be managed through the application, and is the backend prerequisite for the frontend admin panel A4-340.

Scope is backend only: controllers, services, DTOs, mappers, validation, persistence, and audit. The frontend UI is A4-340. The scheduling engine's read/consumption logic is unchanged.

---

## 2. User Story

*As a* Department Coordinator / Administrator (acting through the admin UI),
*I want* REST CRUD APIs for session-derivation rules, soft-constraint weights, and institution common slots,
*so that* these scheduling-engine inputs can be created, viewed, updated, and removed through the application instead of by direct database edits.

**BRD traceability:**
- BRD 7.7 — mixed slot durations (session-derivation rules map L-T-P components to sessions across durations).
- BRD 7.7 — institution-level common slots (CCC/UWE).
- BRD 6.10 — engine feasibility/quality scoring (soft-constraint weights feed the quality score).

**Design decisions this API serves (A4-11):** PD-74 (derivation rules per campus, incl. slot_duration_minutes), PD-70 (weights in DB config, default equal), KD-52 / OQ-D3 (institution common slots — CRUD ownership, which this story closes).

---

## 3. Actors

| Actor | Interaction |
|---|---|
| Coordinator / Administrator (via A4-340 UI) | Calls the CRUD endpoints to manage config. |
| Scheduling Engine (`SchedulingDataLoader`) | Existing downstream reader of the three tables at generation time. Not a caller of this API; the reason the data integrity matters. |

> Role-based authorization (which role may call which endpoint) is deferred to the Auth/RBAC module; see Section 16 OQ-4. This story applies the codebase's current controller security convention (a `@PreAuthorize`/TODO placeholder as used by existing master-data controllers).

---

## 4. User Journeys (API-level)

### Journey A — List
1. Client GETs an entity's list endpoint with a `campusId` filter (paged).
2. API returns active (non-deleted) records for that campus in the standard `{ data, meta }` envelope.

### Journey B — Create
1. Client POSTs a create payload.
2. API validates fields, campus existence, enum membership (weights), FK existence (common slots), and uniqueness.
3. On success → 201 with the created record; an audit event is recorded.
4. On duplicate `(campus, type)` → 409. On invalid field/enum → 400 with field-level details. On missing campus/slot → 404 or 400 (see FR-6).

### Journey C — Update
1. Client PUTs to `/{id}` with an update payload.
2. API loads the active record (404 if not found), re-validates, applies changes.
3. On success → 200 with the updated record; audit event recorded.
4. Validation failures mirror Create.

### Journey D — Delete
1. Client DELETEs `/{id}`.
2. API loads the active record (404 if not found) and **soft-deletes** it (sets `deleted_at`, `is_active = false`).
3. On success → 204; audit event recorded. The record no longer appears in list results.

---

## 5. Functional Requirements

### FR-1 — Common CRUD surface (all three entities)
- FR-1.1 Each entity SHALL expose: list (GET, paged, `campusId` filter), create (POST → 201), update (PUT `/{id}` → 200), delete (DELETE `/{id}` → 204 soft-delete).
- FR-1.2 Endpoints SHALL live under `/api/v1/scheduling-config/<resource>` per api-standards (kebab-case plural). Proposed resources: `derivation-rules`, `soft-constraint-weights`, `common-slots`.
- FR-1.3 List responses SHALL use the standard `{ data, meta }` paged envelope (default page size 20, max 100).
- FR-1.4 List SHALL return only active records (`deleted_at IS NULL`).
- FR-1.5 All input SHALL be validated at the controller with Jakarta Validation (allowlist-based); errors SHALL return 400 with field-level `details[]` and never expose internals.
- FR-1.6 Create/Update/Delete SHALL each record an audit event (via `AuditEventPublisher`) consistent with existing master-data services, within the same transaction as the mutation.

### FR-2 — Session-Derivation Rules
- FR-2.1 Fields: `campusId` (required), `componentType` (required, ≤10 chars), `slotDurationMinutes` (required, positive integer), `hoursPerSession` (required, decimal, one fractional digit — DECIMAL(3,1)), `description` (optional, ≤200 chars).
- FR-2.2 SHALL enforce uniqueness on `(campusId, componentType)` for active records → duplicate create/update returns 409.
- FR-2.3 SHALL verify `campusId` refers to an existing campus.
- FR-2.4 `componentType` allowed value set — see OQ-5 (proposed L/T/P).

### FR-3 — Soft-Constraint Weights
- FR-3.1 Fields: `campusId` (required), `constraintType` (required, ≤40 chars), `weight` (required, decimal, two fractional digits — DECIMAL(4,2), entity default 1.00).
- FR-3.2 `constraintType` SHALL be validated against the `SoftConstraintType` enum (A4-11): `FACULTY_TIME_PREFERENCE`, `FACULTY_DISTRIBUTION_PREFERENCE`, `ROOM_PROXIMITY`, `GAP_MINIMIZATION`, `SOFT_BLOCK_OVERRIDE`, `DAY_PATTERN_BALANCE`. An invalid value returns 400.
- FR-3.3 SHALL enforce uniqueness on `(campusId, constraintType)` for active records → 409 on duplicate.
- FR-3.4 SHALL verify `campusId` exists.
- FR-3.5 `weight` acceptable business range — see OQ-2 (column allows 0.00–99.99; business min/max unspecified).

### FR-4 — Institution Common Slots (CCC/UWE)
- FR-4.1 Fields: `campusId` (required), `name` (required, ≤100 chars), `dayOfWeek` (required, ≤10 chars), `slotDefinitionId` (required, FK), `appliesToAllBatches` (boolean, default true).
- FR-4.2 SHALL verify `campusId` exists and `slotDefinitionId` refers to an existing slot definition (FK integrity).
- FR-4.3 `dayOfWeek` allowed value set — see OQ-5 (proposed MONDAY–SATURDAY).
- FR-4.4 Uniqueness rule for common slots — see OQ-1 (no DB unique constraint exists today).

### FR-5 — Update semantics
- FR-5.1 Update SHALL operate on an existing active record; a missing/deleted `id` returns 404.
- FR-5.2 Re-validation on update mirrors create (uniqueness excluding the record itself, enum, FK, field rules).
- FR-5.3 Whether `campusId` is mutable on update — see OQ-3 (proposed: immutable).

### FR-6 — Error handling
- FR-6.1 Errors SHALL use the standard error envelope (`timestamp, status, error, message, path, details`).
- FR-6.2 Status mapping: 400 field/enum validation; 404 entity/campus/slot not found (or 400 for bad reference — resolved per handler convention, see OQ-6); 409 uniqueness conflict; 422 reserved for business-rule violations; 500 never exposes stack traces.

---

## 6. Constraints Owned by This Document

| ID | Constraint |
|---|---|
| C-1 | Derivation rule unique per `(campusId, componentType)` among active records. |
| C-2 | Soft-constraint weight unique per `(campusId, constraintType)` among active records. |
| C-3 | `constraintType` must be a valid `SoftConstraintType` enum value. |
| C-4 | `campusId` must reference an existing campus (all three entities). |
| C-5 | `slotDefinitionId` must reference an existing slot definition (common slots). |
| C-6 | Delete is soft (sets `deleted_at`); records are never physically removed. |
| C-7 | Every create/update/delete emits an audit event in the same transaction. |

---

## 7. Constraints Referenced from Other Documents

| Constraint / artifact | Owner | How referenced |
|---|---|---|
| `SoftConstraintType` enum (6 values) | A4-11 scheduling engine | Allowed set for `constraintType` (C-3). |
| Campus entity | Master data (campus) | Existence check (C-4). |
| Slot definitions | Time-slot grid module | FK existence (C-5). |
| Read/consumption of these tables | A4-11 `SchedulingDataLoader` | Downstream reader; behavior unchanged by this story. |
| V10 schema (tables, unique constraints, indexes) | Migration V10 | Physical model this API maps to. |

---

## 8. Validation Rules

| Entity | Field | Rule |
|---|---|---|
| Derivation rule | campusId | required; existing campus |
| Derivation rule | componentType | required; ≤10 chars; allowed set OQ-5 |
| Derivation rule | slotDurationMinutes | required; integer > 0 |
| Derivation rule | hoursPerSession | required; decimal, one fractional digit; fits DECIMAL(3,1) |
| Derivation rule | description | optional; ≤200 chars |
| Weight | campusId | required; existing campus |
| Weight | constraintType | required; ≤40 chars; ∈ SoftConstraintType |
| Weight | weight | required; decimal, two fractional digits; fits DECIMAL(4,2); business range OQ-2 |
| Common slot | campusId | required; existing campus |
| Common slot | name | required; ≤100 chars |
| Common slot | dayOfWeek | required; ≤10 chars; allowed set OQ-5 |
| Common slot | slotDefinitionId | required; existing slot definition |
| Common slot | appliesToAllBatches | boolean; default true |

---

## 9. Non-Functional Requirements

- **NFR-1 (Performance):** All read endpoints < 500ms (api-standards); config volumes are small. List endpoints paginate — no unbounded queries.
- **NFR-2 (Security):** Parameterized queries only (JPA/Specifications). Allowlist input validation. No secrets/stack traces in responses. Endpoints require authentication; role authorization applied when Auth module lands (OQ-4).
- **NFR-3 (Auditability):** Mutations logged via audit trail in the same transaction (aligns with the audit service pattern).
- **NFR-4 (Consistency):** Follow the existing Asset CRUD layering (Controller → Service → Repository; MapStruct mapper with `BaseMapperConfig`; `GlobalExceptionHandler`).
- **NFR-5 (Documentation):** Springdoc OpenAPI annotations on all endpoints/DTOs.

---

## 10. Acceptance Criteria (Given / When / Then)

1. **Given** active config records for a campus, **When** a client GETs each list endpoint with `campusId`, **Then** a paged list of active records is returned in the `{ data, meta }` envelope.
2. **Given** a valid create payload, **When** a client POSTs, **Then** the record is created (201) and returned, and an audit event is recorded.
3. **Given** a duplicate `(campus, componentType)` or `(campus, constraintType)`, **When** a client POSTs, **Then** the API returns 409 without exposing internals. *(error path)*
4. **Given** a valid update payload for an existing record, **When** a client PUTs `/{id}`, **Then** the record is updated (200) and an audit event is recorded.
5. **Given** an existing record, **When** a client DELETEs `/{id}`, **Then** it is soft-deleted (204) and no longer appears in list results.
6. **Given** a soft-constraint weight with a `constraintType` not in `SoftConstraintType`, **When** a client POSTs/PUTs, **Then** the API returns 400 with a field-level error. *(edge case)*
7. **Given** a create payload referencing a non-existent `campusId` (or `slotDefinitionId` for common slots), **When** a client POSTs, **Then** the API rejects it with a clear error and does not persist. *(edge case)*

---

## 11. Data Model (conceptual — existing V10 tables)

All three extend `BaseEntity` (id, is_active, created_at/by, updated_at/by, deleted_at).

- **session_derivation_rules:** campus_id, component_type, slot_duration_minutes, hours_per_session, description. Unique `(campus_id, component_type)`.
- **soft_constraint_weights:** campus_id, constraint_type, weight. Unique `(campus_id, constraint_type)`.
- **institution_common_slots:** campus_id, name, day_of_week, slot_definition_id (FK → slot_definitions), applies_to_all_batches. No unique constraint today (OQ-1).

No schema change is required by the core scope; any new uniqueness constraint (OQ-1) would need an additive migration.

---

## 12. Dependencies

| Dependency | Type | Status | Notes |
|---|---|---|---|
| V10 config tables | Hard | Exists | Physical model. |
| Campus master data | Hard | Available | Existence check. |
| Slot definitions (time-slot grid) | Hard | Available | FK for common slots. |
| `SoftConstraintType` enum (A4-11) | Reference | Available | Enum validation. |
| Audit service / `AuditEventPublisher` | Hard | Available | Audit events. |
| Asset CRUD pattern | Reference | Available | Layering template. |

---

## 13. Assumptions

1. A1 — The three tables and their constraints (V10) are the source of truth; this API maps to them without schema changes (except a possible additive uniqueness migration if OQ-1 resolves yes).
2. A2 — Campus existence and slot-definition existence can be checked via existing repositories.
3. A3 — Soft-delete semantics (BaseEntity `deleted_at`) apply; no hard deletes.
4. A4 — The `SoftConstraintType` enum is the authoritative allowed set for `constraintType`.
5. A5 — Authentication exists at the API layer; role authorization is applied later (OQ-4), matching current master-data controller convention.

---

## 14. Consistency Notes

- The V10 unique constraints (`uq_derivation_campus_type`, `uq_weight_campus_type`) are enforced by C-1/C-2; the service performs an explicit duplicate check → 409 (matching the Asset pattern) in addition to the DB constraint.
- `institution_common_slots` has **no** DB unique constraint (confirmed in V10). This document does not invent one; OQ-1 asks whether one is needed.
- `constraintType` is a plain `String` column but is functionally an enum (SchedulingDataLoader calls `SoftConstraintType.valueOf`). C-3 makes the API validate it so invalid values can't be persisted (which would otherwise break the engine read).
- All TBDs in FRs are carried into Section 16.

---

## 15. Out of Scope

- Frontend UI (A4-340).
- Changes to how the scheduling engine consumes this config, or the engine algorithm.
- RBAC role definitions (Auth module).
- Bulk import/export of configuration.
- Seeding of default/production data (dev seed already exists per A4-11).

---

## 16. Open Questions

| # | Question | Owner | Notes / Proposed default |
|---|---|---|---|
| OQ-1 | Should `institution_common_slots` have a uniqueness constraint (e.g., `(campus_id, day_of_week, slot_definition_id)`)? None exists today. | Product Owner / System Design | Proposed: add `(campus_id, day_of_week, slot_definition_id)` unique for active rows [confirm]. If yes → additive migration. |
| OQ-2 | Acceptable business range for `weight`? Column is DECIMAL(4,2) (0.00–99.99); no BRD min/max. | Product Owner | Proposed 0.00–10.00 [confirm] (matches A4-340 design PD-77, still provisional). |
| OQ-3 | Is `campusId` mutable on update, or fixed once created? | Product Owner / System Design | Proposed: immutable (change = delete + recreate) [confirm]. |
| OQ-4 | Which roles may call these endpoints (Coordinator vs Admin, per entity)? | Product Owner / Auth | Deferred to Auth module; placeholder security applied. |
| OQ-5 | Allowed value sets for `componentType` (L/T/P?) and `dayOfWeek` (MON–SAT?), and where they are authoritatively defined. | System Design | Proposed static sets; consider enums to match the engine. |
| OQ-6 | For a non-existent referenced `campusId`/`slotDefinitionId`, return 404 or 400? | Lead | Proposed: 404 for path-id lookups, 400/validation-style for body references [confirm]. |

---

## 17. Traceability

| BRD / Design ref | Requirement here | Verified |
|---|---|---|
| BRD 7.7 mixed slot durations | FR-2 (derivation rules) | Manages duration→session mapping data. |
| BRD 7.7 CCC/UWE common slots | FR-4 (common slots) | CRUD for institution common slots. |
| BRD 6.10 quality scoring | FR-3 (weights) | Manages soft-constraint weights feeding the score. |
| A4-11 PD-74 | FR-2 fields | Per-campus derivation rules incl. slot duration. |
| A4-11 PD-70 | FR-3 | Weights in DB config. |
| A4-11 KD-52 / OQ-D3 | FR-4 + story intent | Closes common-slot CRUD ownership. |
| A4-340 dependency | Story "Blocks A4-340" | This API is the frontend's prerequisite. |

---

*Prepared for Lead review (A4-381). Open questions OQ-1, OQ-2, OQ-3, OQ-5, OQ-6 should be resolved during/after review so the design can pin the contract.*
