# A4-445 — Frontend Time-Slot Grid Configuration (CRUD): Requirement Document

- **Story:** A4-445 — Frontend — Time-Slot Grid Configuration (CRUD)
- **Requirement Generation subtask:** A4-446
- **Consumes backend:** A4-10 (Time-Slot Grid Configuration) — `/api/v1/time-slots` (grids + slot definitions with day-aware overlap validation, already implemented)
- **BRD:** 6.2 (configurable time-slot grids per campus), 7.4 (period duration, periods/day, break/lunch windows, per campus), 7.7 (mixed slot durations — 1-hour, 1.5-hour, 3-hour simultaneously)
- **Date:** 2026-09-09

## 1. Introduction

This document specifies the **UI-only** requirements for a System Administrator to configure a per-campus time-slot grid: create/rename/delete the campus grid, and add/remove slot definitions (teaching, break, lunch) with start/end times, an optional day override, and an optional label. It is grounded in the **verified A4-10 REST contract**. Several natural expectations from the story wording — a slot *edit* endpoint, a per-slot *duration restriction*, and a slot-reference guard — differ from what the backend actually exposes today; these are raised as Open Questions (Section 16) rather than invented into requirements.

## 2. User Story

*As a* System Administrator, *I want* a web UI to configure per-campus time-slot grids with mixed slot durations (60/90/180 min), break/lunch windows, and period definitions, *so that* each campus has its own daily schedule structure for lectures, tutorials, and practicals.

*Story Points:* 5

## 3. Actors

- **System Administrator** — creates the campus grid, renames it, adds/removes slots, deletes the grid. (Backend RBAC is not enforced yet — the A4-10 endpoints are `permitAll` with `// TODO @PreAuthorize("hasRole('ADMIN')")`; this UI does not gate by role — see OQ-5.)

## 4. User Journeys

1. **Pick a campus.** The screen starts by selecting a campus (reuse the campus list from A4-410 / A4-2). The UI then loads that campus's grid via `GET /time-slots/campus/{campusId}`.
2. **Campus has no grid yet (empty state).** If the campus has no active grid (backend returns 404 for that campus), the UI shows an empty state with a "Create grid" action. Creation requires a grid name and **at least one teaching slot** (backend rejects otherwise).
3. **Create the grid.** Admin enters a grid name and defines an initial set of slots (mixed durations 60/90/180, break/lunch entries). On success (201) the grid and its slots render on a day timeline.
4. **Add a slot.** With a grid loaded, admin adds a slot (start, end, type, optional day override, optional label) via `POST /time-slots/{gridId}/slots`. Backend re-checks overlap (day-aware) and returns 409 on conflict, surfaced inline.
5. **Remove a slot.** Admin removes a slot via `DELETE /time-slots/{gridId}/slots/{slotId}` behind a confirmation dialog. Backend blocks removing the **last** teaching slot (422), surfaced inline.
6. **Rename the grid.** Admin edits the grid name via `PUT /time-slots/{id}` (name only — slots are not edited by this endpoint).
7. **Delete the grid.** Admin deletes the whole grid via `DELETE /time-slots/{id}` behind a confirmation dialog; this soft-deletes the grid and all its slots. The screen returns to the empty state for that campus.
8. **Switch campus.** Selecting a different campus loads that campus's own grid independently (each campus has at most one active grid).
9. **View a specific day.** Admin can view the effective slots for one weekday via `GET /time-slots/{gridId}/effective?day=MONDAY`, which merges day-specific overrides over all-days slots (day-specific wins for overlapping ranges).

## 5. Functional Requirements

### FR-1 — Campus selection context
- **FR-1.1** The screen requires a selected campus. The campus is chosen from the campus list (reuse A4-410 / A4-2 `GET /campuses`).
- **FR-1.2** On selection, the UI loads that campus's grid via `GET /time-slots/campus/{campusId}`. A 404 for the campus is treated as "no grid yet" (empty state, FR-2.2), **not** as an error toast.

### FR-2 — Grid lifecycle (create / rename / delete)
- **FR-2.1** When the campus has a grid, display it: grid name, and its slots (see FR-3). Provide "Rename grid" and "Delete grid" actions.
- **FR-2.2** When the campus has no grid, show an empty state with a "Create grid" CTA.
- **FR-2.3** **Create** a grid via `POST /time-slots` with: `campusId` (from the selected campus), `gridName` (required, ≤200), and `slots` (**at least one**, at least one of type `TEACHING`). Each slot carries the fields in FR-3.3.
- **FR-2.4** **Rename** the grid via `PUT /time-slots/{id}` sending `gridName` only (≤200). This endpoint does **not** modify slots (see OQ-1).
- **FR-2.5** **Delete** the grid via `DELETE /time-slots/{id}` (204) behind a confirmation dialog that states all slots will be removed. On success, return to the empty state.
- **FR-2.6** If the campus already has an active grid, creation is not offered (a second create returns 409 `ConflictException`); if a 409 does occur, surface it inline.

### FR-3 — Slot definitions (add / list / remove)
- **FR-3.1** Render the grid's slots as a **day timeline** ordered by start time. Show start, end, derived `durationMinutes` (returned by the backend), type, day scope (a day name or "All days"), and label if present.
- **FR-3.2** Break and lunch slots are **visually distinguished** from teaching slots (AC-3), e.g., distinct color/label treatment.
- **FR-3.3** **Add** a slot via `POST /time-slots/{gridId}/slots` with: `startTime` (required, HH:mm), `endTime` (required, HH:mm), `slotType` (required: `TEACHING` / `BREAK` / `LUNCH`), `applicableDay` (optional — one of MONDAY..SUNDAY, or empty = all days), `label` (optional, ≤100).
- **FR-3.4** **Remove** a slot via `DELETE /time-slots/{gridId}/slots/{slotId}` (204) behind a confirmation dialog.
- **FR-3.5** There is **no slot-edit endpoint** in A4-10. "Editing" a slot is therefore modelled as remove-then-add in the UI, or deferred — see OQ-1. No FR promises an in-place slot `PUT`.
- **FR-3.6** Provide a "view by day" control that calls `GET /time-slots/{gridId}/effective?day={DAY}` to preview the effective schedule for one weekday (day-specific overrides merged over all-days slots).

### FR-4 — Client-side validation
- **FR-4.1** Zod validation before any request: `startTime` and `endTime` required; `endTime` **strictly after** `startTime` (AC-5); `slotType` ∈ {TEACHING, BREAK, LUNCH}; `applicableDay` empty or ∈ {MONDAY..SUNDAY}; `label` ≤100; grid `gridName` required and ≤200. Invalid submit shows inline errors and sends no request.
- **FR-4.2** On create, the client enforces "at least one slot, at least one teaching slot" before submitting (mirrors backend HC-GRID-4), with an inline message if unmet.

### FR-5 — Server error surfacing
- **FR-5.1** **Overlap (409):** when the backend rejects an overlapping slot (day-aware), surface the returned overlap message inline next to the slot form; the slot is not added (AC-2).
- **FR-5.2** **Last teaching slot (422):** when removing a teaching slot would leave none, surface the backend message; the slot is not removed.
- **FR-5.3** **Start-before-end (422):** if the backend rejects start≥end (defense in depth beyond FR-4.1), surface it inline.
- **FR-5.4** All server errors are mapped via the shared `mapApiError` helper; no stack traces or raw payloads are shown.

### FR-6 — Feedback and states
- **FR-6.1** Loading skeletons on grid/slot load; empty state (FR-2.2); list-error retry.
- **FR-6.2** Success feedback on create/rename/delete/add/remove consistent with the reused feedback components (inline message regions; a toast if the shared toast pattern exists — see OQ-6).
- **FR-6.3** Destructive actions (delete grid, remove slot) always require confirmation.

## 6. Constraints Owned by This Document

- Client-side Zod validation for the grid form (name bounds) and slot form (times, end-after-start, type allowlist, day allowlist, label bounds), and the create-time "≥1 teaching slot" pre-check. Backend re-validates authoritatively.
- Client-side presentation of the `slotType` and `applicableDay` option lists (mirroring the backend enums — OQ-2/OQ-3).
- Timeline rendering, day-scope labelling ("All days" vs a weekday), and the break/lunch visual distinction.

## 7. Constraints Referenced from Other Documents

- **A4-10 (backend):** one active grid per campus (409 on duplicate); grid must have ≥1 teaching slot (422, HC-GRID-4); slot `start < end` (422, HC-GRID-3); day-aware overlap rule (409, HC-GRID-1) — `null` day = all-days, non-null = day-specific override, override does not "overlap" an all-days slot, different specific days never overlap; `durationMinutes` derived server-side; `PUT` updates grid name only; slot add/remove only (no slot edit); delete is soft-delete of grid + slots; effective-slots-for-day merges overrides over all-days slots.
- **A4-2 / A4-410 (campus):** used to select the campus whose grid is managed.

## 8. Validation Rules (client-side)

| Field | Rule |
|-------|------|
| gridName | required, ≤200 chars |
| slot startTime | required (HH:mm) |
| slot endTime | required (HH:mm), strictly after startTime |
| slotType | required, one of TEACHING / BREAK / LUNCH |
| applicableDay | optional; empty = all days, else one of MONDAY..SUNDAY |
| label | optional, ≤100 chars |
| create slot set | ≥1 slot; ≥1 of type TEACHING |

## 9. Non-Functional Requirements

- **NFR-1 (security/XSS):** all dynamic text rendered as escaped JSX; no `dangerouslySetInnerHTML` (org XSS standard).
- **NFR-2 (a11y):** labels on all controls; `aria-label` on icon-only buttons; keyboard-accessible dialogs (native `<dialog>`); inline errors announced; the day timeline is navigable/readable by keyboard and screen reader.
- **NFR-3 (performance):** route-based code splitting (lazy route). Grid data is small (one grid per campus); no pagination required.
- **NFR-4 (standards):** plain JSX (no TypeScript — hard rule), PropTypes, enforced import order; reuse the shared table/modal/confirm/feedback components and `validateWith` / `mapApiError` / api-client, per the A4-410/415/420/425 precedent.

## 10. Acceptance Criteria

- **AC-1** *Given* the grid editor for a selected campus with no grid, *When* the admin creates a grid with mixed-duration slots (60/90/180 min) that do not overlap, *Then* the grid is saved (201) and the slots appear on the day timeline.
- **AC-2** *Given* a loaded grid, *When* the admin adds a slot that overlaps an existing slot (same day scope), *Then* the backend overlap error (409) is shown inline and the slot is not added.
- **AC-3** *Given* break and/or lunch slots on the grid, *When* the timeline renders, *Then* they are visually distinguished from teaching slots.
- **AC-4** *Given* two campuses with different grids, *When* the admin switches the campus selector, *Then* each campus's own grid and slots are shown independently.
- **AC-5** *Given* a slot whose end time is not after its start time, *When* submitted, *Then* an inline validation error is shown and no request is sent.
- **AC-6** *Given* a grid with exactly one teaching slot, *When* the admin tries to remove that teaching slot, *Then* the backend 422 (last teaching slot) is surfaced and the slot is not removed.
- **AC-7** *Given* a loaded grid, *When* the admin deletes the grid after confirming, *Then* the grid is deleted (204) and the campus returns to the empty state.

## 11. Data Model (conceptual, frontend view)

- **TimeSlotGrid** (`TimeSlotGridDto`): `id`, `campusId`, `gridName`, `slots` (list of SlotDefinition), `createdAt`, `updatedAt`.
- **SlotDefinition** (`SlotDefinitionDto`): `id`, `gridId`, `startTime`, `endTime`, `durationMinutes` (derived, read-only), `slotType` (TEACHING/BREAK/LUNCH), `applicableDay` (weekday or null = all days), `label`.

## 12. Dependencies

- A4-10 backend running at `/api/v1/time-slots`.
- A4-2 / A4-410 campus list (to select the campus context).
- Reused frontend components (table/modal/confirm/feedback) + `validateWith` / `mapApiError` / api-client, per the A4-410/415/420/425 precedent.

## 13. Assumptions

1. The screen operates on **one campus at a time**, and each campus has **at most one** active grid (backend enforces one-grid-per-campus).
2. A 404 from `GET /time-slots/campus/{campusId}` means "no grid yet" (empty state), not a failure.
3. The `slotType` and `applicableDay` option lists are mirrored in the frontend as constants (no backend lookup endpoints — OQ-2/OQ-3).
4. `durationMinutes` is computed and returned by the backend; the UI displays it read-only and does not send it.
5. "Mixed durations 60/90/180" (BRD 7.7) are the durations the grid must *accommodate*; the backend does not restrict slot duration to those three values, so the UI does not hard-block other durations (OQ-4).

## 14. Consistency Notes

- The story lists "add/**edit**/delete slots", but A4-10 exposes only **add** and **remove** for slots (no slot `PUT`). Reconciled: FR-3.5 models editing as remove-then-add or defers it; no FR promises an in-place slot edit (OQ-1).
- The story's `PUT` maps to grid-**name** update only; slot changes go through the add/remove endpoints. FR-2.4 reflects this precisely.
- The overlap rule is **day-aware**: an all-days slot and a same-time day-specific slot do **not** conflict (the day-specific one is an override). The UI surfaces whatever the backend returns and does not attempt its own overlap verdict beyond the end-after-start check (client cannot authoritatively replicate the day-aware merge).
- The story mentions "toasts"; feedback is surfaced via the reused feedback components; a toast is used only if the shared toast pattern already exists (OQ-6). No new toast system is built here.

## 15. Out of Scope

- Backend / API / schema changes (owned by A4-10).
- Academic calendar frontend (A4-9 / A4-440) — which *days* are working; this story is about *periods within a day*.
- RBAC / role gating; scheduling-engine placement; drag-drop editor (A4-15).
- Session-reference impact when removing a slot/grid (backend has this as a TODO pending the scheduling module — OQ-7).

## 16. Open Questions

- **OQ-1:** The story says "add/edit/delete slots," but the backend has **no slot-edit endpoint** (only add/remove). Confirm the reconciliation: model "edit" as remove-then-add in the UI, or defer slot editing until a backend `PUT /time-slots/{gridId}/slots/{slotId}` exists. Proposed: remove-then-add. Confirm.
- **OQ-2:** No backend endpoint lists valid `slotType` values; the frontend mirrors {TEACHING, BREAK, LUNCH}. Acceptable?
- **OQ-3:** No backend endpoint lists valid `applicableDay` values; the frontend mirrors {MONDAY..SUNDAY} plus an "All days" (null) option. Acceptable?
- **OQ-4:** BRD 7.7 names 60/90/180 min. The backend does **not** restrict slot duration to an enum. Should the UI (a) allow any positive duration (proposed), or (b) restrict/warn to 60/90/180? Confirm.
- **OQ-5:** A4-10 endpoints are `permitAll` (RBAC not yet built). This UI does not gate by role. Confirm no role gating is expected in this story.
- **OQ-6:** Prior frontend stories surfaced feedback via inline regions (no toast system was built). Confirm the story's "toasts" is satisfied by the existing feedback pattern, or specify a toast component to introduce.
- **OQ-7:** Removing a slot or deleting a grid does **not** yet check session references (backend TODO KD-43, pending scheduling module). Confirm the UI simply surfaces whatever the backend allows today (no extra client guard).
- **OQ-8:** The story mentions an "optional day override" and a "visual day timeline." Confirm the day-view UX: a single all-days timeline with day-override badges, plus a per-day preview using `GET /{gridId}/effective?day=` (proposed), versus a full 7-day grid view.

## 17. Traceability

| BRD / Story | Requirement |
|-------------|-------------|
| BRD 6.2 (configurable grids per campus) | FR-1, FR-2 (per-campus create/rename/delete) |
| BRD 7.4 (period duration, periods/day, break/lunch, per campus) | FR-3 (slots with duration + break/lunch), FR-2.3 |
| BRD 7.7 (mixed durations 60/90/180 simultaneously) | FR-3.1/FR-3.3 (mixed durations on one grid), AC-1; OQ-4 (no hard restriction) |
| Story AC-1 (mixed durations, no overlap) | FR-2.3, FR-3.1/3.3, AC-1 |
| Story AC-2 (overlap error surfaced) | FR-5.1, AC-2 |
| Story AC-3 (break/lunch distinguished) | FR-3.2, AC-3 |
| Story AC-4 (per-campus independence) | FR-1.2, FR-2.1, AC-4 |
| Story AC-5 (end-before-start rejected) | FR-4.1, AC-5 |
| Story "add/edit/delete slots" | FR-3.3/3.4, FR-3.5 + OQ-1 (edit reconciled) |
| Story "Zod validation, delete confirmation, skeletons, empty state, toasts" | FR-4, FR-6, OQ-6 (toasts) |
