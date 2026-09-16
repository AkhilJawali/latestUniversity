# A4-430 — Frontend Room and Lab Master Data (CRUD): Design Document

- **Story:** A4-430 — Frontend Room and Lab Master Data (CRUD)
- **Design Derivation subtask:** A4-432
- **Requirement source:** docs/requirements/A4-430-frontend-room-and-lab-master-data-requirements.md (A4-431, Approved)
- **Consumes backend:** A4-6 (`/api/v1/rooms`), A4-2/A4-410 (campus list for the picker/filter)
- **Date:** 2026-09-04

## 1. Overview

A new frontend feature module `features/master-data/room-management/` providing room/lab CRUD against the A4-6 REST API. Follows the A4-410/415/420/425 precedent — reuse `ConfigTable`, `ConfirmDeleteDialog`, `validateWith`, `mapApiError`; add a lazy route + nav item. The create/edit form is a small custom modal because it needs an equipment-tags multi-value input and a campus picker that is create-only (immutable on edit).

## 2. Resolved Open Questions (lead-approved, from requirement §16 / OQ log)

| OQ | Decision |
|----|----------|
| A430-OQ-1 | Use the available server filters (campus, type, building, minCapacity, single equipment tag); no free-text search box. |
| A430-OQ-2 | building/floor are free-text inputs; building filter driven by distinct values in the loaded rooms. |
| A430-OQ-3 | Equipment-tag filter is a single tag (exact match). |
| A430-OQ-4 | No active/inactive column (RoomDto has no isActive; only active rooms are returned). |

## 3. Design Decisions

- **PD-1:** Reuse `ConfigTable`, `ConfirmDeleteDialog`, `validateWith`, `mapApiError` in place (prior precedent).
- **PD-2:** Room create/edit is a custom `RoomFormModal` (native `<dialog>`) — needs a multi-value equipment-tags input and create-only code + campus fields (both immutable on edit). Runs the Zod schema via `validateWith`; maps backend errors via `mapApiError`. Memoized `initialValues`; delete via `ConfirmDeleteDialog` with `onError` surfacing the reason (prior lessons).
- **PD-3:** Server-side filtering — the page holds filter state (campusId, roomType, building, minCapacity, equipmentTag) and passes it as query params to `GET /rooms`; the query key includes the filters so changing them refetches. This matches the paged `{data,meta}` contract (unlike the client-side filters used for the small non-paged lists in prior stories).
- **PD-4:** Campus picker/filter reuses `useCampuses` (from campus-hierarchy). Room type + the building filter options are frontend-derived (fixed enum; distinct building values from the current page).
- **PD-5:** Equipment tags entered as a simple comma/enter-driven tag input (chips); sent as `equipmentTags: string[]`. On edit, code + campus render read-only.
- **PD-6:** Pagination — render page controls driven by `meta` (this story wires real pagination since the endpoint is paged, addressing the gap noted in prior faculty story reviews).

## 4. Module Structure

```
features/master-data/room-management/
  constants/room-options.js       # ROOM_TYPES (4 enum values)
  schemas/room-schemas.js         # roomCreateSchema, roomEditSchema (+ test)
  api/useRooms.js                 # list (paged+filters), create, update, delete
  components/RoomFormModal.jsx    # custom create/edit modal (tags input, create-only code/campus)
  pages/RoomManagementPage.jsx    # filters + table + pagination + modal
  room-management.css
```
Plus: lazy route `/master-data/rooms`, "Rooms" nav item.

## 5. Component & Data Design

### 5.1 API hooks (`useRooms.js`, TanStack Query, apiClient baseURL `/api/v1`)
- `useRooms(params)` — `GET /rooms` with `{campusId?, roomType?, building?, minCapacity?, equipmentTag?, page?, size?}`; returns `{data, meta}`. Query key includes params.
- `useCreateRoom` — `POST /rooms` (201); invalidate list.
- `useUpdateRoom` — `PUT /rooms/{id}` (name, capacity, roomType, equipmentTags, building, floor only); invalidate list.
- `useDeleteRoom` — `DELETE /rooms/{id}` (204); invalidate list.

### 5.2 Schemas (`room-schemas.js`)
- `roomCreateSchema`: name(1..200), code(1..20, `^[A-Za-z0-9_-]+$`), campusId(coerce int positive), capacity(coerce int ≥1), roomType(enum ROOM_TYPES), equipmentTags(array of strings, optional), building(≤100 optional), floor(≤20 optional).
- `roomEditSchema`: name, capacity, roomType, equipmentTags, building, floor (no code / campusId — immutable per UpdateRoomRequest).
- Helper `parseTags(str)` to turn comma/newline input into a trimmed string array (and `formatTags` back).

### 5.3 RoomFormModal (custom)
- Props: open, mode, initialValues, campuses, isPending, onSubmit, onClose.
- Create renders: name, code, campus `<select>` (from campuses), capacity (number), room type `<select>` (ROOM_TYPES), equipment tags (chip input), building, floor.
- Edit renders: name, capacity, room type, equipment tags, building, floor — code + campus shown read-only.
- Submit: `validateWith(schema, values)` → on failure set field errors, no request (AC-4); on success `onSubmit(data, {onError})` mapping backend field/message errors (409 duplicate code → message).

### 5.4 RoomManagementPage
- Filter bar: campus `<select>` (useCampuses), room type `<select>` (ROOM_TYPES), building `<select>` (distinct from current rows) or text, min-capacity number, equipment-tag text. Filters drive `useRooms(params)` (PD-3).
- `ConfigTable` columns: name, code, campus, capacity, type, equipment tags (joined), building, floor. Edit/Delete actions.
- Pagination controls from `meta` (PD-6).
- Create/Edit via `RoomFormModal` with memoized `initialValues`; delete via `ConfirmDeleteDialog` with `onError`.
- Loading skeleton / empty state / retry via `ConfigTable`.

## 6. Error Handling

- Form submit: client Zod first (no request on invalid); backend 400 → field errors, 409 (duplicate code within campus) → message, via `mapApiError`.
- Delete: `onError` surfaces reason; dialog stays open.

## 7. Accessibility & Security

- Labels on all controls; `aria-label` on icon-only buttons; native `<dialog>` focus trap + Escape; inline errors `role="alert"`; error text tied via `aria-describedby`.
- No `dangerouslySetInnerHTML`; escaped JSX. Plain JSX, PropTypes, enforced import order.

## 8. Testing Strategy

- Unit tests for `room-schemas.js`: create schema (each field, code regex, capacity ≥1, roomType enum), edit schema (code/campus absent), and `parseTags`/`formatTags`. Verify via full vitest suite + vite build (new lazy chunk).

## 9. Traceability

| Requirement | Design element |
|-------------|----------------|
| FR-1 (list + filters) | 5.1 useRooms, 5.4 page filters + pagination, ConfigTable |
| FR-2 (create) | 5.2 roomCreateSchema, 5.3 RoomFormModal (create) |
| FR-3 (edit, code/campus immutable) | 5.2 roomEditSchema, 5.3 RoomFormModal (edit, read-only code/campus) |
| FR-4 (equipment tags) | 5.2 parseTags, 5.3 tag input, 5.4 tag filter |
| FR-5 (soft-delete) | 5.4 ConfirmDeleteDialog + onError |
| FR-6 (room type options) | 5.2 ROOM_TYPES constant |
| NFR-1..4 | §6, §7 |

## 10. Out of Scope / Carried Items

- All A4-430 OQs are resolved (§2); none carried as unresolved.
- Backend/API change, schedulable assets (A4-435), RBAC, scheduling/allocation — out of scope.
