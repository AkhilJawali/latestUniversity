# A4-440 — Frontend: Academic Calendar Management (CRUD) — Requirement Document

| Field | Value |
|-------|-------|
| Story | A4-440 — Frontend — Academic Calendar Management (CRUD) |
| Requirement Subtask | A4-441 |
| Epic | A4 — UTMS |
| Role / Type | Frontend (React + plain JSX) |
| Consumes backend | A4-9 — Academic Calendar Management (REST APIs, already implemented) |
| BRD Requirements | 6.1, 7.4, 6.12 |
| Story Points | 5 |

---

## 1. Introduction

This document specifies the requirements for the **web UI** that lets a Registrar manage academic calendars per campus — semester dates, holidays, exam windows, orientation periods, and the campus working-day pattern. It is a **frontend-only** story. All persistence, validation authority, and business rules live in the already-implemented backend (A4-9); this UI consumes those REST endpoints and provides fast client-side feedback. No API, schema, or backend changes are in scope.

---

## 2. User Story

*As a* Registrar,
*I want* a web UI to manage the academic calendar — semester start/end dates, holidays, exam windows, orientation periods, and working-day patterns, with per-campus variation,
*so that* scheduling correctly excludes non-working days and respects term boundaries.

---

## 3. Actors

- **Registrar** — the primary actor; creates and manages calendars and patterns. (RBAC enforcement is out of scope — see §15; the UI assumes an authenticated user.)

---

## 4. User Journeys

**J1 — View calendars for a campus**
1. Registrar opens the Academic Calendar page.
2. Selects a campus from the campus selector.
3. System loads and lists all calendars for that campus (each showing academic year, semester identifier, start/end dates).
4. Empty state shown if the campus has no calendars.

**J2 — Create a calendar**
1. Registrar clicks "New Calendar".
2. Fills academic year, semester identifier, start date, end date (holidays/exam windows/orientation can be added now or later).
3. Client validates (required fields, end ≥ start). On invalid input, inline errors appear and submit is blocked.
4. On save, the calendar is created for the selected campus; list refreshes; success toast.
5. On server error (e.g., duplicate, backend validation), an error toast/inline message is shown and nothing is lost.

**J3 — View a calendar's detail**
1. Registrar selects a calendar from the list.
2. System shows its semester dates plus three sub-lists: holidays, exam windows, orientation periods.

**J4 — Add a holiday (campus-specific or institution-wide)**
1. From a calendar's detail, Registrar opens "Add Holiday".
2. Enters start date, end date, description, and scope (Campus-specific / Institution-wide).
3. Client validates; on save, the holiday appears in the calendar's holiday list; success toast.

**J5 — Remove a holiday**
1. Registrar clicks delete on a holiday row.
2. Confirmation dialog appears.
3. On confirm, the holiday is removed; list refreshes; success toast.

**J6 — Add an exam window**
1. Registrar opens "Add Exam Window" from a calendar.
2. Enters start date, end date, exam type (Mid-semester / End-semester / Supplementary), optional description.
3. Client validates; on save, it appears in the exam-window list; success toast.

**J7 — Add an orientation period**
1. Registrar opens "Add Orientation Period".
2. Enters start date, end date, optional description.
3. Client validates; on save, it appears in the orientation list; success toast.

**J8 — Configure / update the campus working-day pattern**
1. Registrar opens the working-day pattern editor for the selected campus.
2. System loads the existing pattern if one exists (create-or-edit).
3. Registrar picks a pattern type (5-day / 6-day / Alternate Saturdays / Custom); for Alternate Saturdays, specifies which Saturdays (e.g., 1st and 3rd); for Custom, provides the custom definition.
4. Client validates; on save, the pattern is stored and displayed.

**J9 — Delete a calendar**
1. Registrar clicks delete on a calendar.
2. Confirmation dialog warns this removes the calendar and all its entries.
3. On confirm, the calendar is soft-deleted server-side and removed from the list; success toast.

---

## 5. Functional Requirements

Organized by user behavior. Every FR maps to a verified A4-9 endpoint (see §12 for the exact contract).

### FR-1: Campus context and calendar listing
- **FR-1.1** The page shall provide a campus selector. Calendar data is always scoped to the selected campus.
- **FR-1.2** On campus selection, the UI shall fetch and display all calendars for that campus via `GET /academic-calendars/campus/{campusId}`, showing academic year, semester identifier, and start/end dates per calendar.
- **FR-1.3** While loading, the UI shall show skeleton placeholders (not a spinner). When the campus has no calendars, an empty state with a "New Calendar" CTA is shown.

### FR-2: Create a calendar
- **FR-2.1** The UI shall provide a form to create a calendar with: academic year, semester identifier, semester start date, semester end date. It may optionally include initial holidays, exam windows, and orientation periods (the create endpoint accepts nested lists).
- **FR-2.2** The UI shall validate client-side before submit (§8) and block submission on invalid input, showing inline field errors.
- **FR-2.3** On submit, the UI shall call `POST /academic-calendars` with the selected `campusId`. On 201, refresh the list and show a success toast.
- **FR-2.4** On error (400/409/422/500), the UI shall surface a user-friendly message (mapping backend field errors to form fields where present) and preserve entered data.

### FR-3: View calendar detail
- **FR-3.1** On selecting a calendar, the UI shall fetch its full detail via `GET /academic-calendars/{id}` and render semester dates plus three sub-lists: holidays, exam windows, orientation periods.

### FR-4: Holiday management
- **FR-4.1** The UI shall add a holiday to a calendar via `POST /academic-calendars/{calendarId}/holidays` with start date, end date, description (required), and scope.
- **FR-4.2** Scope shall be selectable as **Campus-specific** or **Institution-wide**. Campus-specific holidays are displayed under that campus's calendar; institution-wide holidays are labelled as applying to all campuses (AC-2, AC-3). *(The UI reflects the scope value returned by the API; it does not itself compute cross-campus propagation — see §14.)*
- **FR-4.3** The UI shall remove a holiday via `DELETE /academic-calendars/{calendarId}/holidays/{holidayId}`, guarded by a confirmation dialog.

### FR-5: Exam window management
- **FR-5.1** The UI shall add an exam window via `POST /academic-calendars/{calendarId}/exam-windows` with start date, end date, exam type, and optional description.
- **FR-5.2** Exam type shall be one of: Mid-semester, End-semester, Supplementary (from the backend `ExamType` enum).

### FR-6: Orientation period management
- **FR-6.1** The UI shall add an orientation period via `POST /academic-calendars/{calendarId}/orientation-periods` with start date, end date, and optional description.

### FR-7: Working-day pattern editor
- **FR-7.1** The UI shall load the existing pattern for the selected campus via `GET /academic-calendars/patterns/campus/{campusId}`.
- **FR-7.2** If no pattern exists, the UI shall create one via `POST /academic-calendars/patterns` (with `campusId`); if one exists, it shall update it via `PUT /academic-calendars/patterns/campus/{campusId}`. This is a create-or-edit ("upsert-style") behavior at the UI level — one pattern per campus.
- **FR-7.3** Pattern type shall be one of: 5-day, 6-day, Alternate Saturdays, Custom. When **Alternate Saturdays** is chosen, the UI shall require a working-Saturdays value (comma-separated ordinals, e.g., `1,3`). When **Custom** is chosen, the UI shall require a custom definition.
- **FR-7.4** On save, the UI shall show a success toast and reflect the saved pattern.

### FR-8: Delete a calendar
- **FR-8.1** The UI shall delete a calendar via `DELETE /academic-calendars/{id}`, guarded by a confirmation dialog that states all entries will be removed. On 204, refresh the list and show a success toast.

### FR-9: Cross-cutting UI behaviors
- **FR-9.1** All destructive actions (delete calendar, delete holiday) require a confirmation dialog (per UI standards).
- **FR-9.2** All list/detail loads use skeletons; all empty lists use empty states with a relevant CTA.
- **FR-9.3** All create/delete outcomes produce a toast (success green, error red; error toasts persist until dismissed).
- **FR-9.4** All action buttons show a pending/disabled state while a request is in flight.

---

## 6. Constraints Owned by This Document

This is a frontend consumer story; it owns **UI-level** constraints only. It does not own any data or business rule (those are owned by A4-9).

- **UC-1 (Client validation is advisory, not authoritative):** The UI validates for fast feedback, but the backend is the source of truth. The UI must handle backend rejection even when client validation passed.
- **UC-2 (Campus scoping):** No calendar or pattern operation is issued without a selected campus.
- **UC-3 (Single pattern per campus):** The UI treats the working-day pattern as exactly one per campus (create-or-edit), matching the backend's per-campus pattern contract.

---

## 7. Constraints Referenced from Other Documents (owned by A4-9 backend)

The UI relies on but does not define these; it surfaces whatever the API enforces:

- End date ≥ start date for semesters, holidays, exam windows, orientation periods.
- Overlap rules between windows (where the API enforces them).
- Uniqueness of a calendar per campus/year/semester (if enforced by A4-9).
- Holiday impact detection on session data (backend side effect of add-holiday and pattern update).
- Soft-delete semantics of calendar deletion.

*(Exact enforcement is owned by A4-9; the UI must display any resulting 4xx errors gracefully — see OQ-2.)*

---

## 8. Validation Rules (client-side, mirroring A4-9 request bounds)

| Field | Rule | Source (A4-9 request) |
|-------|------|-----------------------|
| Academic year | Required; ≤ 20 chars | `CreateAcademicCalendarRequest.academicYear` |
| Semester identifier | Required; ≤ 30 chars | `CreateAcademicCalendarRequest.semesterIdentifier` |
| Semester start/end date | Both required; end ≥ start | `semesterStartDate`, `semesterEndDate` |
| Holiday start/end date | Both required; end ≥ start | `CreateHolidayRequest` |
| Holiday description | Required; ≤ 200 chars | `CreateHolidayRequest.description` (`@NotBlank`) |
| Holiday scope | Required; enum {Campus-specific, Institution-wide} | `HolidayScope` |
| Exam window start/end date | Both required; end ≥ start | `CreateExamWindowRequest` |
| Exam type | Required; enum {Mid-sem, End-sem, Supplementary} | `ExamType` |
| Exam window description | Optional; ≤ 200 chars | `CreateExamWindowRequest.description` |
| Orientation start/end date | Both required; end ≥ start | `CreateOrientationPeriodRequest` |
| Orientation description | Optional; ≤ 200 chars | `CreateOrientationPeriodRequest.description` |
| Pattern type | Required; enum {5-day, 6-day, Alternate Sat, Custom} | `PatternType` |
| Working Saturdays | Required iff Alternate Saturdays; ≤ 100 chars; ordinals like `1,3` | `CreateWorkingDayPatternRequest.workingSaturdays` |
| Custom definition | Required iff Custom | `CreateWorkingDayPatternRequest.customDefinition` |

*Note:* the "end ≥ start" rule and the conditional-required rules for pattern sub-fields are **client-side conveniences inferred from field semantics**; the backend re-validates. See OQ-1.

---

## 9. Non-Functional Requirements

- **NFR-1 (No TypeScript):** All files `.jsx`/`.js`; PropTypes for props; Zod for runtime validation. Hard rule.
- **NFR-2 (Conventions):** Follow the existing master-data feature layout (`features/master-data/<feature>/{api,components,constants,pages,schemas}`), TanStack Query for server state, the shared `apiClient` (baseURL `/api/v1`), and Zustand only if client-only state is needed.
- **NFR-3 (Accessibility, WCAG AA):** Keyboard-accessible forms and dialogs; focus trapping in modals; labels above inputs; ARIA labels on icon-only buttons; visible focus indicators.
- **NFR-4 (Security):** No `dangerouslySetInnerHTML`; validate with Zod before submit; never render raw API errors/stack traces.
- **NFR-5 (Performance):** Route-based lazy loading of the page (matching the existing router pattern); skeletons over spinners; read endpoints respond < 500ms (backend NFR) so no special handling beyond standard query caching.
- **NFR-6 (Responsiveness):** Desktop-first admin layout (Registrar). Usable down to tablet; mobile is not a target for this admin view.

---

## 10. Acceptance Criteria (Given / When / Then)

1. **Create calendar (happy path):** *Given* the calendar form with a campus selected, *When* the Registrar sets semester start/end dates (and required identifiers) and saves, *Then* the calendar is created for that campus and appears in the list with a success toast.
2. **Campus-specific holiday:** *Given* a calendar, *When* a holiday is added with **Campus-specific** scope, *Then* it appears under that campus's calendar and is labelled campus-specific.
3. **Institution-wide holiday:** *Given* a calendar, *When* a holiday is added with **Institution-wide** scope, *Then* it is labelled as applying to all campuses.
4. **Working-day pattern:** *Given* the pattern editor for a campus, *When* the Registrar configures Alternate Saturdays (e.g., 1st & 3rd) and saves, *Then* the pattern is saved and displayed; re-opening shows the saved values.
5. **Invalid date range (error path):** *Given* the calendar form, *When* an end date before the start date is submitted, *Then* an inline validation error is shown and the request is not sent.
6. **Delete confirmation (edge/destructive):** *Given* an existing calendar, *When* the Registrar clicks delete, *Then* a confirmation dialog appears, and only on confirm is `DELETE` issued, after which the calendar disappears from the list.
7. **Backend rejection despite valid client input:** *Given* client validation passed, *When* the backend returns a 4xx (e.g., overlap/duplicate), *Then* the UI shows a user-friendly error and preserves the form input.

---

## 11. Data Model (conceptual — display shapes only, no persistence)

- **Calendar:** id, campusId, academicYear, semesterIdentifier, semesterStartDate, semesterEndDate, holidays[], examWindows[], orientationPeriods[], createdAt, updatedAt.
- **Holiday:** id, calendarId, startDate, endDate, description, scope.
- **ExamWindow:** id, calendarId, startDate, endDate, examType, description.
- **OrientationPeriod:** id, calendarId, startDate, endDate, description.
- **WorkingDayPattern:** id, campusId, patternType, workingSaturdays, customDefinition, createdAt, updatedAt.

*(Shapes taken directly from A4-9 DTOs — the UI does not add fields.)*

---

## 12. Dependencies

- **A4-9 backend** (implemented) — verified endpoints under `/api/v1/academic-calendars`:
  - `POST /academic-calendars` (create), `GET /{id}`, `GET /campus/{campusId}`, `GET /year/{academicYear}`, `DELETE /{id}`
  - `POST /{calendarId}/holidays`, `DELETE /{calendarId}/holidays/{holidayId}`
  - `POST /{calendarId}/exam-windows`
  - `POST /{calendarId}/orientation-periods`
  - `POST /patterns`, `GET /patterns/campus/{campusId}`, `PUT /patterns/campus/{campusId}`
  - (query endpoints `GET /query/is-working-day`, `GET /query/is-exam-window` exist but are for the scheduling engine, not this UI)
- **Campus list** — existing `useCampuses` hook (`features/master-data/campus-hierarchy/api/useCampuses.js`) supplies the campus selector options.
- **Shared infra** — `@/lib/api-client`, TanStack Query, router (`app/router.jsx`), `AppShell` layout.

---

## 13. Assumptions

1. The Registrar is authenticated; RBAC/authorization is out of scope for this story.
2. The campus selector reuses the existing `useCampuses` hook and its data shape.
3. Success responses are wrapped as `{ "data": ... }` and errors follow the standard error envelope (`status`, `error`, `message`, `path`, `details[]`) per API standards.
4. The working-day pattern is exactly one per campus (backend exposes create + get-by-campus + update-by-campus, no list).

---

## 14. Consistency Notes (contradiction check)

- **"create/edit/delete" for sub-lists vs. backend contract.** The story scope says holidays, exam windows, and orientation periods each have "create/edit/delete". The verified A4-9 contract provides: holidays = **add + delete** (no edit); exam windows = **add only** (no edit, no delete); orientation periods = **add only** (no edit, no delete); calendars = **create + delete** (no edit). There is **no update endpoint** for any sub-entity or for the calendar itself. This is a real mismatch. This document scopes the UI to **only the operations the backend supports** and raises the gap as **OQ-3** rather than inventing endpoints (which would violate the frontend-only boundary and the no-invention rule). "Edit" of a sub-entity, if required, would be delete-then-re-add for holidays, and is **not possible at all** for exam windows / orientation periods without a backend change.
- **Holiday scope display.** AC-2/AC-3 require institution-wide holidays to show as applying to all campuses. The UI displays the `scope` field returned by the API; whether an institution-wide holiday is physically replicated across campus calendars or resolved at read time is a backend concern (OQ-4). The UI must not assume replication.
- No internal FR contradicts another; all FRs map to existing endpoints.

---

## 15. Out of Scope

- Any backend, API, schema, or validation-rule change (owned by A4-9).
- Time-slot grid UI (separate frontend story, A4-10 frontend).
- RBAC / authorization / login.
- Scheduling, conflict detection, or session-impact visualization (this UI does not render impact-detection results even though add-holiday/pattern-update trigger it backend-side).
- Editing exam windows / orientation periods / calendars, and editing holidays in place — **blocked by missing backend endpoints** (see OQ-3).
- Bulk import/export of calendar data.

---

## 16. Open Questions

| # | Question | Why it matters | Proposed default (pending confirmation) |
|---|----------|----------------|------------------------------------------|
| OQ-1 | Are "end ≥ start" and the pattern conditional-required rules enforced by A4-9, or only advisory here? | Determines whether client validation can be trusted or is purely UX. | Treat as advisory client-side; always handle backend 4xx. |
| OQ-2 | What exact error shapes/codes does A4-9 return for overlaps, duplicates, and invalid ranges? | Needed to map backend field errors to form fields (FR-2.4). | Assume standard error envelope with `details[].field`. |
| OQ-3 | The story asks for edit/delete on sub-entities, but the backend lacks update endpoints (and lacks delete for exam windows/orientation, and lacks calendar edit). Do we (a) descope edit to match backend, (b) request backend additions in A4-9, or (c) implement delete-then-re-add where possible? | Directly changes deliverable scope; blocks AC coverage for "edit". | (a) Descope edit for this story; flag backend gap for A4-9 follow-up. |
| OQ-4 | For institution-wide holidays, does the API return them under every campus's calendar, or must the UI display them separately? | Determines how AC-3 is rendered. | Display based on the `scope` field on whichever calendar the API returns them under. |
| OQ-5 | Is `GET /year/{academicYear}` needed by this UI (e.g., a cross-campus year view), or is campus-scoped listing sufficient? | Avoids building an unused view. | Campus-scoped listing only for this story. |

---

## 17. Traceability

| BRD / Story requirement | Covered by |
|-------------------------|-----------|
| BRD 6.1 — semester start/end, holidays, exam windows, orientation, per-campus variation | FR-1, FR-2, FR-3, FR-4, FR-5, FR-6 |
| BRD 7.4 — academic calendar; working-days pattern (5-day/6-day, alternate Saturdays, campus-specific) | FR-7 |
| BRD 6.12 — campus-specific calendars within one unified system | FR-1.1, UC-2, campus selector |
| Story AC-1 — create calendar for selected campus | FR-2, AC-1 |
| Story AC-2 — campus-specific holiday under that campus | FR-4.2, AC-2 |
| Story AC-3 — institution-wide holiday applies to all | FR-4.2, AC-3 (see OQ-4) |
| Story AC-4 — working-day pattern saved & displayed | FR-7, AC-4 |
| Story AC-5 — invalid date range → inline error, not saved | FR-2.2, §8, AC-5 |
| Story "confirmation on delete; skeletons, empty states, toasts" | FR-9 |
| Story "each with create/edit/delete" (edit portion) | **Not fully coverable — see OQ-3 / §14** |
