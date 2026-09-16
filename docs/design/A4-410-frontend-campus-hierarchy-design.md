# A4-410 — Frontend: Campus Hierarchy Master Data (CRUD) — Design

| | |
|---|---|
| **Issue** | A4-410 (Story) |
| **Design subtask** | A4-412 |
| **Requirement** | A4-411 (Approved) |
| **Consumes backend** | A4-2 (Campus, Department, Program, Batch, Section + Hierarchy Tree) |
| **Stack** | React 18 (plain JSX), React Router v6, TanStack Query v5, Zustand, Zod, Axios |

---

## 1. Overview

Implement a master-data admin feature for the Campus → Department → Program → Batch → Section hierarchy. The design mirrors the existing **scheduling-config** feature (A4-340) and **reuses its generic presentational components** (`ConfigTable`, `ConfigFormModal`, `ConfirmDeleteDialog`) so we do not duplicate table/modal/dialog logic. Each hierarchy level is a tab with list + create/edit modal + delete confirmation, driven by TanStack Query hooks over the A4-2 REST endpoints.

## 2. Open-Question Resolutions (from A4-411) → Provisional Decisions

| OQ | Resolution (verified against A4-2 controllers) | Decision |
|----|-----------------------------------------------|----------|
| OQ#1 | Child lists DO support parent filters: `GET /departments?campusId=`, `/programs?departmentId=`, `/batches?programId=` (each `@RequestParam(required=false) + Pageable`). | **PD-1:** child tabs fetch filtered by the selected parent id. |
| OQ#2 | Sections are **nested & non-paginated**: `GET/POST /api/v1/batches/{batchId}/sections` (returns `{data:[...]}`, no `meta`); `GET/PUT/DELETE /api/v1/sections/{id}`. | **PD-2:** Section hooks use the nested list endpoint and single-resource endpoints; Section tab has no pagination. |
| OQ#3 | UI standards: modals for quick edits. All five forms are short. | **PD-3:** use the shared `ConfigFormModal` (modal) for every create/edit. |

Additional decision:
- **PD-4:** Navigation is a **parent-selector drill-down** (not a live tree widget). The page shows a Campus tab (top-level list); selecting a campus reveals Departments filtered by it, then Program/Batch/Section each filtered by the selected parent above. This satisfies FR-1 (browse hierarchy) using the paginated filtered list endpoints, and avoids depending on the tree DTO shape. The hierarchy-tree endpoints remain available but are not required for this story.

## 3. Backend Contract (consumed, not defined)

| Entity | List | Create | Get | Update | Delete |
|--------|------|--------|-----|--------|--------|
| Campus | `GET /campuses` (paged) | `POST /campuses` | `GET /campuses/{id}` | `PUT /campuses/{id}` | `DELETE /campuses/{id}` |
| Department | `GET /departments?campusId=` (paged) | `POST /departments` | `GET /departments/{id}` | `PUT /departments/{id}` | `DELETE /departments/{id}` |
| Program | `GET /programs?departmentId=` (paged) | `POST /programs` | `GET /programs/{id}` | `PUT /programs/{id}` | `DELETE /programs/{id}` |
| Batch | `GET /batches?programId=` (paged) | `POST /batches` | `GET /batches/{id}` | `PUT /batches/{id}` | `DELETE /batches/{id}` |
| Section | `GET /batches/{batchId}/sections` (list, no page) | `POST /batches/{batchId}/sections` | `GET /sections/{id}` | `PUT /sections/{id}` | `DELETE /sections/{id}` |

- Base URL: `apiClient` `/api/v1`. List → `{data, meta}` (except Section list → `{data}`). Single → `{data}`. Delete → 204.
- Errors: 400 validation, 409 conflict (duplicate code / referential-integrity delete). The global axios flow rejects; components surface a readable message.

### Entity fields (from A4-2 DTOs)
- **Campus:** id, name, code, location. Create: name, code (alphanumeric/`-`/`_`, ≤20), location. Update: name, location (code immutable).
- **Department:** id, name, code, campusId, campusName. Create: name, code, campusId. Update: name (code treated read-only on edit, matching backend "update name").
- **Program:** id, name, code, departmentId, departmentName, durationSemesters, degreeType. Create: name, code, departmentId, durationSemesters, degreeType.
- **Batch:** id, yearIdentifier, strength, electiveBasket, programId, programName. Create: yearIdentifier, strength (≥1), programId, electiveBasket (optional).
- **Section:** id, sectionIdentifier, subStrength, batchId. Create (under batch): sectionIdentifier, subStrength.

## 4. File Layout (`frontend/src/features/master-data/campus-hierarchy/`)

```
features/master-data/campus-hierarchy/
├── pages/
│   └── CampusHierarchyPage.jsx        # drill-down orchestrator (campus→dept→program→batch→section)
├── components/
│   ├── CampusTab.jsx                  # top-level list + CRUD
│   ├── DepartmentTab.jsx              # filtered by campusId
│   ├── ProgramTab.jsx                 # filtered by departmentId
│   ├── BatchTab.jsx                   # filtered by programId
│   └── SectionTab.jsx                 # nested under batchId (non-paged)
├── api/
│   ├── useCampuses.js
│   ├── useDepartments.js
│   ├── usePrograms.js
│   ├── useBatches.js
│   └── useSections.js
├── schemas/
│   └── hierarchy-schemas.js           # Zod schemas per entity
├── constants/
│   └── hierarchy-options.js           # degreeType options, etc.
└── campus-hierarchy.css
```

**Reused (no new copies):** `ConfigTable`, `ConfigFormModal`, `ConfirmDeleteDialog` from A4-340. See PD-5 for their location decision.

## 5. Component & Hook Design

### 5.1 Page — `CampusHierarchyPage.jsx`
- Renders a breadcrumb/selector chain. Level state held in page-local `useState` (selectedCampusId, selectedDepartmentId, selectedProgramId, selectedBatchId).
- Always shows `CampusTab`. When a campus is selected → shows `DepartmentTab(campusId)`; when a department is selected → `ProgramTab(departmentId)`; then `BatchTab(programId)`; then `SectionTab(batchId)`.
- Selecting a row at level N sets the parent id for level N+1 and clears deeper selections.
- Lazy-loaded via `React.lazy` in the router (NFR-1).

### 5.2 Entity tabs (Campus/Department/Program/Batch)
Each mirrors `DerivationRuleTab`: defines `COLUMNS`, `FIELDS`, `BLANK`; wires list/create/update/delete hooks; renders reused `ConfigTable` + `ConfigFormModal` + `ConfirmDeleteDialog`. `rows = list.data?.data ?? []`.
- Create/Update mutations pass the parent id (e.g., DepartmentTab injects `campusId`).
- On mutation success: close modal; TanStack Query invalidates the list key → auto-refresh (FR-3.3).
- Edit forms mark immutable fields read-only (Campus code; Department code) (FR-4.2).

### 5.3 SectionTab (special — PD-2)
- Uses `useSections(batchId)` → GET `/batches/{batchId}/sections` returning `{data:[...]}` (no pagination UI).
- Create posts to `/batches/{batchId}/sections`; update/delete use `/sections/{id}`.

### 5.4 API hooks (per entity) — TanStack Query
- Query keys: `['master-data', <entity>, 'list', <parentId?>]`.
- `useXxx(parentId)` — GET list; `enabled` only when the required parent id is present (child entities).
- `useCreateXxx / useUpdateXxx / useDeleteXxx` — mutations; `onSuccess` → `invalidateQueries` on the list key.
- All via shared `apiClient`; **BASE paths are the confirmed A4-2 routes** (avoids the A4-390 URL-mismatch class of bug).
- Error surfaced by the tab (inline for 400 field errors where available; toast/message otherwise) (FR-6).

### 5.5 Zod schemas (`hierarchy-schemas.js`)
One schema per entity mirroring backend validation:
- campus: name (1–200), code (regex `^[A-Za-z0-9_-]+$`, 1–20), location (1–500).
- department: name, code, campusId (positive int).
- program: name, code, departmentId, durationSemesters (int > 0), degreeType (non-empty).
- batch: yearIdentifier (1–20), strength (int ≥ 1), programId, electiveBasket (optional ≤ 200).
- section: sectionIdentifier (non-empty), subStrength (int > 0).

## 6. Routing
Add a route in `app/router.jsx`: `/master-data/campus-hierarchy` → lazy `CampusHierarchyPage` inside `AppShell`, matching the existing lazy+Suspense pattern. Add a nav entry (sidebar) if the shell exposes one.

## 7. Error & Empty/Loading States
- Loading: `ConfigTable` already renders a loading state; pass `isLoading`.
- Empty: `ConfigTable` shows empty text; provide entity-specific label + Add action.
- Error on list: `onRetry={list.refetch}`.
- 409 on create (duplicate code) / delete (referential integrity): surface the server message; do not mutate local list (FR-3.4, FR-5.3).

## 8. Requirement → Design Traceability

| Requirement | Design element |
|---|---|
| FR-1 browse hierarchy | `CampusHierarchyPage` drill-down (PD-4) |
| FR-2 list per entity (paged, parent-filtered) | entity tabs + `useXxx(parentId)` hooks; Section non-paged (PD-2) |
| FR-3 create + Zod + 409 | `ConfigFormModal` + schemas + mutation onError |
| FR-4 edit, immutable fields | tabs' edit mode, read-only code fields |
| FR-5 delete + confirm + referential-integrity | `ConfirmDeleteDialog` + mutation onError |
| FR-6 error/feedback, no raw errors | tab-level message/toast; global handler |
| C-1 plain JSX | all `.jsx`/`.js` |
| C-2 shared apiClient | all hooks use `apiClient` |
| C-4 Zod | `hierarchy-schemas.js` |
| NFR-1 code splitting | `React.lazy` route |
| NFR-2 a11y | reused modal/table already keyboard+ARIA (verify in review) |

## 9. Testing Strategy (for later subtasks)
- Unit (Vitest): each Zod schema (valid/invalid), each tab's row-mapping + submit wiring (mock hooks), Section non-paged path.
- Reuse existing `ConfigTable/ConfigFormModal/ConfirmDeleteDialog` tests (already present) — if promoted to `components/ui/`, move their tests too.

## 10. Provisional Decisions (pending stakeholder ratification)

| PD | Decision |
|----|----------|
| PD-1 | Child tabs fetch filtered by selected parent id. |
| PD-2 | Section uses nested non-paged endpoints. |
| PD-3 | Modal forms for all create/edit. |
| PD-4 | Drill-down parent-selector navigation (not a live tree widget); tree endpoints not required. |
| PD-5 | Promote `ConfigTable`/`ConfigFormModal`/`ConfirmDeleteDialog` to `src/components/ui/` and re-import in both features; fall back to in-place import if the move risks A4-340's approved code. |

## 11. Out of Scope
Same as A4-411 §15: no backend changes, no RBAC, no scheduling, no bulk import, no other master-data frontends.

## 12. Open Questions (carried)
None blocking. PD-5 (component location) is the only choice with a fallback; will be finalized during implementation and noted in the code-review doc.
