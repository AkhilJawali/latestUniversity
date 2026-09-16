# A4-445 — Frontend Time-Slot Grid Configuration (CRUD): Design Document

- **Story:** A4-445 — Frontend — Time-Slot Grid Configuration (CRUD)
- **Design Derivation subtask:** A4-447
- **Requirement source:** docs/requirements/A4-445-frontend-time-slot-grid-configuration-requirements.md (A4-446, Approved)
- **Consumes backend:** A4-10 (`/api/v1/time-slots`), A4-2/A4-410 (campus list for the selector)
- **Date:** 2026-09-09

## 1. Overview

A new frontend feature module `features/master-data/time-slot-grid/` providing, for a selected campus: view/create/rename/delete the campus's single time-slot grid, and add/remove slot definitions (teaching/break/lunch) with start/end times, an optional day override, and an optional label. Follows the A4-410/415/420/425 precedent — reuse `ConfigTable`, `ConfirmDeleteDialog`, `validateWith`, `mapApiError`; add a lazy route + nav item. Slot create is a small custom modal (native `<dialog>`) because it needs `<input type="time">` fields plus type/day selects that the generic `ConfigFormModal` doesn't provide. Because the backend exposes **no slot-edit** endpoint, "edit" is modelled as remove-then-add (OQ-1).

## 2. Resolved Open Questions (lead-approved via requirement §16 approval)

| OQ | Decision |
|----|----------|
| A445-OQ-1 | No backend slot-edit endpoint → the UI offers **remove-then-add** (an "Edit" action opens the add-modal prefilled; on save it removes the old slot then adds the new one). No in-place slot `PUT`. |
| A445-OQ-2 | Frontend mirrors `slotType` constants {TEACHING, BREAK, LUNCH}. |
| A445-OQ-3 | Frontend mirrors day constants {MONDAY..SUNDAY} plus an "All days" (null) option. |
| A445-OQ-4 | UI allows **any positive duration** (end-after-start only); it does not restrict to 60/90/180. Duration is displayed read-only from the backend `durationMinutes`. |
| A445-OQ-5 | No role gating in this UI (backend `permitAll`). |
| A445-OQ-6 | Feedback via existing inline message regions (consistent with A4-425); no new toast system introduced. |
| A445-OQ-7 | UI surfaces whatever the backend allows today; no extra client-side session-reference guard. |
| A445-OQ-8 | Day UX: a single **all-days timeline** with day-override badges, plus a per-day **preview** via `GET /{gridId}/effective?day=` (no full 7-day grid). |

## 3. Design Decisions

- **PD-1:** Reuse `ConfigTable` (slot list), `ConfirmDeleteDialog` (remove slot / delete grid), `validateWith` (`@/features/scheduling-config/schemas/config-schemas`), `mapApiError` (`@/lib/api-error`) in place — prior precedent.
- **PD-2:** Campus selector at the top reuses `useCampuses` (`@/features/master-data/campus-hierarchy/api/useCampuses`). The whole screen is scoped to the selected `campusId`.
- **PD-3:** Grid load uses `GET /time-slots/campus/{campusId}`. A **404 is treated as "no grid"** (empty state), not an error — the api hook maps 404 → `null` grid rather than throwing (FR-1.2).
- **PD-4:** Slot create/edit is a custom `SlotDefinitionModal` (native `<dialog>`) — needs two `<input type="time">`, a `slotType` `<select>`, an `applicableDay` `<select>` (with "All days"), and an optional label input. Runs the Zod schema via `validateWith`; maps backend errors via `mapApiError`. Memoized `initialValues`. In edit mode, submit performs remove-then-add (OQ-1).
- **PD-5:** Grid create is a small `GridCreateModal` — grid name + an in-modal slot list the admin builds before first save (must contain ≥1 slot with ≥1 TEACHING, pre-checked client-side, FR-4.2). Submits one `POST /time-slots` with `campusId` + `gridName` + `slots`.
- **PD-6:** Grid rename is an inline edit (single field) → `PUT /time-slots/{id}` sending `gridName` only.
- **PD-7:** Time handling — `<input type="time">` yields `HH:mm`; sent as-is (backend `LocalTime` accepts `HH:mm`). On read, display the `HH:mm[:ss]` string trimmed to `HH:mm`.
- **PD-8:** Break/lunch visual distinction (FR-3.2/AC-3) — a `slotType` badge + row class on `ConfigTable`; distinct color for BREAK/LUNCH vs TEACHING, meeting 3:1 contrast.
- **PD-9:** "View by day" is a day `<select>` that, when set, swaps the table source from the grid's raw slots to `GET /{gridId}/effective?day=` results (a read-only preview; add/edit/remove actions are disabled in preview mode to avoid ambiguity about which day scope a new slot would target).

## 4. Module Structure

```
features/master-data/time-slot-grid/
  constants/time-slot-options.js       # SLOT_TYPES, DAY_OPTIONS (incl. ALL_DAYS)
  schemas/time-slot-schemas.js         # slotDefinitionSchema, gridCreateSchema, gridRenameSchema (+ test)
  api/
    useTimeSlotGrid.js                 # getByCampus (404→null), create, renameGrid, deleteGrid
    useSlotDefinitions.js              # addSlot, removeSlot, effectiveByDay
  components/
    GridCreateModal.jsx                # grid name + initial slot list (≥1 teaching)
    SlotDefinitionModal.jsx            # add/edit (edit = remove-then-add) slot modal
    SlotTimeline.jsx                   # ConfigTable-based slot list with type badges
  pages/TimeSlotGridPage.jsx           # campus selector + grid header + timeline + day preview
  time-slot-grid.css
```
Plus: lazy route `/master-data/time-slot-grid` in `app/router.jsx`, "Time-Slot Grid" nav item in `AppShell`.

## 5. Component & Data Design

### 5.1 API hooks (TanStack Query, `apiClient` baseURL `/api/v1`)
- **useTimeSlotGrid(campusId)**
  - `useGridByCampus(campusId)` — `GET /time-slots/campus/{campusId}` → `{data:{...}}`; **404 mapped to `null`** (no grid); `enabled: campusId != null`.
  - `useCreateGrid()` — `POST /time-slots` body `{campusId, gridName, slots:[...]}`; invalidate grid-by-campus.
  - `useRenameGrid()` — `PUT /time-slots/{id}` body `{gridName}`; invalidate grid-by-campus.
  - `useDeleteGrid()` — `DELETE /time-slots/{id}` (204); invalidate grid-by-campus.
- **useSlotDefinitions(gridId)**
  - `useAddSlot(gridId)` — `POST /time-slots/{gridId}/slots`; invalidate grid-by-campus.
  - `useRemoveSlot(gridId)` — `DELETE /time-slots/{gridId}/slots/{slotId}` (204); invalidate grid-by-campus.
  - `useEffectiveSlots(gridId, day)` — `GET /time-slots/{gridId}/effective?day={DAY}` → `{data:[...]}`; `enabled: gridId != null && day != null`.

### 5.2 Schemas (`time-slot-schemas.js`)
- `slotDefinitionSchema`: startTime (`HH:mm` regex), endTime (`HH:mm` regex), slotType (enum SLOT_TYPES), applicableDay (optional; "" / ALL_DAYS sentinel → sent as null, else enum day), label (optional ≤100). Cross-field refine: endTime strictly after startTime (string compare works for zero-padded `HH:mm`).
- `gridCreateSchema`: gridName (1..200), slots (array, min 1, refine "≥1 TEACHING").
- `gridRenameSchema`: gridName (1..200).

### 5.3 GridCreateModal
- Props: open, campusId, isPending, onSubmit, onClose.
- Fields: grid name input; an editable in-modal slot list (add/remove rows using the same slot fields as 5.4). Client pre-check via `gridCreateSchema` (≥1 slot, ≥1 TEACHING, FR-4.2) before submit; on success `onSubmit({campusId, gridName, slots}, {onError})` mapping backend errors (409 duplicate grid, 409 overlap, 422 no-teaching / start≥end).

### 5.4 SlotDefinitionModal (add / edit)
- Props: open, mode ('add'|'edit'), gridId, initialValues, isPending, onSubmit, onClose.
- Fields: start `<input type="time">`, end `<input type="time">`, slotType `<select>` (SLOT_TYPES), applicableDay `<select>` (DAY_OPTIONS incl. "All days"), optional label input.
- Submit: `validateWith(slotDefinitionSchema, values)` → on failure set field errors, no request (AC-5). On success:
  - **add:** `useAddSlot` POST; surface 409 overlap inline via `mapApiError` (AC-2).
  - **edit (OQ-1):** `useRemoveSlot(oldId)` then `useAddSlot(new values)`. If the add step fails (e.g., 409 overlap), the modal stays open with the entered values and the mapped error so the admin can correct and retry the add (which re-creates the slot). The trade-off — a transient window where the old slot is removed before the new one lands — is documented in §6; a future backend slot-`PUT` would eliminate it.

### 5.5 SlotTimeline
- `ConfigTable` (columns: start, end, duration (read-only from `durationMinutes`), type badge, day scope ["All days" or weekday], label) sorted by start time. Row/badge styling distinguishes BREAK/LUNCH from TEACHING (PD-8, AC-3). Row actions: Edit (opens SlotDefinitionModal prefilled) and Remove (ConfirmDeleteDialog). In day-preview mode the source is `useEffectiveSlots` and add/edit/remove are disabled (PD-9).

### 5.6 TimeSlotGridPage
- Campus selector (`useCampuses`) → sets `campusId`.
- When a campus is selected and **has a grid**: a header showing grid name with inline **Rename** and **Delete grid** (ConfirmDeleteDialog warning "all slots will be removed", FR-2.5); an **Add slot** button; a day `<select>` for preview (PD-9); and `SlotTimeline`.
- When the campus has **no grid** (grid === null): empty state + **Create grid** CTA opening `GridCreateModal` (FR-2.2). Create is not shown when a grid already exists (FR-2.6); a 409 from a race is surfaced inline.
- Loading skeleton / list-error retry via `ConfigTable` (FR-6.1).

## 6. Error Handling

- Slot form / grid create: client Zod first (no request on invalid, AC-5); backend 409 overlap (day-aware) → inline message via `mapApiError` (AC-2); 422 start≥end / no-teaching → inline message; 409 duplicate grid on create → inline message.
- Remove slot: `ConfirmDeleteDialog` `onError` surfaces the reason (e.g., 422 last-teaching-slot, AC-6); dialog stays open.
- Delete grid: `ConfirmDeleteDialog` `onError` surfaces any backend error; on success return to empty state (AC-7).
- Edit (remove-then-add) caveat: if the add step fails after the remove succeeded, the UI shows the mapped error and keeps the modal open so the admin can retry the add (re-creating the slot). No partial silent loss — the error is explicit. (Known trade-off of OQ-1; a future backend slot-`PUT` would remove this.)
- All errors mapped via `mapApiError`; no stack traces or raw payloads shown (NFR-1).

## 7. Accessibility & Security

- Labels on all controls; `aria-label` on icon-only Edit/Remove buttons; native `<dialog>` focus trap + Escape; inline errors `role="alert"`; timeline table keyboard-navigable and screen-reader readable (NFR-2).
- No `dangerouslySetInnerHTML`; escaped JSX. Plain JSX, PropTypes, enforced import order (NFR-1, NFR-4).
- Break/lunch color distinction meets contrast minimums (PD-8).

## 8. Testing Strategy

- Unit tests for `time-slot-schemas.js`: `slotDefinitionSchema` (each field, `HH:mm` format, end-after-start refine, applicableDay optional/ALL_DAYS→null), `gridCreateSchema` (min 1 slot, ≥1 TEACHING refine), `gridRenameSchema` (name bounds). Verify via full vitest suite + vite build (new lazy chunk). Route/nav smoke as per prior stories.

## 9. Traceability

| Requirement | Design element |
|-------------|----------------|
| FR-1 (campus context, 404→empty) | 5.6 campus selector via useCampuses; 5.1 useGridByCampus (404→null); PD-2/PD-3 |
| FR-2 (grid create/rename/delete) | 5.1 useCreateGrid/useRenameGrid/useDeleteGrid; 5.3 GridCreateModal; 5.6 header rename+delete; PD-5/PD-6 |
| FR-2.6 (one grid per campus) | 5.6 create hidden when grid exists; 409 surfaced |
| FR-3 (slot add/list/remove, timeline, break/lunch) | 5.1 useAddSlot/useRemoveSlot; 5.4 SlotDefinitionModal; 5.5 SlotTimeline; PD-8 |
| FR-3.5 (no slot edit → remove-then-add) | 5.4 edit mode; §6 caveat; OQ-1 |
| FR-3.6 (view by day) | 5.1 useEffectiveSlots; 5.5 preview mode; PD-9 |
| FR-4 (client validation, ≥1 teaching) | 5.2 schemas; 5.3 gridCreate pre-check |
| FR-5 (server errors: overlap/last-teaching/start-end) | §6; 5.4/5.5 |
| FR-6 (states/feedback/confirm) | 5.5/5.6 skeleton/empty/retry; ConfirmDeleteDialog; OQ-6 |
| NFR-1..4 | §6, §7, §8 |

## 10. Out of Scope / Carried Items

- All A4-445 OQs are resolved (§2); none carried as unresolved.
- Backend/API/schema change (A4-10), academic calendar frontend (A4-440/A4-9), RBAC, scheduling/drag-drop editor — out of scope.
- Session-reference impact on removal is a backend TODO (KD-43); the UI surfaces current backend behavior only (OQ-7).
