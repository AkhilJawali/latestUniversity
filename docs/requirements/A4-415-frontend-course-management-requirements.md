# A4-415 — Frontend: Course Management (CRUD) — Requirements

| | |
|---|---|
| **Issue** | A4-415 (Story) |
| **Epic** | A4-1 — UTMS |
| **Requirement subtask** | A4-416 |
| **Consumes backend** | A4-3 — Course Management (APIs implemented) |
| **Role** | frontend |
| **Tech** | React 18 (plain JSX, no TypeScript), React Router v6, TanStack Query, Zustand, Zod, Axios |

---

## 1. Introduction

This document specifies the **frontend UI** for managing courses — their L-T-P (Lecture-Tutorial-Practical) structure, credits, type (core/elective/audit), equipment tags, prerequisites, and cross-listings — through the browser. It is UI-only: all data, validation, and business rules are owned by backend story **A4-3**; this story consumes those REST APIs and presents them. No API/schema/rule is defined or changed here.

## 2. User Story

*As a* Department Coordinator, *I want* a web UI to manage courses with their credit structure, type, prerequisites, equipment tags, and cross-listings, *so that* I can maintain accurate course master data without direct database edits.

## 3. Actors

- **Department Coordinator / Admin** — creates, reads, updates, soft-deletes courses and manages their prerequisites/cross-listings. (RBAC deferred to the Auth module; authenticated user assumed.)

## 4. User Journeys

1. **Browse courses** — open the Course Management page, see a paginated, filterable list of courses.
2. **Create a course** — click Add, fill L-T-P, credits, type, department, equipment tags (and optionally prerequisites), submit; the course appears in the list; validation errors block bad input.
3. **Edit a course** — open an existing course, change editable fields, save; the list reflects the change.
4. **Delete a course** — trigger delete, confirm; soft-deleted and removed from the list; backend errors surfaced.
5. **Manage prerequisites** — for a course, view its prerequisite courses and add/remove them; a non-existent or cyclic prerequisite is rejected by the backend and surfaced.
6. **Manage cross-listings** — add/remove a department a course is cross-listed to; the course then shows the cross-listed designation.

## 5. Functional Requirements

### FR-1 — List courses
- FR-1.1 A paginated list view consuming `GET /api/v1/courses`, using the standard `{ data, meta }` envelope.
- FR-1.2 The list SHALL support client-side filtering by course type (core/elective/audit) and a text search over name/code (server has no type filter param — see OQ#1). Cross-listed courses SHALL show the cross-listed designation.
- FR-1.3 Loading shows skeletons; empty result shows an empty state with an "Add course" action.

### FR-2 — Create a course
- FR-2.1 A create form capturing: name, code, department, lectureHours, tutorialHours, practicalHours, credits, courseType, equipment tags (multi-value), and optionally prerequisite course IDs.
- FR-2.2 Zod validation before submit: required fields; L/T/P integers >= 0; credits >= 0.1; courseType in {CORE, ELECTIVE, AUDIT}; code alphanumeric (with - and _), <= 20 chars; name <= 200.
- FR-2.3 On success, the course is persisted via `POST /api/v1/courses` and the list refreshes without a full page reload.
- FR-2.4 On a uniqueness conflict (duplicate code) the UI SHALL surface the backend 409 as a readable message and SHALL NOT create a duplicate.
- FR-2.5 On a prerequisite validation failure (non-existent course or cycle) the UI SHALL surface the backend error.

### FR-3 — Edit a course
- FR-3.1 An edit form pre-filled with current values.
- FR-3.2 Editable fields per the A4-3 update contract: name, L-T-P, credits, type, equipment tags. The **code is immutable** on edit (read-only), consistent with the backend update.
- FR-3.3 On success the list/detail reflects the update.

### FR-4 — Delete a course (soft-delete)
- FR-4.1 Delete requires a confirmation dialog before calling `DELETE /api/v1/courses/{id}`.
- FR-4.2 A successful delete removes the row (backend soft-deletes; returns 204).
- FR-4.3 Backend rejection (e.g., referenced as a prerequisite elsewhere) SHALL be surfaced and the row retained.

### FR-5 — Manage prerequisites
- FR-5.1 For a selected course, list its prerequisite course IDs via `GET /api/v1/courses/{id}/prerequisites`.
- FR-5.2 Add a prerequisite via `POST /api/v1/courses/{id}/prerequisites/{prerequisiteId}`; remove via `DELETE`.
- FR-5.3 Backend rejection of an invalid prerequisite (non-existent or cycle, per A4-3) SHALL be surfaced; the list refreshes on success.

### FR-6 — Manage cross-listings
- FR-6.1 Add a cross-listing to a department via `POST /api/v1/courses/{id}/cross-listings/{departmentId}`; remove via `DELETE`.
- FR-6.2 After a cross-listing change the course's cross-listed designation SHALL reflect the current state (via re-fetch).

### FR-7 — Error handling & feedback
- FR-7.1 Validation failures appear as inline field-level messages; operation failures as form-level messages/toasts.
- FR-7.2 The UI SHALL never display raw backend payloads or stack traces (org security standard).
- FR-7.3 Background refetch SHALL not blank the screen (stale-while-revalidate); only initial loads show skeletons.

## 6. Constraints Owned by This Document

- C-1 Plain JavaScript/JSX only — no TypeScript.
- C-2 All API calls via the shared `apiClient` (baseURL `/api/v1`); no bespoke HTTP client.
- C-3 No `dangerouslySetInnerHTML`; course data is plain text.
- C-4 Forms validated with Zod before submission.

## 7. Constraints Referenced from Other Documents (owned by A4-3)

- Course code uniqueness, prerequisite existence + cycle prevention, cross-listing code-collision checks — **A4-3**.
- Which fields are mutable on update (code immutable) — **A4-3**.
- Soft-delete semantics and exclusion of deleted rows — **A4-3**.
- Department references (for department picker and cross-listing) — **A4-2**.

## 8. Validation Rules (client-side, mirroring backend)

| Field | Client rule (Zod) | Authoritative check |
|---|---|---|
| name | non-empty, <= 200 | backend 400 |
| code | non-empty, <= 20, regex `^[A-Za-z0-9_-]+$` | backend 400 / 409 on duplicate |
| departmentId | required (positive int) | backend 400 |
| lectureHours / tutorialHours / practicalHours | integer >= 0, required | backend 400 |
| credits | number >= 0.1, required | backend 400 |
| courseType | one of CORE / ELECTIVE / AUDIT | backend 400 |
| equipmentTags | optional list of strings | — |
| prerequisiteCourseIds | optional, <= 20 entries | backend 400 / cycle check |

## 9. Non-Functional Requirements

- NFR-1 Route-level code splitting (`React.lazy`), consistent with existing feature pages.
- NFR-2 Accessibility (WCAG 2.1 AA): keyboard-operable forms/dialogs, focus trap in modals, ARIA labels, visible focus.
- NFR-3 Follows existing frontend structure (`features/master-data/...`), naming, import-order.
- NFR-4 No secrets/hardcoded env values.

## 10. Acceptance Criteria

1. **Create (happy path)** — *Given* the create-course form, *When* an L-T-P split (e.g. 3-1-2), credits, type, and department are submitted, *Then* the course is created and shown in the list without a full reload.
2. **Prerequisite validation** — *Given* a create/prerequisite-add referencing a non-existent or cyclic prerequisite, *When* submitted, *Then* the UI shows the backend error and the invalid prerequisite is not added.
3. **Equipment tags** — *Given* equipment tags entered as multiple values, *When* saved, *Then* they are stored and displayed on the course record.
4. **Type filter** — *Given* the type filter set to "elective", *When* applied, *Then* only elective courses are listed.
5. **Cross-listed designation** — *Given* a cross-listed course, *When* viewed, *Then* the shared/cross-listed designation is visible.
6. **Validation blocks bad input** — *Given* a missing required field or credits < 0.1, *When* submitted, *Then* inline validation errors appear and no request creates a bad record.
7. **Duplicate code** — *Given* an existing course code, *When* a create with the same code is submitted, *Then* the UI shows the backend 409 and no duplicate is created.

## 11. Data Model (conceptual — owned by A4-3)

Course: name, code, departmentId, lectureHours, tutorialHours, practicalHours, credits, courseType, equipmentTags[], isCrossListed. Sub-resources: prerequisites (course to course IDs), cross-listings (course to department IDs). Exact types are defined by A4-3 DTOs.

## 12. Dependencies

- **A4-3** — backend course CRUD + prerequisite + cross-listing APIs (implemented). Hard dependency.
- **A4-2** — department data for the department picker and cross-listing target.
- **A4-335** — app shell/router/providers (done).
- Shared `apiClient`, reused CRUD table/modal/dialog components, `mapApiError`.

## 13. Assumptions

1. A4-3 endpoints reachable at `/api/v1/...` via the dev proxy / reverse proxy.
2. Authenticated user; RBAC out of scope.
3. List returns `{ data, meta }`, single returns `{ data }`, sub-resource lists return `{ data: [...] }`, delete returns 204.
4. A department list is available (from A4-2) to populate department/cross-listing pickers.

## 14. Consistency Notes

- No contradictions between FRs. Where a rule is shared (uniqueness, prerequisite cycle), the UI only **displays** the backend's decision (FR-2.4, FR-2.5, FR-5.3).
- Page-size numbers cited from the UTMS API standard; field bounds cited from A4-3 DTOs — none invented.

## 15. Out of Scope

- Any backend/API/schema change (A4-3).
- RBAC / authorization enforcement (Auth module).
- Scheduling / room-matching use of equipment tags (engine).
- Bulk import/export of courses (A4-51).
- Other master-data frontends.

## 16. Open Questions

| # | Question | Disposition |
|---|---|---|
| OQ#1 | Course list endpoint has no server-side type/department filter param (global paginated list). Filter by type/search client-side, or add a dept context? | Decide in design — default: client-side filter over the fetched page; note pagination interaction. |
| OQ#2 | Prerequisite management UX: inline multi-select in the create/edit form vs. a dedicated panel on a selected course. | Decide in design per UI standards; create supports `prerequisiteCourseIds`, edit uses the add/remove sub-resource endpoints. |
| OQ#3 | Cross-listing UX and where the department picker comes from (A4-2 departments list). | Decide in design; confirm department list hook availability. |

## 17. Traceability

| BRD ref | Requirement | FR |
|---|---|---|
| 6.1 — "Course master: credit hours, contact hours (L-T-P split), course type (core/elective/audit), prerequisite courses" | Course CRUD + L-T-P + type + prerequisites | FR-1, FR-2, FR-3, FR-5 |
| 7.1 — "Course credit structure (L-T-P)" | L-T-P + credits fields | FR-2.1, FR-2.2 |
| 7.1 — "Core vs. elective classification" | courseType + type filter | FR-1.2, FR-2, AC-4 |
| 7.1 — "Prerequisite mapping" | prerequisite management | FR-5, AC-2 |
| A4-3 cross-listing | cross-listed designation + management | FR-6, AC-5 |
| Org security standard | safe error display | FR-7.2 |
| Frontend language rule | plain JSX only | C-1 |
