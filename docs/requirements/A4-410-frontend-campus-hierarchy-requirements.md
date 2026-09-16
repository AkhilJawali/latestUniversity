# A4-410 — Frontend: Campus Hierarchy Master Data (CRUD) — Requirements

| | |
|---|---|
| **Issue** | A4-410 (Story) |
| **Epic** | A4-1 — University Timetable Management System (UTMS) |
| **Requirement subtask** | A4-411 |
| **Consumes backend** | A4-2 — Campus Hierarchy Master Data Management (APIs implemented) |
| **Role** | frontend |
| **Tech** | React 18 (plain JSX, no TypeScript), React Router v6, TanStack Query, Zustand, Zod, Axios |

---

## 1. Introduction

This document specifies the requirements for the **frontend UI** that lets an administrator view and manage the institution's organizational hierarchy — Campus → Department → Program → Batch → Section — through the browser. It is a UI-only story: all data, validation, and persistence are owned by the backend story **A4-2**; this story consumes those REST APIs and presents them. It does not define or change any API, schema, or business rule.

## 2. User Story

*As a* System Administrator, *I want* a web UI to view and manage the campus hierarchy with full CRUD, *so that* I can maintain the institution's organizational structure without direct database edits.

## 3. Actors

- **System Administrator** — the primary actor; creates, reads, updates, and soft-deletes hierarchy entities. (RBAC enforcement is deferred to the Auth module; this story assumes an authenticated admin.)

## 4. User Journeys

1. **Browse the hierarchy** — Admin opens the Campus Hierarchy page → sees the list of campuses → expands/drills into a campus to see its departments, then programs, batches, and sections.
2. **Create an entity** — Admin clicks "Add" at a level → fills a form → submits → the new record appears in the list; validation errors block bad input.
3. **Edit an entity** — Admin opens an existing record → changes editable fields → saves → the list reflects the change.
4. **Delete an entity** — Admin triggers delete → confirms in a dialog → record is soft-deleted and disappears from the list; if the backend rejects due to child references, the error is shown and the record remains.
5. **Handle errors** — Any API/validation failure is surfaced to the admin in a readable form (inline field errors or a toast); raw errors/stack traces are never shown.

## 5. Functional Requirements

Entities in scope (each is a level of the hierarchy): **Campus, Department, Program, Batch, Section.** The FRs below apply per entity except where noted.

### FR-1 — Hierarchy browse view
- FR-1.1 The page SHALL present the hierarchy so the admin can navigate Campus → Department → Program → Batch → Section (tree/drill-down), backed by the backend hierarchy-tree endpoint(s) owned by A4-2.
- FR-1.2 The page SHALL require/allow selecting or expanding a parent before showing its children, consistent with the backend tree contract.
- FR-1.3 Soft-deleted records SHALL NOT appear (the backend already excludes them; the UI shows what the API returns).

### FR-2 — List each entity
- FR-2.1 Each entity SHALL have a paginated list view consuming the backend list endpoint, using the standard `{ data, meta }` response envelope.
- FR-2.2 Child entities SHALL be filterable by their parent where the backend list endpoint supports a parent filter (exact parameter confirmed at design time — see OQ#1).
- FR-2.3 Pagination SHALL use the backend `Pageable` contract (page, size, sort); default page size follows the UTMS API standard (20; max 100) — not an invented value.
- FR-2.4 List loading SHALL show skeleton placeholders; an empty result SHALL show an empty state with a primary "Add" action.

### FR-3 — Create an entity
- FR-3.1 Each entity SHALL have a create form (modal or panel) capturing the fields defined by A4-2 for that entity (e.g., campus: name, code, location; department: name, code, parent campus; program: name, code, parent department, degree type, duration; batch: year identifier, strength, elective basket, parent program; section: section identifier, sub-strength, parent batch).
- FR-3.2 Inputs SHALL be validated client-side with Zod before submit (required fields, type/length/format); the backend re-validates authoritatively.
- FR-3.3 On success the record SHALL be persisted via the backend create endpoint and the relevant list SHALL refresh to include it without a full page reload.
- FR-3.4 On a uniqueness conflict (e.g., duplicate code) the UI SHALL surface the backend 409 as a readable message and SHALL NOT create a duplicate.

### FR-4 — Edit an entity
- FR-4.1 Each entity SHALL have an edit form pre-filled with current values.
- FR-4.2 Only fields the backend permits to change SHALL be editable (e.g., campus update covers name/location; immutable identifiers such as code SHALL be read-only in edit, per the A4-2 contract).
- FR-4.3 On success the list/detail SHALL reflect the updated values.

### FR-5 — Delete an entity (soft-delete)
- FR-5.1 Delete SHALL require a confirmation dialog before calling the backend delete endpoint.
- FR-5.2 A successful delete SHALL remove the record from the list (backend performs soft-delete; DELETE returns 204).
- FR-5.3 If the backend rejects the delete due to referential integrity (e.g., a department with active programs), the UI SHALL show that error and keep the record in the list.

### FR-6 — Error handling & feedback
- FR-6.1 Validation failures SHALL appear as inline field-level messages; transient/operation failures SHALL appear as toasts.
- FR-6.2 The UI SHALL never display raw backend error payloads or stack traces (org security standard); it shows user-friendly messages.
- FR-6.3 A background refetch SHALL not blank the screen (stale-while-revalidate); only initial loads show skeletons.

## 6. Constraints Owned by This Document

This story owns only **presentation/interaction constraints** (it owns no data or business rules):
- C-1 The frontend MUST be plain JavaScript/JSX — no TypeScript (`.jsx`/`.js` only), per the frontend language rule.
- C-2 All API calls MUST go through the shared axios client (`apiClient`, baseURL `/api/v1`); no component builds its own HTTP client.
- C-3 No `dangerouslySetInnerHTML`; any rich text is sanitized (org XSS standard). Hierarchy data is plain text, so no raw HTML injection.
- C-4 Forms MUST be validated with Zod before submission.

## 7. Constraints Referenced from Other Documents (owned by A4-2)

- Referential integrity (reject orphan references; block delete of parents with children) — **A4-2**.
- Uniqueness rules (campus code, department code scope, etc.) — **A4-2**.
- Field definitions, types, and which fields are mutable on update — **A4-2**.
- Soft-delete semantics and exclusion of deleted rows from reads — **A4-2**.
- Hierarchy tree DTO shape and endpoints — **A4-2**.

## 8. Validation Rules (client-side, mirroring backend)

| Field group | Client rule (Zod) | Authoritative check |
|---|---|---|
| Required text (name, code, identifiers) | non-empty, trimmed, max length per A4-2 | backend 400 |
| Numeric (strength, sub-strength, duration) | integer, > 0 | backend 400 |
| Parent reference | must be selected (non-null) | backend 400/409 |
| Code uniqueness | not checked client-side | backend 409 (surfaced by UI) |

## 9. Non-Functional Requirements

- NFR-1 List/read views SHALL render within the app's normal responsiveness; route-level code splitting SHALL be used (`React.lazy`) consistent with existing feature pages.
- NFR-2 Accessibility (WCAG 2.1 AA): keyboard-operable forms/dialogs, focus trap in modals, ARIA labels on icon-only actions, visible focus indicators.
- NFR-3 The feature SHALL follow the existing frontend structure (`features/master-data/...`), naming, and import-order conventions.
- NFR-4 No secrets or hardcoded environment values in the frontend.

## 10. Acceptance Criteria

1. **Browse** — *Given* the admin opens the Campus Hierarchy page, *When* data loads, *Then* campuses are listed and each can be expanded to reveal its departments, programs, batches, and sections.
2. **Create (happy path)** — *Given* the create-campus form, *When* name/code/location are submitted, *Then* the campus is created and appears in the list without a full page reload.
3. **Referential-integrity delete (error path)** — *Given* a department with active programs, *When* the admin attempts to delete it, *Then* the UI shows the backend referential-integrity error and the row is not removed.
4. **Child create with parent** — *Given* a batch/section form, *When* strength/sub-strength, parent, and (for batch) elective basket are entered, *Then* the record is saved and shown with its parent association.
5. **Validation (edge case)** — *Given* an invalid parent reference or a missing required field, *When* the form is submitted, *Then* inline validation errors appear and no create request produces a bad record.
6. **Duplicate code** — *Given* an existing campus code, *When* the admin submits a create with the same code, *Then* the UI shows the backend 409 conflict and no duplicate is created.

## 11. Data Model (conceptual — owned by A4-2)

Read-only from the frontend's perspective. Entities and their parent links:
- Campus (name, code, location)
- Department (name, code) → Campus
- Program (name, code, degree type, duration) → Department
- Batch (year identifier, strength, elective basket) → Program
- Section (section identifier, sub-strength) → Batch

Exact field names/types are defined by A4-2 DTOs and confirmed at design time.

## 12. Dependencies

- **A4-2** — backend CRUD + hierarchy-tree APIs (implemented). Hard dependency.
- **A4-335** — frontend app shell/routing/providers (done); this feature mounts inside the existing AppShell and router.
- Shared `apiClient`, UI primitives, and toast/feedback components from the existing frontend.

## 13. Assumptions

1. The A4-2 endpoints listed in the design will be reachable at `/api/v1/...` via the existing dev proxy / reverse proxy.
2. The admin is authenticated; RBAC is out of scope (Auth module).
3. Backend list endpoints return the standard `{ data, meta }` envelope, and single-resource endpoints return `{ data }`.

## 14. Consistency Notes

- No contradictions found between FRs. The UI never defines a rule the backend also defines; where both "touch" a rule (e.g., uniqueness), the UI's role is strictly to **display** the backend's decision (FR-3.4, FR-5.3).
- FR-2.3 page-size numbers are cited from the UTMS API standard, not invented.

## 15. Out of Scope

- Any backend/API/schema change (owned by A4-2).
- RBAC / authorization enforcement (Auth module).
- Scheduling, allocation, or any use of the hierarchy beyond CRUD/browse.
- Bulk import/export of hierarchy data (separate story, A4-51).
- The other master-data frontends (courses, faculty, rooms, etc.) — their own stories (A4-415, A4-420, …).

## 16. Open Questions

| # | Question | Disposition |
|---|---|---|
| OQ#1 | Exact parent-filter query params for child list endpoints (e.g., `/departments?campusId=`) vs. relying solely on the tree endpoints. | Confirm against A4-2 controllers during Design Derivation (A4-412). |
| OQ#2 | Whether a dedicated Section list endpoint exists or sections are reached only via the batch tree. | Confirm against A4-2 `SectionController` during design. |
| OQ#3 | Whether create/edit forms should use modals or full pages per level (UX depth). | Decide in design per the UI standards (modals for quick edits; full pages for complex forms). |

## 17. Traceability

| BRD ref | Requirement | FR |
|---|---|---|
| 6.1 — "Maintain hierarchical master data: Campus → Department → Program → Batch/Section" | Browse + manage all five levels | FR-1, FR-2, FR-3, FR-4, FR-5 |
| 6.1 — "Batch/section master: strength, program, elective basket enrolled" | Batch/section create/edit fields | FR-3.1, FR-4, AC-4 |
| A4-2 referential integrity | Delete blocked when children exist, error surfaced | FR-5.3, AC-3 |
| Org security standard (no internal leakage) | Safe error display | FR-6.2 |
| Frontend language rule | Plain JSX only | C-1 |
