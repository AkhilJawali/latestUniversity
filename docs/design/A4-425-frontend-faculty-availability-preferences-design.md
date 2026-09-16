# A4-425 — Frontend Faculty Availability and Preferences (CRUD): Design Document

- **Story:** A4-425 — Frontend Faculty Availability and Preferences (CRUD)
- **Design Derivation subtask:** A4-427
- **Requirement source:** docs/requirements/A4-425-frontend-faculty-availability-preferences-requirements.md (A4-426, Approved)
- **Consumes backend:** A4-5 (`/api/v1/faculty/{facultyId}/availability` + `/preferences`), A4-4/A4-420 (faculty list for the selector)
- **Date:** 2026-09-04

## 1. Overview

A new frontend feature module `features/master-data/faculty-availability/` providing, for a selected faculty: CRUD over availability (unavailability) windows and an upsert preferences form. Follows the A4-410/415/420 precedent — reuse `ConfigTable`, `ConfirmDeleteDialog`, `validateWith`, `mapApiError`; add a lazy route + nav item. The window create/edit form is a small custom modal (native `<dialog>`) because it needs `<input type="time">` fields the generic `ConfigFormModal` doesn't provide.

## 2. Resolved Open Questions (lead-approved, from the requirement §16 / OQ log)

| OQ | Decision |
|----|----------|
| A425-OQ-1 | Windows = hard section, preferences = soft section (no per-window flag). |
| A425-OQ-2 | Windows labelled "unavailability" by default (mode not exposed by backend). |
| A425-OQ-3 | Frontend mirrors the 7 day-name constants. |
| A425-OQ-4 | Preferred time-of-day = MORNING / AFTERNOON / NO_PREFERENCE only. |
| A425-OQ-5 | Preference weighting deferred (no UI). |
| A425-OQ-6 | `reasonCode` is a free-text field with a short helper. |

## 3. Design Decisions

- **PD-1:** Reuse `ConfigTable`, `ConfirmDeleteDialog`, `validateWith`, `mapApiError` in place (prior precedent).
- **PD-2:** Window create/edit is a custom `AvailabilityWindowModal` (native `<dialog>`) — needs a day `<select>`, two `<input type="time">`, a reason-code text input, and an optional note textarea. Runs the Zod schema via `validateWith`; maps backend errors via `mapApiError`. Memoized `initialValues` (prior lesson); delete via `ConfirmDeleteDialog` with `onError` surfacing the reason (prior lesson).
- **PD-3:** Faculty selector at the top of the page reuses `useFacultyList` (from faculty-management) — the whole screen is scoped to the selected faculty id.
- **PD-4:** Preferences are an inline form (two selects), not a modal — a single upsert `PUT`. Loaded via GET (which returns a default object when unset, so the form always has values). Its own inline save button + status message.
- **PD-5:** Time handling — `<input type="time">` yields `HH:mm`; sent as-is (backend `LocalTime` accepts `HH:mm`). On read, display the `HH:mm[:ss]` string trimmed to `HH:mm`.

## 4. Module Structure

```
features/master-data/faculty-availability/
  constants/availability-options.js   # DAYS_OF_WEEK, TIME_OF_DAY, SESSION_DISTRIBUTION
  schemas/availability-schemas.js      # availabilityWindowSchema, preferenceSchema (+ test)
  api/
    useAvailabilityWindows.js          # list/create/update/delete (nested under facultyId)
    useFacultyPreferences.js           # get + upsert(put)
  components/
    AvailabilityWindowModal.jsx        # custom create/edit modal
    PreferencesForm.jsx                # inline soft-preferences form
  pages/FacultyAvailabilityPage.jsx    # faculty selector + windows table + prefs form
  faculty-availability.css
```
Plus: lazy route `/master-data/faculty-availability`, "Faculty Availability" nav item.

## 5. Component & Data Design

### 5.1 API hooks (TanStack Query, apiClient baseURL `/api/v1`)
- **useAvailabilityWindows(facultyId)**
  - list — `GET /faculty/{facultyId}/availability` → `{data:[...]}` (plain list; `enabled: facultyId != null`).
  - `useCreateWindow(facultyId)` — `POST /faculty/{facultyId}/availability`; invalidate list.
  - `useUpdateWindow(facultyId)` — `PUT /faculty/{facultyId}/availability/{windowId}`; invalidate list.
  - `useDeleteWindow(facultyId)` — `DELETE /faculty/{facultyId}/availability/{windowId}` (204); invalidate list.
- **useFacultyPreferences(facultyId)**
  - get — `GET /faculty/{facultyId}/preferences` → `{data:{...}}` (`enabled: facultyId != null`).
  - `useSetPreferences(facultyId)` — `PUT /faculty/{facultyId}/preferences` (upsert); invalidate get.

### 5.2 Schemas (`availability-schemas.js`)
- `availabilityWindowSchema`: dayOfWeek (enum DAYS_OF_WEEK), startTime (`HH:mm` regex), endTime (`HH:mm` regex), reasonCode (1..50), reasonNote (optional ≤500). Cross-field refine: endTime strictly after startTime (string compare works for zero-padded `HH:mm`).
- `preferenceSchema`: preferredTimeOfDay (enum TIME_OF_DAY), sessionDistribution (enum SESSION_DISTRIBUTION).

### 5.3 AvailabilityWindowModal (custom)
- Props: open, mode, initialValues, isPending, onSubmit, onClose.
- Fields: day `<select>` (DAYS_OF_WEEK), start `<input type="time">`, end `<input type="time">`, reason-code text, note textarea.
- Submit: `validateWith(availabilityWindowSchema, values)` → on failure set field errors, no request (AC-3); on success `onSubmit(data, {onError})` mapping backend field/message errors.

### 5.4 PreferencesForm (inline)
- Loads current preference (GET default object when unset). Two selects (time-of-day, distribution) + Save. On save `PUT`; success/status message inline; `mapApiError` on failure.

### 5.5 FacultyAvailabilityPage
- Faculty selector (`useFacultyList`) → sets `facultyId`.
- When a faculty is selected: a **Windows (hard / unavailability)** section — `ConfigTable` (columns: day, start, end, reason code, note) with add/edit/delete via `AvailabilityWindowModal` + `ConfirmDeleteDialog` (delete `onError` → dialog label); and a **Preferences (soft)** section — `PreferencesForm`. Distinct visual treatment per section (FR-5). Loading skeleton / empty state / retry via `ConfigTable`.

## 6. Error Handling

- Window form: client Zod first (no request on invalid); backend 400 → field errors, 422 (end-after-start, bad day) → message via `mapApiError`.
- Delete: `onError` surfaces reason into the confirm dialog; dialog stays open.
- Preferences save: 422 (bad allowlist value) → inline message.

## 7. Accessibility & Security

- Labels on all controls; `aria-label` on icon-only buttons; native `<dialog>` focus trap + Escape; inline errors `role="alert"`.
- No `dangerouslySetInnerHTML`; escaped JSX. Plain JSX, PropTypes, enforced import order.

## 8. Testing Strategy

- Unit tests for `availability-schemas.js`: window schema (each field, `HH:mm` format, end-after-start refine) and preference schema (each enum). Verify via full vitest suite + vite build (new lazy chunk).

## 9. Traceability

| Requirement | Design element |
|-------------|----------------|
| FR-1 (faculty context) | 5.5 faculty selector via useFacultyList |
| FR-2 (windows CRUD) | 5.1 useAvailabilityWindows, 5.3 AvailabilityWindowModal, ConfigTable |
| FR-3 (window validation) | 5.2 availabilityWindowSchema |
| FR-4 (preferences) | 5.1 useFacultyPreferences, 5.4 PreferencesForm, 5.2 preferenceSchema |
| FR-5 (hard/soft sections) | 5.5 two distinct sections |
| NFR-1..4 | §6, §7 |

## 10. Out of Scope / Carried Items

- All A4-425 OQs are resolved/deferred (§2); none carried as unresolved.
- Backend/API change, faculty profile CRUD, RBAC, scheduling — out of scope.
