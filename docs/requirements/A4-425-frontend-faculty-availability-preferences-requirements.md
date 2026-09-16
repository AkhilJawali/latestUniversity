# A4-425 — Frontend Faculty Availability and Preferences (CRUD): Requirement Document

- **Story:** A4-425 — Frontend Faculty Availability and Preferences (CRUD)
- **Requirement Generation subtask:** A4-426
- **Consumes backend:** A4-5 (Faculty Availability and Preference Management) — `/api/v1/faculty/{facultyId}/availability` and `/preferences`
- **BRD:** 6.5 (capture faculty time-preferences/unavailability as soft or hard constraints), 7.2 (availability/blocked slots; preference weighting — preferred time-of-day, consecutive vs spread)
- **Date:** 2026-09-04

## 1. Introduction

This document specifies the UI-only requirements for a faculty member (or admin acting for them) to manage availability windows and scheduling preferences. It is grounded in the verified A4-5 REST contract. Importantly, the backend models the "hard vs soft" concept differently from the story's wording, and several story assumptions (per-window hard/soft toggle, preference weighting, an Evening option) are not supported by the backend — these are raised as Open Questions (Section 16) rather than invented.

## 2. User Story

*As a* Faculty member, *I want* a web UI to declare my availability windows (unavailability) and my soft time preferences, *so that* the scheduling engine respects my constraints and preferences.

## 3. Actors

- **Faculty / Admin / HOD** — manages a faculty member's availability windows and preferences. (Backend RBAC is not enforced yet — endpoints are `permitAll` with `// TODO` role annotations; this UI does not gate by role.)

## 4. User Journeys

1. **Pick a faculty.** Because every A4-5 endpoint is nested under `{facultyId}`, the screen first needs a faculty selected (reuse the faculty list from A4-420). Then its availability windows and preferences load.
2. **Add an availability (unavailability) window.** User adds a window: day of week, start time, end time, reason code (required), optional note → saved and listed.
3. **Edit / delete a window.** User edits a window's day/time/reason, or deletes it (with confirmation).
4. **Set preferences.** User picks a preferred time-of-day and a session distribution (consecutive vs spread) → upserted (one per faculty).
5. **Empty state.** A faculty with no windows shows an empty state + create action.

## 5. Functional Requirements

### FR-1 — Faculty selection context
- **FR-1.1** The screen requires a selected faculty (all A4-5 endpoints are nested under `{facultyId}`). The faculty is chosen from the faculty list (reuse A4-420 / A4-4 `GET /faculty`).
- **FR-1.2** On selection, the UI loads that faculty's availability windows (`GET /faculty/{id}/availability`) and preferences (`GET /faculty/{id}/preferences`).

### FR-2 — Availability windows (hard/unavailability)
- **FR-2.1** List the faculty's active windows from `GET /faculty/{id}/availability` (a plain list — not paginated): day of week, start time, end time, reason code, note.
- **FR-2.2** These windows are presented as **hard unavailability/availability** (the backend has no per-window soft/hard flag — see OQ-1/OQ-2). They receive a distinct "hard/blocked" visual treatment, separate from the soft preferences section (FR-4).
- **FR-2.3** Create a window (`POST`) with: day of week (MONDAY..SUNDAY), start time, end time, reason code (required, ≤50), optional note (≤500).
- **FR-2.4** Edit a window (`PUT /availability/{windowId}`) with the same fields.
- **FR-2.5** Delete a window (`DELETE /availability/{windowId}`, 204) behind a confirmation dialog.
- **FR-2.6** Loading skeleton, empty state + "Add window" CTA, and list-error retry.

### FR-3 — Availability validation
- **FR-3.1** Client-side Zod validation: day of week required (∈ 7 values), start and end required, end **strictly after** start, reason code required (≤50), note ≤500. Invalid submit shows inline errors and sends no request (AC-3).
- **FR-3.2** Backend also enforces end-after-start and the day allowlist (422); those messages are surfaced if they occur.

### FR-4 — Preferences (soft)
- **FR-4.1** Load preferences via `GET /faculty/{id}/preferences` (backend returns a default `NO_PREFERENCE`/`NO_PREFERENCE` object when none set — never 404).
- **FR-4.2** A preferences form with: **preferred time-of-day** (MORNING, AFTERNOON, NO_PREFERENCE) and **session distribution** (CONSECUTIVE, SPREAD, NO_PREFERENCE). These are presented with a distinct "soft preference" visual treatment.
- **FR-4.3** Save via `PUT /faculty/{id}/preferences` (upsert; one per faculty); on success the values persist and display; backend allowlist violations (422) are surfaced.

### FR-5 — Hard vs soft visual distinction
- **FR-5.1** Availability windows (FR-2) and preferences (FR-4) are shown as clearly distinct sections — windows as hard constraints, preferences as soft — satisfying the story's hard/soft-distinction requirement at the section level (since the backend has no per-window flag; see OQ-1).

## 6. Constraints Owned by This Document

- Client-side validation for availability windows (day allowlist, end-after-start, reason bounds) and for the preference allowlists. Backend re-validates authoritatively.
- Client-side presentation of the day-of-week, time-of-day, and distribution option lists (mirroring the backend allowlists — OQ-3/OQ-4).

## 7. Constraints Referenced from Other Documents

- **A4-5 (backend):** window fields — dayOfWeek ∈ {MONDAY..SUNDAY}, startTime/endTime (LocalTime, HH:mm[:ss]), start strictly before end (422), reasonCode required ≤50, reasonNote ≤500; preferences — preferredTimeOfDay ∈ {MORNING, AFTERNOON, NO_PREFERENCE}, sessionDistribution ∈ {CONSECUTIVE, SPREAD, NO_PREFERENCE}, upsert one-per-faculty, no delete.
- **A4-4 / A4-420 (faculty):** used to pick the faculty whose availability/preferences are managed.

## 8. Validation Rules (client-side)

| Field | Rule |
|-------|------|
| dayOfWeek | required, one of the 7 day names |
| startTime | required (HH:mm) |
| endTime | required (HH:mm), strictly after startTime |
| reasonCode | required, ≤50 chars |
| reasonNote | optional, ≤500 chars |
| preferredTimeOfDay | one of MORNING / AFTERNOON / NO_PREFERENCE |
| sessionDistribution | one of CONSECUTIVE / SPREAD / NO_PREFERENCE |

## 9. Non-Functional Requirements

- **NFR-1 (security/XSS):** dynamic text as escaped JSX; no `dangerouslySetInnerHTML`.
- **NFR-2 (a11y):** labels on all controls, `aria-label` on icon-only buttons, keyboard-accessible dialogs (native `<dialog>`), inline errors announced.
- **NFR-3 (performance):** route-based code splitting (lazy route). List is small and unpaginated (backend returns the full active set).
- **NFR-4 (standards):** plain JSX (no TypeScript), PropTypes, enforced import order; reuse shared table/modal/confirm components + `validateWith` / `mapApiError`.

## 10. Acceptance Criteria

- **AC-1** *Given* the availability screen for a selected faculty, *When* a hard unavailability window (e.g., Tue 2–4 PM) is added, *Then* it is saved (201) and shown in the windows (hard) section.
- **AC-2** *Given* the preferences form, *When* a soft preference (prefers mornings, spread) is saved, *Then* it persists (`PUT`) and shows in the preferences (soft) section.
- **AC-3** *Given* a window whose end time is before (or equal to) start time, *When* submitted, *Then* an inline validation error shows and no request is sent.
- **AC-4** *Given* an existing window, *When* deleted with confirmation, *Then* it is removed from the list.
- **AC-5** *Given* the screen for a faculty with no windows, *When* rendered, *Then* an empty state with a create action is shown.

## 11. Data Model (conceptual, frontend view)

- **AvailabilityWindow** (FacultyAvailabilityWindowDto): id, facultyId, dayOfWeek, startTime, endTime, reasonCode, reasonNote, isActive, createdAt, updatedAt.
- **Preference** (FacultyPreferenceDto): id, facultyId, preferredTimeOfDay, sessionDistribution, isActive, createdAt, updatedAt (one per faculty; default object when unset).

## 12. Dependencies

- A4-5 backend running (`/api/v1/faculty/{id}/availability` + `/preferences`).
- A4-4 / A4-420 faculty list (to select the faculty context).
- Reused frontend components (table/modal/confirm) + `validateWith` / `mapApiError`, per the A4-410/415/420 precedent.

## 13. Assumptions

1. The screen operates on one selected faculty at a time (endpoints are per-faculty).
2. "Hard vs soft distinction" is realised at the section level: availability windows = hard, preferences = soft — because the backend has no per-window hard/soft flag (OQ-1).
3. The day / time-of-day / distribution option lists are mirrored in the frontend as constants (no backend lookup endpoints — OQ-3/OQ-4).

## 14. Consistency Notes

- The story says windows are flagged "hard vs soft" per window; the backend has **no** per-window flag (hard/blocked meaning is derived from designation server-side and not exposed). Reconciled: windows are presented as hard, preferences as soft (FR-5, OQ-1/OQ-2). No FR promises a per-window toggle.
- The story mentions "preference weighting"; the backend has **no** weight field. Not promised by any FR; raised as OQ-5.
- The story's "preferred time-of-day" is limited to MORNING/AFTERNOON/NO_PREFERENCE (no Evening — OQ-4); FR-4.2 lists exactly the supported values.
- The story mentions "toasts"; feedback is surfaced via inline message regions consistent with the reused components (no toast system is built here).

## 15. Out of Scope

- Backend/API/schema change (owned by A4-5).
- Faculty profile CRUD (A4-420 / A4-4 frontend).
- RBAC / role gating; scheduling.

## 16. Open Questions

- **OQ-1:** The story wants a per-window "hard vs soft" flag, but the backend availability window has **no** such field and exposes no way to set it. Confirm the reconciliation (windows = hard section, preferences = soft section). Or raise a backend follow-up to add a per-window type.
- **OQ-2:** The backend derives a BLOCKED-vs-AVAILABLE *mode* from the faculty's designation but does **not** expose it on any A4-5 endpoint, so the UI cannot definitively label whether a faculty's windows mean "unavailable" or "available." Proposed: label windows as "unavailability" by default (the common case). Confirm, or request a backend endpoint that returns the mode.
- **OQ-3:** No backend endpoint lists valid day-of-week values; the frontend mirrors the 7 uppercase names. Acceptable?
- **OQ-4:** Preferred time-of-day supports only MORNING/AFTERNOON/NO_PREFERENCE (no EVENING). Confirm the UI offers exactly these, or raise a backend follow-up to add EVENING.
- **OQ-5:** The story mentions "preference weighting"; no backend weight/priority field exists. Recommend deferring. Confirm.
- **OQ-6:** `reasonCode` is a free string (≤50) on the backend — no enforced code list. Should the UI offer a free-text field, or a curated dropdown (frontend-only convenience)? Proposed: free text with a short helper. Confirm.

## 17. Traceability

| BRD / Story | Requirement |
|-------------|-------------|
| BRD 6.5 (hard/soft constraints) | FR-2 (hard windows), FR-4 (soft preferences), FR-5 |
| BRD 7.2 (availability / blocked slots) | FR-2 |
| BRD 7.2 (preferred time-of-day, consecutive vs spread) | FR-4.2 |
| BRD 7.2 (preference weighting) | OQ-5 (no backend support — deferred pending decision) |
| Story AC-1 | FR-2.3, AC-1 |
| Story AC-2 | FR-4.3, AC-2 |
| Story AC-3 | FR-3.1, AC-3 |
| Story AC-4 | FR-2.5, AC-4 |
| Story AC-5 | FR-2.6, AC-5 |
| Story "hard vs soft per window" | OQ-1 / OQ-2 (reconciled at section level) |
