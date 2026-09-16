# A4-415 — Frontend: Course Management (CRUD) — Design

| | |
|---|---|
| **Issue** | A4-415 (Story) |
| **Design subtask** | A4-417 |
| **Requirement** | A4-416 (Approved) |
| **Consumes backend** | A4-3 (Course + prerequisites + cross-listings); A4-2 (departments) |
| **Stack** | React 18 (plain JSX), React Router v6, TanStack Query v5, Zustand, Zod, Axios |

---

## 1. Overview

A single-page Course Management admin: a flat, paginated course table with client-side type/search filtering, a create/edit modal, delete confirmation, and — on a selected course — panels to manage prerequisites and cross-listings. Built by reusing the generic A4-340 primitives (`ConfigTable`, `ConfigFormModal`, `ConfirmDeleteDialog`) exactly as A4-410 did (imported in place). Unlike A4-410, courses are **not** a drill-down hierarchy — the list is global.

## 2. Open-Question Resolutions -> Provisional Decisions

| OQ | Resolution (verified against A4-3) | Decision |
|----|-----------------------------------|----------|
| OQ#1 | `GET /api/v1/courses` is a global paginated list with no type/department query param. | **PD-1:** fetch the list and apply **client-side** filter by `courseType` + a name/code text search. (Consistent with the reused `ConfigTable`, which is non-paged UI.) A follow-up can add server filters if course counts grow. |
| OQ#2 | Create accepts `prerequisiteCourseIds` (<=20); edit has no prereq field — prereqs are managed via `GET/POST/DELETE /courses/{id}/prerequisites`. | **PD-2:** create form includes an optional prerequisite multi-select; a **Prerequisites panel** (shown when a course row is selected) does live add/remove via the sub-resource endpoints. |
| OQ#3 | Cross-listing target is a `departmentId`; departments come from A4-2. The A4-410 `useDepartments(campusId)` hook is campus-scoped. | **PD-3:** the create form and cross-listing panel pick a department via a **campus -> department** two-step (pick campus, then its departments via the existing `useDepartments(campusId)` hook / `useCampuses`). Avoids needing a new all-departments backend endpoint. |

Additional:
- **PD-4:** reuse `ConfigTable`/`ConfigFormModal`/`ConfirmDeleteDialog` imported in place from `features/scheduling-config/components/` (same call-site memoization of `initialValues` as fixed in A4-410 review #1).
- **PD-5:** immutable-on-edit fields (code, departmentId) are omitted from the edit form (matching `UpdateCourseRequest`, which has neither).

## 3. Backend Contract (consumed, not defined)

| Operation | Endpoint | Notes |
|-----------|----------|-------|
| List | `GET /courses` (paged) | `{data, meta}`; no filter param |
| Create | `POST /courses` | body incl optional `prerequisiteCourseIds` |
| Get | `GET /courses/{id}` | `{data}` |
| Update | `PUT /courses/{id}` | name, L-T-P, credits, type, equipmentTags (code + departmentId immutable) |
| Delete | `DELETE /courses/{id}` | 204 soft-delete |
| Prereq list | `GET /courses/{id}/prerequisites` | `{data:[ids]}` |
| Prereq add / remove | `POST` / `DELETE /courses/{id}/prerequisites/{prerequisiteId}` | 201 / 204 |
| Cross-list add / remove | `POST` / `DELETE /courses/{id}/cross-listings/{departmentId}` | 201 / 204 |

Fields (CourseDto): id, name, code, departmentId, departmentName, lectureHours, tutorialHours, practicalHours, credits, courseType, equipmentTags[], isCrossListed. `apiClient` baseURL `/api/v1`.

## 4. File Layout (`frontend/src/features/master-data/course-management/`)

```
course-management/
├── pages/
│   └── CourseManagementPage.jsx     # table + filters + selected-course panels
├── components/
│   ├── PrerequisitePanel.jsx        # add/remove prereqs for the selected course
│   └── CrossListingPanel.jsx        # add/remove cross-listings for the selected course
├── api/
│   ├── useCourses.js                # course CRUD hooks
│   ├── usePrerequisites.js          # prereq sub-resource hooks
│   └── useCrossListings.js          # cross-listing add/remove hooks
├── schemas/
│   └── course-schemas.js            # Zod: course create/edit
├── constants/
│   └── course-options.js            # COURSE_TYPES = [CORE, ELECTIVE, AUDIT]
└── course-management.css
```

The course create/edit form uses the reused `ConfigFormModal` with an inline fields spec (no separate modal component needed). Reused (imported in place): `ConfigTable`, `ConfigFormModal`, `ConfirmDeleteDialog`, `validateWith`, `mapApiError`. Department/campus data via A4-410's `useCampuses`/`useDepartments` (imported from `features/master-data/campus-hierarchy/api`).

## 5. Component & Hook Design

### 5.1 Page — `CourseManagementPage.jsx`
- Fetches courses via `useCourses()`; keeps UI state: `typeFilter`, `search`, `selectedCourse`, `formOpen`, `editing`, `deleting`.
- Derives `visibleRows` = client-side filter of `rows` by `typeFilter` (courseType) and `search` (name/code substring, case-insensitive).
- Renders a filter bar (type select + search input) + reused `ConfigTable`.
- When a row is "selected" (a "Manage" action), shows `PrerequisitePanel` and `CrossListingPanel` for that course id below the table.
- Create/edit uses `ConfigFormModal` with a memoized `initialValues` (A4-410 review #1 pattern). Create fields include department (campus->dept two-step) + optional prerequisites; edit fields exclude code + department.
- Lazy-loaded route.

### 5.2 `PrerequisitePanel.jsx`
- `usePrerequisites(courseId)` -> list of prerequisite course IDs; render them with the course name resolved from the loaded course list.
- Add: a course picker (from the same course list, excluding self) -> `useAddPrerequisite`. Remove: per-row -> `useRemovePrerequisite`.
- Backend cycle/non-existent errors surfaced via `mapApiError` (FR-5.3 / AC-2).

### 5.3 `CrossListingPanel.jsx`
- Add: campus->department two-step picker -> `useAddCrossListing(courseId)` POSTs `/courses/{id}/cross-listings/{departmentId}`. Remove -> DELETE.
- On success invalidates the course query so `isCrossListed` refreshes (FR-6.2 / AC-5).

### 5.4 API hooks
- `useCourses` — list/create/update/delete over `/courses`; query key `['master-data','courses','list']`; mutations invalidate it.
- `usePrerequisites(courseId)` — list + add/remove; key `['master-data','courses','prereqs',courseId]`; enabled when courseId set.
- `useCrossListings(courseId)` — add/remove; invalidates the course list (for `isCrossListed`).
- All via shared `apiClient`; BASE paths are the verified A4-3 routes (no URL-mismatch).

### 5.5 Zod schemas (`course-schemas.js`)
- `courseCreateSchema`: name(1..200), code(regex ^[A-Za-z0-9_-]+$, 1..20), departmentId(int>0), lectureHours/tutorialHours/practicalHours(int>=0), credits(number>=0.1), courseType(enum CORE|ELECTIVE|AUDIT), equipmentTags(optional string[]), prerequisiteCourseIds(optional, <=20).
- `courseEditSchema`: same minus code + departmentId (immutable).

## 6. Routing
Add route `/master-data/courses` -> lazy `CourseManagementPage` inside `AppShell`+`Suspense`; add a "Courses" nav item in `AppShell`.

## 7. Error / Empty / Loading
- `ConfigTable` handles loading skeleton, empty state, list error+retry.
- 409 duplicate-code on create, prereq cycle/non-existent, cross-listing collisions -> surfaced via `mapApiError` (form-level message or field errors). Local list not mutated on failure.

## 8. Requirement -> Design Traceability

| Requirement | Design element |
|---|---|
| FR-1 list + type filter + search + cross-listed flag | `CourseManagementPage` + client-side filter; ConfigTable column for `isCrossListed` |
| FR-2 create + Zod + 409 + prereq-on-create | ConfigFormModal + courseCreateSchema + `prerequisiteCourseIds` field |
| FR-3 edit, code/department immutable | courseEditSchema + edit fields exclude code/department |
| FR-4 delete + confirm | ConfirmDeleteDialog + delete mutation |
| FR-5 prerequisites view/add/remove | PrerequisitePanel + usePrerequisites |
| FR-6 cross-listings add/remove + designation refresh | CrossListingPanel + useCrossListings + course invalidation |
| FR-7 safe error display | mapApiError; no raw payloads |
| C-1 plain JSX / C-4 Zod / C-2 apiClient | all files |
| NFR-1 code splitting | lazy route |

## 9. Testing Strategy (later subtasks)
- Unit (Vitest): course create/edit Zod schemas (valid + each invalid branch incl. credits<0.1, bad type, code regex); client-side filter helper (type + search).
- Reused components already have their own tests (A4-340).
- Prereq/cross-listing hooks + panels: covered by build + Testing (A4-419) end-to-end.

## 10. Provisional Decisions
PD-1 client-side filter; PD-2 create-prereqs + live prereq panel; PD-3 campus->department two-step picker; PD-4 reuse components in place with memoized initialValues; PD-5 edit omits immutable code+department.

## 11. Out of Scope
Same as A4-416 §15: no backend/API/schema change, no RBAC, no scheduling, no bulk import, no other master-data frontends.

## 12. Open Questions (carried)
None blocking. PD-3's campus->department two-step is the notable UX choice; if a flat all-departments endpoint is later added, the picker can be simplified.
