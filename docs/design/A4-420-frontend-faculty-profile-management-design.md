# A4-420 — Frontend Faculty Profile Management (CRUD): Design Document

- **Story:** A4-420 — Frontend Faculty Profile Management (CRUD)
- **Design Derivation subtask:** A4-422
- **Requirement source:** docs/requirements/A4-420-frontend-faculty-profile-management-requirements.md (A4-421, Approved)
- **Consumes backend:** A4-4 (`/api/v1/faculty` + sub-resources), A4-3 (courses), A4-2 (campus hierarchy)
- **Date:** 2026-09-04

## 1. Overview

A new frontend feature module `features/master-data/faculty-management/` providing faculty CRUD, competency management, and campus-association management against the A4-4 REST API. It follows the A4-410/A4-415 precedent: reuse the shared `ConfigTable`, `ConfirmDeleteDialog`, `validateWith`, and `mapApiError`; add a lazy route and a nav item. Because the faculty create form needs multi-value inputs (campuses, competencies) that the generic `ConfigFormModal` cannot render, the create/edit forms are **custom** for this feature (PD-2).

## 2. Provisional Decisions (from Open Questions — lead may override at approval)

| PD | Decision | Source OQ |
|----|----------|-----------|
| PD-OQ1 | **Resolved.** Backend `GET /api/v1/faculty/{id}/campuses` was added (returns `{data:[{campusId,name,code}]}`); the campus panel reads the current set from it. | OQ-1 |
| PD-OQ2 | List filtering is department + designation + "competent in course" (no free-text name search — backend has no such param). | OQ-2 |
| PD-OQ3 | Competency-mismatch warning is deferred (no backend surface). | OQ-3 |
| PD-OQ4 | Designation options are a frontend constant mirroring the 7 backend strings. | OQ-4 |
| PD-OQ5 | Competency course names resolved by joining IDs against the A4-3 courses list. | OQ-5 |

## 3. Design Decisions

- **PD-1:** Reuse `ConfigTable`, `ConfirmDeleteDialog`, `validateWith`, `mapApiError` in place (A4-410/A4-415 precedent). Do not reuse `ConfigFormModal` for create/edit (see PD-2).
- **PD-2:** Create/edit forms are a **custom `FacultyFormModal`** (native `<dialog>`), because the create form has multi-selects (campuses, competencies) and a campus→department dependent picker that the generic fields-spec modal cannot express. It runs the Zod schema via `validateWith` and maps backend errors via `mapApiError`, matching the shared modal's behavior.
- **PD-3:** Home-department picker uses the campus→department two-step (reuse `useCampuses` + `useDepartments(campusId)` from campus-hierarchy), same pattern as A4-415's cross-listing panel.
- **PD-4:** Competency management lives in a `CompetencyPanel` shown for a selected faculty (reads `GET /{id}/competencies` → IDs, resolves names via `useCourses`). Adds via bulk POST, removes via DELETE. Consistent with the immutable-on-PUT contract.
- **PD-5:** Campus management lives in a `CampusPanel` for a selected faculty, reading the new `GET /{id}/campuses`, adding/removing via the sub-resource endpoints; last-association removal error (422) surfaced.
- **PD-6:** Edit form omits identifier, campuses, competencies (immutable on PUT / managed via panels). Memoize `initialValues` (A4-410 code-review lesson) even though the custom modal manages its own reset — keeps parity and avoids needless resets.

## 4. Module Structure

```
features/master-data/faculty-management/
  constants/faculty-options.js        # DESIGNATIONS (7 strings)
  schemas/faculty-schemas.js          # facultyCreateSchema, facultyEditSchema (+ test)
  api/
    useFaculty.js                     # list (paged+filters), create, update, delete
    useFacultyCompetencies.js         # list/add(bulk)/remove
    useFacultyCampuses.js             # list (new GET)/add/remove
  components/
    FacultyFormModal.jsx              # custom create/edit modal
    CompetencyPanel.jsx
    CampusPanel.jsx
  pages/FacultyManagementPage.jsx     # table + filters + panels
  faculty-management.css
```
Plus: lazy route `/master-data/faculty` in `app/router.jsx`, "Faculty" nav item in `AppShell.jsx`.

## 5. Component & Data Design

### 5.1 API hooks (TanStack Query, via apiClient baseURL `/api/v1`)
- **useFaculty**
  - `useFacultyList(filters)` — `GET /faculty` with query params `{departmentId?, designation?, competencyCourseId?, page?, size?}`; returns `{data, meta}`. Query key includes the filters so changing them refetches.
  - `useCreateFaculty` — `POST /faculty` (body includes campusIds + optional competencyCourseIds); invalidate list.
  - `useUpdateFaculty` — `PUT /faculty/{id}` (name, designation, qualification, homeDepartmentId, min/max only); invalidate list.
  - `useDeleteFaculty` — `DELETE /faculty/{id}` (204); invalidate list.
- **useFacultyCompetencies(facultyId)** — `GET /faculty/{id}/competencies` (`{data:[ids]}`, enabled when id set); `useAddCompetencies` (`POST` array), `useRemoveCompetency` (`DELETE /{courseId}`); invalidate the competency key.
- **useFacultyCampuses(facultyId)** — `GET /faculty/{id}/campuses` (`{data:[{campusId,name,code}]}`); `useAddFacultyCampus` (`POST /{campusId}`), `useRemoveFacultyCampus` (`DELETE /{campusId}`); invalidate the campus key.

### 5.2 Schemas (`faculty-schemas.js`)
- `facultyCreateSchema`: name(1..200), identifier(1..50), designation(enum DESIGNATIONS), qualification(1..500), homeDepartmentId(coerce int positive), minWeeklyLoad(optional coerce number ≥0), maxWeeklyLoad(optional coerce number ≥0.1), campusIds(array of int, min length 1), competencyCourseIds(array of int, optional). Cross-field refine: if both loads present, min ≤ max.
- `facultyEditSchema`: name, designation, qualification, homeDepartmentId, minWeeklyLoad, maxWeeklyLoad (same rules; no identifier/campusIds/competencyCourseIds).

### 5.3 FacultyFormModal (custom)
- Props: `open, mode('create'|'edit'), initialValues, campuses/departments data + campus selection state, courses (for competency multi-select on create), isPending, onSubmit, onClose`.
- Create mode renders: name, identifier, designation (select from DESIGNATIONS), qualification (textarea), campus→department two-step for homeDepartmentId, min/max load, campusIds (multi-checkbox list of campuses), competencyCourseIds (multi-select from courses).
- Edit mode renders: name, designation, qualification, homeDepartmentId (two-step), min/max load only.
- On submit: `validateWith(schema, values)` → on failure set field errors, send no request (AC-5); on success call `onSubmit(data, {onError})` which maps backend field/message errors.

### 5.4 FacultyManagementPage
- `useFacultyList` with filter state (departmentId via campus→dept two-step, designation select, competency-course select from `useCourses`).
- `ConfigTable` columns: name, identifier, designation, homeDepartmentName, min load, max load, active. Edit/Delete actions.
- Create/Edit via `FacultyFormModal` with memoized `initialValues` (PD-6). Delete via `ConfirmDeleteDialog` with `onError` surfacing failure (A4-415 code-review lesson).
- A "manage faculty" selector reveals `CompetencyPanel` + `CampusPanel` for the selected faculty.
- Loading skeleton, empty state, list error retry (from ConfigTable).

### 5.5 CompetencyPanel / CampusPanel
- CompetencyPanel: lists current competency courses (IDs resolved to names via `useCourses`), add (course picker → bulk POST), remove (DELETE). Errors surfaced inline (`role="alert"`), `key`-remount on faculty switch.
- CampusPanel: lists current campus associations from the new GET, add (campus picker → POST), remove (DELETE, 422 last-association message surfaced), `key`-remount on faculty switch.

## 6. Error Handling

- Form submit: client Zod first (no request on invalid); backend 400 → field errors, 409/422 → message, via `mapApiError`.
- Delete: `onError` surfaces reason, dialog stays open (A4-415 lesson).
- Sub-resource add/remove: inline `role="alert"` message; clear on new action / faculty switch.

## 7. Accessibility & Security

- Labels on all controls; `aria-label` on icon-only buttons; native `<dialog>` focus trap + Escape; inline errors announced.
- No `dangerouslySetInnerHTML`; all dynamic text escaped JSX. Plain JSX, PropTypes, enforced import order.

## 8. Testing Strategy

- Unit tests for `faculty-schemas.js`: create schema (each field + min≤max refine + campusIds non-empty), edit schema (immutable fields absent), and any list-filter helper. Target parity with A4-415's schema test depth.
- Verify via full vitest suite + vite build (new lazy chunk emitted).

## 9. Traceability

| Requirement | Design element |
|-------------|----------------|
| FR-1 (list + filters) | 5.1 useFacultyList, 5.4 page filters, ConfigTable |
| FR-2 (create) | 5.2 facultyCreateSchema, 5.3 FacultyFormModal (create) |
| FR-3 (edit) | 5.2 facultyEditSchema, 5.3 FacultyFormModal (edit), PD-6 |
| FR-4 (competency) | 5.1 useFacultyCompetencies, 5.5 CompetencyPanel |
| FR-5 (campus assoc) | 5.1 useFacultyCampuses (new GET), 5.5 CampusPanel |
| FR-6 (soft-delete) | 5.4 ConfirmDeleteDialog + onError |
| FR-7 (designations) | 5.2 DESIGNATIONS constant |
| NFR-1..5 | §6, §7 |

## 10. Out of Scope / Carried Open Questions

- OQ-2/3/4/5 carried as Provisional Decisions (§2) — lead may override at approval.
- Availability windows (A4-5), RBAC, scheduling — out of scope.
