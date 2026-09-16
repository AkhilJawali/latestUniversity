# A4-435 — Frontend Schedulable Asset Master Data (CRUD): Design Document

- **Story:** A4-435 — Frontend Schedulable Asset Master Data (CRUD)
- **Design Derivation subtask:** A4-437
- **Requirement source:** docs/requirements/A4-435-frontend-schedulable-asset-master-data-requirements.md (A4-436, Approved)
- **Consumes backend:** A4-7 (`/api/v1/assets`), A4-2/A4-410 (campus + department lists)
- **Date:** 2026-09-04

## 1. Overview

A new frontend feature module `features/master-data/asset-management/` providing schedulable-asset CRUD (with an embedded per-asset availability-window list) against the A4-7 REST API. Follows the A4-410/415/420/425/430 precedent — reuse `ConfigTable`, `ConfirmDeleteDialog`, `validateWith`, `mapApiError`; add a lazy route + nav item. The create/edit form is a custom modal because it needs department + campus pickers (create-only) and an editable window sub-list submitted inline (whole-set replacement).

## 2. Resolved Open Questions (lead-approved, from requirement §16 / OQ log)

| OQ | Decision |
|----|----------|
| A435-OQ-1 | assetType is a free-text field (no curated dropdown). |
| A435-OQ-2 | Filters = campus, department, assetType; no free-text search box. |
| A435-OQ-3 | Show a client-side window-count indicator; no server availability-status column. |
| A435-OQ-4 | Edit form shows identifier / owning department / campus read-only (immutable). |
| A435-OQ-5 | No maintenance-period UI (deferred). |
| A435-OQ-6 | Enforce end-after-start client-side on each window. |
| A435-OQ-7 | Frontend mirrors the 7 uppercase day names. |
| A435-OQ-8 | In-form window list edited locally; whole-set replacement on asset save. |

## 3. Design Decisions

- **PD-1:** Reuse `ConfigTable`, `ConfirmDeleteDialog`, `validateWith`, `mapApiError` in place (prior precedent).
- **PD-2:** Asset create/edit is a custom `AssetFormModal` (native `<dialog>`) — needs department + campus pickers (create-only) and an embedded editable availability-window list (add/edit/remove rows). Runs the Zod schema via `validateWith`; maps backend errors via `mapApiError`. Memoized `initialValues`; delete via `ConfirmDeleteDialog` with `onError` (prior lessons).
- **PD-3:** Server-side filtering — the page holds filter state (campusId, departmentId, assetType) + page, passed as query params to the paged `GET /assets`; query key includes them.
- **PD-4:** Department + campus pickers reuse `useCampuses` + `useDepartments(campusId)` (campus→department two-step, as in A4-420). On edit, both render read-only alongside identifier.
- **PD-5:** Windows edited as an in-form array of `{dayOfWeek, startTime, endTime}` rows; on submit the full array is sent as `availabilityWindows` in the asset `POST`/`PUT` (whole-set replacement, OQ-8). No per-window API calls.
- **PD-6:** Pagination controls from `meta` (PD-6 precedent from A4-430).

## 4. Module Structure

```
features/master-data/asset-management/
  constants/asset-options.js       # DAYS_OF_WEEK (7 names)
  schemas/asset-schemas.js         # assetCreateSchema, assetEditSchema, windowSchema (+ test)
  api/useAssets.js                 # list (paged+filters), create, update, delete
  components/AssetFormModal.jsx    # custom create/edit modal (dept/campus pickers, window list)
  pages/AssetManagementPage.jsx    # filters + table + pagination + modal
  asset-management.css
```
Plus: lazy route `/master-data/assets`, "Assets" nav item.

## 5. Component & Data Design

### 5.1 API hooks (`useAssets.js`, TanStack Query, apiClient baseURL `/api/v1`)
- `useAssets(params)` — `GET /assets` with `{campusId?, departmentId?, assetType?, page?, size?}`; returns `{data, meta}`. Query key includes params.
- `useCreateAsset` — `POST /assets` (201, body includes availabilityWindows); invalidate list.
- `useUpdateAsset` — `PUT /assets/{id}` (name, assetType, availabilityWindows only); invalidate list.
- `useDeleteAsset` — `DELETE /assets/{id}` (204); invalidate list.

### 5.2 Schemas (`asset-schemas.js`)
- `windowSchema`: dayOfWeek (enum DAYS_OF_WEEK), startTime (`HH:mm` regex), endTime (`HH:mm` regex), with a refine that endTime is strictly after startTime (OQ-6, client-side).
- `assetCreateSchema`: name(1..200), identifier(1..50, `^[A-Za-z0-9_-]+$`), assetType(1..50), owningDepartmentId(coerce int positive), campusId(coerce int positive), availabilityWindows(array of windowSchema, optional).
- `assetEditSchema`: name, assetType, availabilityWindows (no identifier / department / campus — immutable per UpdateAssetRequest).

### 5.3 AssetFormModal (custom)
- Props: open, mode, initialValues, currentDepartmentName, currentCampusName, campuses, isPending, onSubmit, onClose.
- Create renders: name, identifier, assetType (text), campus `<select>` → department `<select>` (two-step, PD-4), and a windows editor (rows of day `<select>` + start/end `<input type="time">` + remove; an "Add window" button).
- Edit renders: name, assetType, the windows editor — identifier + department + campus shown read-only.
- Submit: `validateWith(schema, values)` (windows validated per-row) → on failure set errors, no request (AC-4); on success `onSubmit(data, {onError})` mapping backend field/message errors (409 duplicate identifier → message).

### 5.4 AssetManagementPage
- Filter bar: campus `<select>` (useCampuses), department `<select>` (useDepartments for the chosen campus), assetType text. Filters drive `useAssets(params)` (PD-3).
- `ConfigTable` columns: name, identifier, assetType, owningDepartmentName, campusName, windows (count — "N windows", OQ-3). Edit/Delete actions. `searchable={false}` (server-side filtering).
- Pagination controls from `meta` (PD-6).
- Create/Edit via `AssetFormModal` with memoized `initialValues`; delete via `ConfirmDeleteDialog` with `onError`.
- Loading skeleton / empty state / retry via `ConfigTable`.

## 6. Error Handling

- Form submit: client Zod first (no request on invalid); backend 400 → field errors, 409 (duplicate identifier) → message, 404 (bad campus/department) → message, via `mapApiError`.
- Delete: `onError` surfaces reason (future A4-8 422 active-reference block); dialog stays open.

## 7. Accessibility & Security

- Labels on all controls; `aria-label` on icon-only buttons (incl. window row remove); native `<dialog>` focus trap + Escape; inline errors `role="alert"` + `aria-describedby`.
- No `dangerouslySetInnerHTML`; escaped JSX. Plain JSX, PropTypes, enforced import order.

## 8. Testing Strategy

- Unit tests for `asset-schemas.js`: create schema (each field, identifier regex, required dept/campus), edit schema (immutable fields absent), windowSchema (day enum, HH:mm, end-after-start refine). Verify via full vitest suite + vite build (new lazy chunk).

## 9. Traceability

| Requirement | Design element |
|-------------|----------------|
| FR-1 (list + filters) | 5.1 useAssets, 5.4 page filters + pagination, ConfigTable |
| FR-2 (create) | 5.2 assetCreateSchema, 5.3 AssetFormModal (create) |
| FR-3 (validation) | 5.2 schemas (incl. windowSchema end-after-start) |
| FR-4 (availability windows, list-based) | 5.2 windowSchema, 5.3 windows editor, PD-5 whole-set replacement |
| FR-5 (edit, immutable identifier/dept/campus) | 5.2 assetEditSchema, 5.3 read-only fields |
| FR-6 (soft-delete) | 5.4 ConfirmDeleteDialog + onError |
| NFR-1..4 | §6, §7 |

## 10. Out of Scope / Carried Items

- All A4-435 OQs are resolved/deferred (§2); none carried as unresolved.
- Backend/API change, resource-blocking workflow (A4-8), rooms (A4-430), RBAC, scheduling — out of scope.
