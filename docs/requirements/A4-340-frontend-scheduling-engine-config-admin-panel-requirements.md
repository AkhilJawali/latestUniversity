# A4-340 — Frontend: Scheduling Engine Configuration Admin Panel (CRUD)

**Jira:** A4-340 (Story) · **Type:** Frontend · **Story Points:** 5
**Parent Epic:** UTMS · **Depends on:** A4-335 (Frontend SPA foundation)
**Requirement Generation Subtask:** A4-341
**Status of this document:** Draft for Lead review

---

## 1. Introduction

This document specifies the requirements for a frontend admin panel that lets a Department Coordinator / Administrator view and manage the scheduling engine's three configuration data sets through table-based screens with add / edit / delete actions. The three data sets are: **session-derivation rules**, **soft-constraint weights**, and **institution common slots (CCC / UWE)**. Today these tables are read-only inputs to the scheduling engine and have no management UI, so the values can only be changed directly in the database.

This document covers only the **frontend** behavior. It has a hard prerequisite: the three backend configuration tables currently expose **no CRUD REST API** (the engine only reads them). That backend gap is called out explicitly in Section 12 (Dependencies) and Section 16 (Open Questions) and must be resolved before this story can be implemented.

---

## 2. User Story

*As a* Department Coordinator / Administrator,
*I want* table-based screens to view and manage the scheduling engine's configuration data (session-derivation rules, soft-constraint weights, and institution common slots), with add / edit / delete actions triggered from row buttons,
*so that* I can control how the engine derives sessions and scores drafts without touching the database directly.

**BRD traceability:**
- BRD 7.7 — "Mixed slot durations" (session-derivation rules govern L-T-P → session mapping across durations).
- BRD 7.7 — "Institution-level common slots (CCC / UWE)".
- BRD 6.10 — engine feasibility / quality scoring (soft-constraint weights feed the quality score).

**Design decisions consumed (from A4-11 scheduling engine design):**
- **PD-74** — Session-derivation rules per campus (includes `slot_duration_minutes`).
- **PD-70** — Soft-constraint weights in a DB config table; default equal weights.
- **KD-52 / OQ-D3** — Institution common slots (CCC/UWE) pre-placed as immovable before solving; CRUD ownership left to a future story (this story, in its frontend part, closes the UI half of OQ-D3).

---

## 3. Actors

| Actor | Interaction |
|---|---|
| Department Coordinator / Administrator | Primary user. Views the config tables, adds / edits / deletes records for their campus. |
| Scheduling Engine (indirect) | Downstream consumer. Reads the configuration at generation time. Not a UI actor, but the reason the config matters. |

> Role gating (who exactly may see/edit which entity) is enforced by the backend API and the SPA's auth layer (A4-335). This document assumes an authenticated Coordinator/Admin session and does not define the RBAC rules themselves — see Section 16 OQ-4.

---

## 4. User Journeys

### Journey A — View configuration
1. User navigates to the Scheduling Engine Configuration screen.
2. The screen presents three sections/tabs — Derivation Rules, Soft-Constraint Weights, Common Slots.
3. Each section loads its records into a sortable, paginated table.
4. If a section has no records, the table shows an empty-state message.
5. If the API call fails, an error state is shown with a retry action.

### Journey B — Add a record
1. User clicks **Add** on a table.
2. A modal form opens with the fields for that entity.
3. User fills the form; client-side validation (Zod) runs on submit.
4. On invalid input → inline field-level errors are shown, no request is sent.
5. On valid input → a create request is sent. On success the modal closes, a success toast shows, and the table refreshes to include the new row.
6. On backend rejection (e.g., 409 duplicate for the (campus, type) unique rule) → the error is surfaced to the user in the modal (form stays open).

### Journey C — Edit a record
1. User clicks **Edit** on a row.
2. A modal opens pre-filled with that record's current values.
3. User changes fields; validation runs on save.
4. On valid save → update request sent; on success modal closes, toast shows, table reflects the change.
5. On backend rejection → error surfaced in the modal.

### Journey D — Delete a record
1. User clicks **Delete** on a row.
2. A confirmation dialog appears naming the record.
3. On confirm → soft-delete request sent; on success the row is removed from the table and a toast shows.
4. On cancel → dialog closes, nothing changes.
5. On backend rejection → error surfaced (toast / dialog).

---

## 5. Functional Requirements

### FR-1 — Configuration screen shell
- FR-1.1 The system SHALL present a single Scheduling Engine Configuration screen that groups the three entities into distinct sections or tabs: Derivation Rules, Soft-Constraint Weights, Common Slots.
- FR-1.2 The screen SHALL be reachable only within an authenticated Coordinator/Admin session and SHALL follow the routing, layout, and auth conventions established by A4-335.
- FR-1.3 Each section SHALL independently show loading, empty, error, and populated states.

### FR-2 — Data tables (all three entities)
- FR-2.1 Each entity's records SHALL be displayed in a table.
- FR-2.2 Tables SHALL support sorting by column.
- FR-2.3 Tables SHALL support pagination. (Page size default — see Section 16 OQ-6; align with A4-335 table convention.)
- FR-2.4 Each row SHALL provide **Edit** and **Delete** action controls.
- FR-2.5 Each table SHALL provide an **Add** control.
- FR-2.6 All values rendered into the table and forms SHALL use context-aware output encoding; user-supplied text (e.g., rule description, common-slot name) SHALL never be injected as raw HTML.

**Derivation Rules table columns:** component type, slot duration (minutes), hours per session, description, active status.
**Soft-Constraint Weights table columns:** constraint type, weight, active status.
**Common Slots table columns:** name, day of week, slot definition, applies-to-all-batches, active status.

> All three entities are campus-scoped. How the active campus is chosen/displayed (dropdown, session context, or Admin-selectable) is Section 16 OQ-3.

### FR-3 — Add (Create) via modal form
- FR-3.1 Clicking **Add** SHALL open a modal form with the entity's editable fields.
- FR-3.2 The form SHALL validate input client-side (Zod) before sending any request.
- FR-3.3 On successful create the modal SHALL close and the table SHALL refresh to show the new record.
- FR-3.4 On backend validation/conflict error the form SHALL remain open and display the error (mapped from the backend error envelope).

**Derivation Rules add form fields:**
- Component type — required; selected from the allowed component-type options (L / T / P — exact allowed set is OQ-5).
- Slot duration minutes — required; positive integer.
- Hours per session — required; decimal with one fractional digit (backend type DECIMAL(3,1)).
- Description — optional; max 200 characters.

**Soft-Constraint Weights add form fields:**
- Constraint type — required; selected from the allowed soft-constraint types (the backend enum: `FACULTY_TIME_PREFERENCE`, `FACULTY_DISTRIBUTION_PREFERENCE`, `ROOM_PROXIMITY`, `GAP_MINIMIZATION`, `SOFT_BLOCK_OVERRIDE`, `DAY_PATTERN_BALANCE`). Source of this list is OQ-5.
- Weight — required; decimal, two fractional digits (backend type DECIMAL(4,2)). Acceptable business range is OQ-2.

**Common Slots add form fields:**
- Name — required; max 100 characters.
- Day of week — required; selected from the allowed day values.
- Slot definition — required; selected from available slot definitions for the campus (source is OQ-5).
- Applies to all batches — boolean; default true.

### FR-4 — Edit (Update) via modal form
- FR-4.1 Clicking **Edit** SHALL open a modal pre-filled with the record's current values.
- FR-4.2 The same field-level validation as Add SHALL apply.
- FR-4.3 On successful save the modal SHALL close and the table SHALL reflect the updated values.
- FR-4.4 On backend rejection the form SHALL remain open and display the error.

### FR-5 — Delete (soft) with confirmation
- FR-5.1 Clicking **Delete** SHALL open a confirmation dialog identifying the record.
- FR-5.2 On confirm the system SHALL send a delete request; the backend performs a **soft delete** (sets `deleted_at`). The frontend does not perform hard deletes.
- FR-5.3 On success the row SHALL be removed from the table.
- FR-5.4 On cancel nothing SHALL change.
- FR-5.5 On backend rejection the error SHALL be surfaced.

### FR-6 — Client-side validation
- FR-6.1 Each form SHALL use a Zod schema matching the entity's field rules (required/optional, type, length, numeric format).
- FR-6.2 Validation errors SHALL be shown inline at the offending field.
- FR-6.3 No create/update request SHALL be sent while any validation error is present.

### FR-7 — Error and feedback handling
- FR-7.1 Backend errors SHALL be mapped from the standard error envelope (`status`, `error`, `message`, `details`) into user-readable messages; internal details/stack traces SHALL never be shown.
- FR-7.2 Successful create/update/delete SHALL produce a confirmation toast.
- FR-7.3 List-load failures SHALL show a retryable error state (FR-1.3).

---

## 6. Constraints Owned by This Document

This is a frontend story; it does not own any scheduling or data-integrity constraints. It owns only UI-behavior rules:

| ID | Rule |
|---|---|
| UI-1 | No create/update request is issued unless client-side (Zod) validation passes. |
| UI-2 | Delete is always preceded by an explicit confirmation dialog. |
| UI-3 | User-supplied text is rendered with context-aware encoding; never as raw HTML (no `dangerouslySetInnerHTML` with user data). |
| UI-4 | Backend error envelope is surfaced without exposing internal details. |

---

## 7. Constraints Referenced from Other Documents

| Constraint | Owner | How referenced here |
|---|---|---|
| Uniqueness: derivation rule per (campus, component_type) | Backend config table (V10 migration) / prerequisite backend CRUD story | Frontend must surface the resulting 409 conflict on Add/Edit. |
| Uniqueness: weight per (campus, constraint_type) | Backend config table (V10) / prerequisite backend CRUD story | Same — surface 409. |
| Allowed soft-constraint types (6 values) | A4-11 `SoftConstraintType` enum | Populates the weights "constraint type" selector; validation must reject values outside this set. |
| Institution common slots pre-placed as immovable | A4-11 KD-52 | Explains why this data matters; no UI enforcement here. |
| Weights feed the quality score (weighted average) | A4-11 PD-70 / KD-49 | Context only. |
| Slot definitions (referenced by common slots) | Time-slot grid module | Populates the "slot definition" selector. |
| Campus | Master data (campus) | Scopes all three entities. |

---

## 8. Validation Rules (client-side, Zod)

| Entity | Field | Rule |
|---|---|---|
| Derivation Rule | component type | required; one of allowed component types (OQ-5) |
| Derivation Rule | slot duration minutes | required; integer > 0 |
| Derivation Rule | hours per session | required; decimal ≥ 0, one fractional digit; fits DECIMAL(3,1) |
| Derivation Rule | description | optional; ≤ 200 chars |
| Weight | constraint type | required; one of the 6 `SoftConstraintType` values |
| Weight | weight | required; decimal, two fractional digits; fits DECIMAL(4,2); business min/max = OQ-2 |
| Common Slot | name | required; ≤ 100 chars |
| Common Slot | day of week | required; one of allowed day values |
| Common Slot | slot definition | required; a valid campus slot definition |
| Common Slot | applies to all batches | boolean; default true |

> Client-side validation is a UX convenience. Authoritative validation (uniqueness, FK existence, enum membership) is enforced by the backend; the frontend must handle backend rejections gracefully regardless of client checks.

---

## 9. Non-Functional Requirements

- **NFR-1 (Performance):** Table load and paginated fetches SHALL feel responsive; align with A4-335's list-view performance expectations. Config data volumes are small (a handful of rows per entity per campus).
- **NFR-2 (Security / XSS):** Per org standards, all rendered user data uses context-aware encoding; sanitize any rich text with DOMPurify if rich text is ever introduced (not expected here).
- **NFR-3 (Accessibility):** Modals SHALL manage focus (trap focus while open, restore on close); tables and actions SHALL be keyboard-navigable; contrast SHALL meet WCAG 2.1 AA. (Note: full WCAG conformance requires manual assistive-technology testing.)
- **NFR-4 (Consistency):** Screens SHALL reuse the shared component library / design system from A4-335 (tables, modals, form controls, toasts).

---

## 10. Acceptance Criteria (Given / When / Then)

1. **Given** configuration data exists, **When** the coordinator opens the configuration screen, **Then** each entity (derivation rules, soft weights, common slots) is shown in a sortable, paginated table.
2. **Given** a table, **When** the coordinator clicks Add and submits a valid form, **Then** a create request is sent, the record is created, and it appears in the table.
3. **Given** a table row, **When** the coordinator clicks Edit and saves valid changes, **Then** the record is updated and the change is reflected in the table.
4. **Given** a table row, **When** the coordinator clicks Delete and confirms, **Then** the record is soft-deleted and removed from the table.
5. **Given** invalid form input, **When** the coordinator submits, **Then** inline field-level validation errors are shown and no request is sent.
6. **Given** a duplicate (campus, type) for a derivation rule or weight, **When** the coordinator submits Add, **Then** the backend returns a 409 conflict and the form stays open showing a readable duplicate-error message. *(error path)*
7. **Given** the list API fails, **When** the section loads, **Then** an error state with a retry action is shown (no crash, no blank table). *(edge case)*
8. **Given** the Delete confirmation dialog is open, **When** the coordinator cancels, **Then** the dialog closes and no record is changed. *(edge case)*

---

## 11. Data Model (conceptual — frontend view only)

The frontend consumes DTOs from the backend CRUD API (to be created — Section 12). Conceptual shapes:

- **DerivationRule:** `{ id, campusId, componentType, slotDurationMinutes, hoursPerSession, description?, isActive }`
- **SoftConstraintWeight:** `{ id, campusId, constraintType, weight, isActive }`
- **CommonSlot:** `{ id, campusId, name, dayOfWeek, slotDefinitionId, appliesToAllBatches, isActive }`

Exact request/response DTO field names and the campus-selection contract are defined by the prerequisite backend story and finalized in this story's design derivation.

---

## 12. Dependencies

| Dependency | Type | Status | Notes |
|---|---|---|---|
| **Backend CRUD REST API for the three config entities** | **Hard prerequisite** | **DOES NOT EXIST** | The tables `session_derivation_rules`, `soft_constraint_weights`, `institution_common_slots` are currently read-only (only `SchedulingDataLoader` reads them). No controller/service/DTO/mapper exists. A backend CRUD story (controllers, services, DTOs, MapStruct mappers, validation, soft-delete, audit events) MUST exist before this frontend story can be implemented. See OQ-1. |
| A4-335 — Frontend SPA foundation | Hard | Assumed available | Routing, auth, shared component library, table/modal/toast primitives, API client. |
| Campus master data | Soft | Available | For campus scoping / selector. |
| Slot definitions (time-slot grid) | Soft | Available | Populates the common-slot "slot definition" selector. |
| `SoftConstraintType` enum (A4-11) | Reference | Available | Source of the 6 weight constraint-type values. |

---

## 13. Assumptions

1. A1 — A4-335 provides the SPA shell, auth session, shared table/modal/form/toast components, and a configured API client. This story builds on them, not from scratch.
2. A2 — The three config entities are managed per campus (matching the backend schema: every table has `campus_id`).
3. A3 — Delete is always soft-delete via the backend; the UI never issues hard deletes.
4. A4 — Data volumes are small (config tables), so client-side sorting/pagination or simple server pagination both suffice; the exact approach follows A4-335 convention.
5. A5 — The backend CRUD story (OQ-1) will expose the standard UTMS error envelope so the frontend can map errors consistently.

---

## 14. Consistency Notes

- The Jira story description and this document agree on scope: three entities, table + modal CRUD, soft delete, Zod validation. No contradictions found.
- The story labels it "CRUD / frontend / scheduling-engine" and flags the backend gap; this document elevates that gap to a formal hard-prerequisite dependency (Section 12) and an Open Question (OQ-1) rather than assuming the API exists.
- `institution_common_slots` has **no** DB unique constraint today (unlike the other two tables). This document does not invent one; the question of whether it needs one is OQ-7 and is a backend-story decision.
- Every [TBD]/uncertain item in the FRs is carried into Section 16.

---

## 15. Out of Scope

- **Backend CRUD API implementation** — belongs to the prerequisite backend story (OQ-1), not this frontend story.
- **Direct editing of scheduled sessions / drag-and-drop timetable editing** — owned by A4-15.
- **Generation trigger and draft viewing** — separate story.
- **RBAC rule definition** (which role may edit which entity) — enforced by backend + A4-335 auth; not defined here (OQ-4).
- **Bulk import/export of configuration** — not requested by the BRD or story; deferred.
- **Rich-text / HTML fields** — none of the fields are rich text.

---

## 16. Open Questions

| # | Question | Owner | Notes / Proposed default |
|---|---|---|---|
| OQ-1 | **Backend CRUD API does not exist.** A prerequisite backend story (CRUD controllers/services/DTOs/mappers/validation for the three config tables) must be created and completed before this frontend story. Confirm creation of that backend story and its scope. | Product Owner / Lead | **Blocking.** Proposed: one backend story covering all three entities, mirroring the Asset CRUD pattern (campus-existence check, (campus,type) uniqueness → 409, `constraintType` validated against `SoftConstraintType`, `slotDefinitionId` FK check, soft-delete, audit events). |
| OQ-2 | What is the acceptable business range for a soft-constraint **weight**? Column is DECIMAL(4,2) (0–99.99) but no business min/max is stated. | Product Owner | Proposed: 0.00–10.00 [TBD — confirm]. |
| OQ-3 | How is the active **campus** chosen in the UI (session-scoped for Coordinator, dropdown for Admin)? | Product Owner / A4-335 | [TBD — confirm]. |
| OQ-4 | Exact **RBAC**: may a Coordinator edit institution-level common slots, or is that Admin-only? | Product Owner | [TBD — confirm]. Affects which entities are editable per role. |
| OQ-5 | Source of dropdown option lists: allowed **component types** (L/T/P?), **soft-constraint types** (from A4-11 enum — via API?), **days of week**, and **slot definitions** (campus slot-grid API). Which endpoints provide these? | Backend / System Design | [TBD]. `SoftConstraintType` enum values are known; the others need a lookup source. |
| OQ-6 | Default table **page size** and sortable columns per entity. | A4-335 convention | Proposed: follow A4-335 default. |
| OQ-7 | Should `institution_common_slots` have a **uniqueness** rule (e.g., campus + day + slot definition)? Currently none exists. | Backend story | [TBD — backend decision]. |

---

## 17. Traceability

| BRD / Design ref | Requirement here | Verified |
|---|---|---|
| BRD 7.7 — mixed slot durations | FR-3 (derivation-rule form: slot duration, hours per session) | Derivation-rule CRUD lets admins configure duration→session mapping. |
| BRD 7.7 — institution common slots (CCC/UWE) | FR-2/FR-3 common-slots table + form | Provides the management UI for CCC/UWE slots. |
| BRD 6.10 — feasibility/quality scoring | FR-2/FR-3 soft-weights table + form | Weights feed the quality score (PD-70/KD-49). |
| A4-11 PD-74 | Derivation-rule fields (component type, slot duration, hours) | Fields match V10 schema. |
| A4-11 PD-70 | Soft-weights CRUD; default equal weights noted | Weight management UI. |
| A4-11 KD-52 / OQ-D3 | Common-slots CRUD (frontend half of OQ-D3) | UI to manage previously orphaned config. |
| Story dependency A4-335 | FR-1.2, NFR-4, A1 | Built on SPA foundation. |
| Story backend-dependency flag | Section 12 + OQ-1 | Elevated to formal hard prerequisite. |

---

*Prepared for Lead review (A4-341). Blocking item OQ-1 (missing backend CRUD API) must be resolved before design derivation and implementation.*
