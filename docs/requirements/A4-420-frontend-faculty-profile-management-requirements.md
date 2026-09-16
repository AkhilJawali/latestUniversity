# A4-420 — Frontend Faculty Profile Management (CRUD): Requirement Document

- **Story:** A4-420 — Frontend Faculty Profile Management (CRUD)
- **Requirement Generation subtask:** A4-421
- **Consumes backend:** A4-4 (Faculty Profile Management) — `/api/v1/faculty`
- **BRD:** 6.1 (faculty master: qualification, designation, home department, min/max weekly load, subject competency, campus associations), 7.2 (subject-competency mapping, multi-department/multi-campus load)
- **Date:** 2026-09-04

## 1. Introduction

This document specifies the UI-only requirements for managing faculty profiles: listing/filtering faculty, creating and editing a profile (designation, home department, qualification, min/max weekly load), managing subject competencies, and managing multi-campus associations. All persistence is provided by the already-implemented A4-4 backend; this story adds no backend or schema changes. The document is grounded in the verified A4-4 REST contract, and several story assumptions that the backend does not currently support are raised explicitly as Open Questions (Section 16) rather than invented.

## 2. User Story

*As an* HOD, *I want* a web UI to manage faculty profiles — designation, home department, qualification, subject-competency list, multi-campus associations, and min/max weekly load — *so that* faculty can be maintained and correctly matched to courses across campuses.

## 3. Actors

- **HOD / Admin** — the primary user who creates, edits, and soft-deletes faculty profiles and manages their competencies and campus associations. (RBAC is not enforced yet on the backend — all endpoints currently `permitAll`; this UI does not implement role gating, which is owned by a separate RBAC story.)

## 4. User Journeys

1. **Browse & filter faculty.** User opens the Faculty page → sees a paginated table of faculty (name, identifier, designation, home department, min/max load, active) → filters by department and/or designation and/or "competent in course X" → the table updates.
2. **Create a faculty profile.** User clicks "Add faculty" → fills name, identifier, designation (from fixed list), home department, qualification, min/max weekly load, at least one campus, optionally competency courses → submits → profile is created and appears in the list.
3. **Edit a faculty profile.** User edits a faculty → changes name, designation, qualification, home department, min/max load → submits. Identifier, campus associations, and competencies are NOT edited here (managed elsewhere — see journeys 4 & 5).
4. **Manage competencies.** User selects a faculty → sees its competency courses → adds one or more courses / removes a course.
5. **Manage campus associations.** User selects a faculty → adds a campus association / removes one (removing the last association is blocked by the backend).
6. **Soft-delete a faculty.** User deletes a faculty → confirms → the record is soft-deleted and leaves the active list.

## 5. Functional Requirements

### FR-1 — Faculty list and filtering
- **FR-1.1** The page lists faculty from `GET /api/v1/faculty` (paginated `{data, meta}`), showing: name, identifier, designation, home department name, min weekly load, max weekly load, active status.
- **FR-1.2** The list supports filtering by **home department** (`departmentId` query param) and by **designation** (`designation` exact-match query param).
- **FR-1.3** The list supports a "competent in course" filter using the `competencyCourseId` query param (backed by `GET /api/v1/faculty?competencyCourseId=` / the `by-competency/{courseId}` convenience route). This satisfies AC-4 ("search by subject competency"), realised as selecting a course rather than free-text.
- **FR-1.4** Pagination controls reflect `meta` (page, size, totalElements, totalPages).
- **FR-1.5** Loading state shows skeleton rows; empty state shows a message + "Add faculty" CTA; list error shows a retry affordance.

### FR-2 — Create faculty
- **FR-2.1** A create form collects: name (required, ≤200), identifier (required, ≤50), designation (required, one of the fixed list — FR-7), qualification (required, ≤500), home department (required, chosen from departments), min weekly load (optional, ≥0.0), max weekly load (optional, ≥0.1), campus associations (required, at least one), competency courses (optional).
- **FR-2.2** Client-side Zod validation mirrors the A4-4 bounds; an invalid submit shows inline field errors and sends **no** request (AC-5).
- **FR-2.3** Cross-field rule: if both provided, min weekly load ≤ max weekly load; violation shown inline before submit (backend also enforces, returning 422).
- **FR-2.4** On success (201), the modal/form closes and the new faculty appears in the list; on backend error, the mapped message/field errors are surfaced.

### FR-3 — Edit faculty
- **FR-3.1** An edit form collects only the fields `PUT /api/v1/faculty/{id}` accepts: name, designation, qualification, home department, min/max weekly load.
- **FR-3.2** Identifier, campus associations, and competencies are **not** editable in this form (identifier is immutable; the other two are managed via their own panels — FR-4, FR-5).
- **FR-3.3** Same validation and error handling as create (FR-2.2, FR-2.3, FR-2.4).

### FR-4 — Competency management
- **FR-4.1** For a selected faculty, the UI reads the current competency **course IDs** from `GET /api/v1/faculty/{id}/competencies` (`{data:[ids]}`) and resolves display names by joining against the courses list (A4-3).
- **FR-4.2** Adding competencies calls `POST /api/v1/faculty/{id}/competencies` with a JSON array of course IDs (bulk, idempotent, ≤50).
- **FR-4.3** Removing a competency calls `DELETE /api/v1/faculty/{id}/competencies/{courseId}`.
- **FR-4.4** Backend errors (e.g., non-existent course → 404) are surfaced inline.

### FR-5 — Campus association management
- **FR-5.1** Adding a campus association calls `POST /api/v1/faculty/{id}/campuses/{campusId}` (campus chosen from the campuses list, A4-2).
- **FR-5.2** Removing calls `DELETE /api/v1/faculty/{id}/campuses/{campusId}`; the backend blocks removing the **last** association (422) and that message is surfaced.
- **FR-5.3** See **OQ-1**: the backend exposes no GET to read back a faculty's current campus associations, so the UI cannot display the existing set from the API. Behaviour here depends on the OQ-1 resolution.

### FR-6 — Soft-delete
- **FR-6.1** Deleting a faculty calls `DELETE /api/v1/faculty/{id}` (204) behind a confirmation dialog.
- **FR-6.2** On success the record leaves the active list; on failure the reason is surfaced and the dialog stays open.

### FR-7 — Designation options
- **FR-7.1** Designation is chosen from the fixed backend allowlist: `Professor`, `Associate Professor`, `Assistant Professor`, `Lecturer`, `Senior Lecturer`, `Lab Instructor`, `Visiting Faculty`. See **OQ-4** (no backend endpoint lists these; the frontend mirrors the strings).

## 6. Constraints Owned by This Document

- Client-side form validation for faculty create/edit (mirrors A4-4 bounds). Authoritative validation remains on the backend.
- Client-side presentation of the fixed designation list (OQ-4).

## 7. Constraints Referenced from Other Documents

- **A4-4 (backend):** all field bounds — name ≤200, identifier ≤50, designation ≤50 and ∈ fixed list, qualification ≤500, minWeeklyLoad ≥0.0, maxWeeklyLoad ≥0.1, min ≤ max; identifier/campusIds/competencyCourseIds immutable on PUT; last-campus-association removal blocked (HC-FAC-7).
- **A4-3 (courses):** used to resolve competency course IDs → names and to populate the competency picker.
- **A4-2 (campus hierarchy):** used to populate the department filter/picker and the campus-association picker.

## 8. Validation Rules (client-side)

| Field | Rule |
|-------|------|
| name | required, ≤200 chars |
| identifier | required, ≤50 chars (create only) |
| designation | required, one of the 7 fixed values |
| qualification | required, ≤500 chars |
| homeDepartmentId | required, positive |
| minWeeklyLoad | optional; if present ≥0.0 |
| maxWeeklyLoad | optional; if present ≥0.1 |
| min/max | if both present, min ≤ max |
| campusIds (create) | required, at least one |

## 9. Non-Functional Requirements

- **NFR-1 (security/XSS):** all dynamic text rendered as escaped JSX; no `dangerouslySetInnerHTML`.
- **NFR-2 (a11y):** labels on all controls, `aria-label` on icon-only buttons, keyboard-accessible dialogs (native `<dialog>` focus trap + Escape), inline errors announced.
- **NFR-3 (performance):** route-based code splitting (lazy route); list uses server pagination.
- **NFR-4 (standards):** plain JSX (no TypeScript), PropTypes, enforced import order; reuse the shared table/modal/confirm components.
- **NFR-5 (feedback):** save/delete outcomes and sub-resource errors are surfaced to the user (inline message regions consistent with the reused components; a global toast system is not part of this story).

## 10. Acceptance Criteria

- **AC-1** *Given* the create-faculty form, *When* designation, home department, and competency list are submitted, *Then* the profile is created (201) and listed.
- **AC-2** *Given* the campus-association actions, *When* two campuses are added, *Then* both add calls succeed (201) — subject to OQ-1 for read-back display.
- **AC-3** *Given* min/max weekly load fields, *When* saved, *Then* the values persist and display on the record.
- **AC-4** *Given* the "competent in course" filter set to a course (e.g., Data Structures), *When* applied, *Then* only faculty with that competency are listed (via `competencyCourseId`).
- **AC-5** *Given* a required field left blank, *When* the form is submitted, *Then* an inline validation error is shown and no request is sent.
- **AC-6** *Given* an edit form, *When* opened, *Then* identifier is not editable and campus/competency are not part of the edit form (managed via their own panels).
- **AC-7** *Given* a delete confirmation, *When* the backend rejects the delete, *Then* the failure reason is surfaced and the dialog stays open.

## 11. Data Model (conceptual, frontend view)

- **Faculty** (from FacultyDto): id, name, identifier, designation, qualification, homeDepartmentId, homeDepartmentName, minWeeklyLoad, maxWeeklyLoad, isActive, createdAt, updatedAt.
- **Competency**: bare list of course IDs per faculty (names resolved via A4-3).
- **Campus association**: managed by campusId; no API read-back of the current set (OQ-1).

## 12. Dependencies

- A4-4 backend running (`/api/v1/faculty` + sub-resources).
- A4-3 courses list (competency picker + name resolution).
- A4-2 campus hierarchy (department filter/picker + campus-association picker).
- Reused frontend components from scheduling-config (table/modal/confirm) and the `validateWith` / `mapApiError` helpers, per the A4-410/A4-415 precedent.

## 13. Assumptions

1. The 7 designation strings are stable enough to mirror in the frontend until a "list designations" endpoint exists (OQ-4).
2. Competency and campus associations are managed on a per-selected-faculty basis (panels), not inline in the edit form — consistent with the immutable-on-PUT contract.
3. "Search by subject competency" (story AC-4) means selecting a course to filter by `competencyCourseId`, not free-text name search (which the backend does not offer — OQ-2).

## 14. Consistency Notes

- The story's "search by name" wording is reconciled with the backend as: department + designation + competency-course filters (no free-text name search exists — OQ-2). No FR promises free-text name search.
- The story's "competency-mismatch warning" scope item has no backend surface; it is **not** promised by any FR and is raised as OQ-3.
- The story mentions "toasts"; the reused components surface feedback via inline message regions, so NFR-5 frames feedback generically rather than promising a toast system that this story does not build.
- Edit-form field set (FR-3.1) is consistent with UpdateFacultyRequest (no identifier/campus/competency).

## 15. Out of Scope

- Any backend/API/schema change (owned by A4-4).
- Faculty **availability windows** (owned by the A4-5 frontend story).
- RBAC / role gating (separate story; backend currently `permitAll`).
- Scheduling, workload computation, substitution.

## 16. Open Questions

- **OQ-1 (blocking for FR-5 display):** The backend has **no GET endpoint** to read a faculty's current campus associations, and campuses are absent from FacultyDto. How should the UI show existing associations? Options: (a) request a backend `GET /faculty/{id}/campuses` (preferred, small A4-4 follow-up); (b) UI supports add/remove blind (no current-set display); (c) infer via `campusId` filter round-trips. Needs lead decision.
- **OQ-2:** The story mentions "search by name". The backend list endpoint has no free-text/name param. Confirm that department + designation + competency-course filtering is sufficient for this story, or raise a backend follow-up for a name search param.
- **OQ-3:** The story lists a "competency-mismatch warning". No backend flag/endpoint exists. Confirm this is deferred (recommended) or specify where such a signal would come from.
- **OQ-4:** No endpoint lists valid designations; the frontend will mirror the 7 fixed strings. Acceptable, or add a lookup endpoint?
- **OQ-5:** Competency read-back returns only course IDs (no names). Confirm resolving names via the loaded courses list is acceptable (it is the only option today).

## 17. Traceability

| BRD / Story | Requirement |
|-------------|-------------|
| BRD 6.1 (designation, home dept, qualification, min/max load, competency, campuses) | FR-2, FR-3, FR-4, FR-5, FR-7 |
| BRD 7.2 (subject-competency mapping) | FR-4 |
| BRD 7.2 (multi-department / multi-campus) | FR-1.2, FR-5 |
| Story AC-1 | FR-2, AC-1 |
| Story AC-2 | FR-5, AC-2 (subject to OQ-1) |
| Story AC-3 | FR-2.1/FR-3.1, AC-3 |
| Story AC-4 | FR-1.3, AC-4 |
| Story AC-5 | FR-2.2, AC-5 |
| Story "competency-mismatch warning" | OQ-3 (no backend support — deferred pending decision) |
