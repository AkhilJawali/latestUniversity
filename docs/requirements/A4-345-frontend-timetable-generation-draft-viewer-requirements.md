# Requirement Document — Frontend: Timetable Generation and Draft Viewer

## 1. Introduction

This document captures the requirements for the coordinator-facing frontend screen that lets a Department Coordinator **trigger** timetable generation, **watch its progress**, and **view the produced draft** — placed sessions, feasibility and quality scores, and the soft-constraint violations list.

This is a **read-only display + trigger** frontend story. It consumes REST endpoints that already exist in the backend (A4-11 Timetable Generation Engine, A4-12 Timeout and Infeasibility Handling). It does **not** define or change any backend behavior, and it does **not** cover drag-and-drop editing or real-time (< 2 s) conflict feedback — those are owned by A4-15. It builds on the SPA foundation delivered by A4-335 (React 18 + JSX, React Router v6, TanStack Query, Zustand, the `/api/v1` axios client, and the app shell + design system).

## 2. User Story

**A4-345:** As a Department Coordinator, I want to trigger timetable generation, watch its progress, and view the produced draft — placed sessions in a table, feasibility and quality scores, and the soft-constraint violations list — so that I can see and evaluate the scheduling engine's output.

**Story Points:** 5

**BRD Requirements:**
- **6.2** — "System shall generate a proposed weekly timetable per batch/section." (This story surfaces the trigger and the produced draft.)
- **6.10** — "Provide a feasibility/quality score and a list of unresolved soft-constraint violations for each generated draft." (This story displays the scores and the violations list.)
- **Section 8** — "Auto-generate a full department timetable within 2 minutes." (This story provides progress visibility while the engine runs; it does not implement the 2-minute NFR, which is backend-owned in A4-11.)

## 3. Actors

| Actor | Interaction |
|---|---|
| Department Coordinator | Selects department/semester, triggers generation, watches progress, reviews the produced draft, scores, violations, and (on failure modes) unplaced sessions / infeasibility report |
| Backend Scheduling API (A4-11 / A4-12) | Accepts the generate request, returns status/progress, draft, sessions, violations, infeasibility report, unplaced sessions |
| SPA Foundation (A4-335) | Provides router, app shell, `/api/v1` API client, TanStack Query + Zustand providers, and design-system primitives |

## 4. User Journeys

### Journey 1 — Trigger generation and watch progress

**Before:**
- Master data is configured (backend precondition, validated by A4-11's `SchedulingDataLoader`). The coordinator has navigated to the Timetable Generation screen (route `/scheduling/generation`, currently a placeholder in A4-335's router).
- The coordinator has selected a **department**, **semester**, and **academic year** [see OQ#1 — source of the selection lists].

**During:**
1. Coordinator clicks **Generate**.
2. The screen submits `POST /api/v1/timetables/generate` with `{ departmentId, semester, academicYear, seed? }`.
3. The backend responds `202 Accepted` with `{ requestId, status, departmentId, semester, statusUrl }`.
4. The screen shows a progress indicator and begins **polling** `GET /api/v1/timetables/generate/{requestId}/status` [see OQ#2 — polling cadence].
5. Each poll displays: `status`, `phase`, `bestSoFarCount` of `totalSessions`, and `elapsedSeconds`.
6. Polling continues while `status = IN_PROGRESS`.

**After:**
- When `status` reaches a terminal value, polling stops and the screen routes the coordinator to the appropriate result view (Journey 2, 3, or 4).
- Terminal statuses are defined by the backend enum (referenced, not owned here): `COMPLETED`, `TIMED_OUT`, `INFEASIBLE`, `FAILED`, `CANCELLED`.

### Journey 2 — View a completed draft

**Before:** `status = COMPLETED` and the status response carries a `draftId`.

**During:**
1. The screen calls `GET /api/v1/timetables/{draftId}` and displays the draft summary: **feasibility score**, **quality score**, `totalSessionsRequired`, `totalSessionsPlaced`, `version`, `violationCount`, and `generatedAt`.
2. The screen calls `GET /api/v1/timetables/{draftId}/sessions?page=&size=&sort=` and displays placed sessions in a **sortable, paginated table**.
3. The coordinator sorts and pages through the sessions table.
4. The coordinator opens the **violations** view: `GET /api/v1/timetables/{draftId}/violations`, displaying each violation's `constraintType`, affected entity, `description`, and `relaxationReason`.

**After:**
- The coordinator has evaluated the output. Further editing (manual placement, re-generation, locking) is out of scope here — owned by A4-14/A4-15.

### Journey 3 — Timed-out (partial) result

**Before:** `status = TIMED_OUT`. A partial draft was persisted with `feasibilityScore < 1.0` (per A4-12).

**During:**
1. The screen clearly labels the result as **partial / timed out** (distinct from a complete draft).
2. It displays the draft summary and placed sessions exactly as in Journey 2.
3. It additionally calls `GET /api/v1/timetables/{draftId}/unplaced` and displays the **unplaced sessions** list: `courseCode`, `courseName`, `facultyName`, `batchName`, `sessionType`, `requiredDurationMinutes`, and `reason`.

**After:**
- The coordinator understands what was placed vs. what remains. Adjusting inputs and re-running is available via re-triggering generation.

### Journey 4 — Infeasible result

**Before:** `status = INFEASIBLE`. No complete draft exists; an infeasibility report was persisted (per A4-12).

**During:**
1. The screen clearly labels the result as **infeasible**.
2. It calls `GET /api/v1/timetables/generate/{requestId}/infeasibility` and displays the report: `summary`, `detectedAt`, and the list of `conflicts` — each with `affectedSessionDescription`, `conflictingConstraints`, and `explanation`.

**After:**
- The coordinator has actionable information about which constraints conflict and can adjust inputs and re-run.

### Journey 5 — Error and empty states

**Before:** An API call fails (network error, `503` queue-full on generate, `404` on a missing draft/report) or returns empty data.

**During:**
1. On `503` from generate (queue full), the screen shows a non-technical retry message.
2. On other API errors, the screen shows a user-friendly message via a toast or inline alert — never a raw error or stack trace (frontend security standard).
3. Empty result sets (e.g., zero violations) render a clear empty state rather than a blank area.

**After:** The coordinator can retry without a full page reload.

## 5. Functional Requirements

Organized by coordinator-observable behavior, not by internal mechanism.

### FR-1: Generation Trigger
- **FR-1.1** The screen shall provide a **Generate** action that submits `POST /api/v1/timetables/generate` with `departmentId` (required), `semester` (required), `academicYear` (required), and `seed` (optional).
- **FR-1.2** While a generate request is being submitted, the Generate action shall show a pending/disabled state (no double-submit).
- **FR-1.3** On `202 Accepted`, the screen shall retain the returned `requestId` and `statusUrl` and begin progress polling (FR-2).
- **FR-1.4** On `503 Service Unavailable` (engine queue full), the screen shall display a retry-later message and re-enable the Generate action.

### FR-2: Progress Display (Polling)
- **FR-2.1** The screen shall poll `GET /api/v1/timetables/generate/{requestId}/status` while `status = IN_PROGRESS` [interval per OQ#2].
- **FR-2.2** The screen shall display, from the status response: `status`, `phase` (one of the six backend phases), `bestSoFarCount` / `totalSessions`, and `elapsedSeconds`.
- **FR-2.3** The screen shall stop polling when `status` becomes any terminal value and shall route to the matching result view.
- **FR-2.4** The screen shall not present generation as a silent black box — progress information (FR-2.2) is visible throughout (BRD Section 8, A4-12 AC-5).

### FR-3: Completed Draft Display
- **FR-3.1** On `COMPLETED`, the screen shall fetch `GET /api/v1/timetables/{draftId}` and display **feasibility score** and **quality score** (BRD 6.10), plus `totalSessionsRequired`, `totalSessionsPlaced`, `version`, `violationCount`, and `generatedAt`.
- **FR-3.2** The score presentation format (raw decimal vs. percentage) shall follow OQ#3.

### FR-4: Placed Sessions Table
- **FR-4.1** The screen shall fetch `GET /api/v1/timetables/{draftId}/sessions` with Spring `Pageable` params (`page`, `size`, `sort`) and render a **paginated, sortable** table.
- **FR-4.2** The table shall display the fields returned by `ScheduledSessionDto`: `dayOfWeek`, `slotDefinitionId`, `courseId`, `facultyId`, `batchId`, `sectionId`, `roomId`, `sessionType`, and `isLocked`.
- **FR-4.3** Because the sessions endpoint returns **identifiers, not human-readable names**, the display of names (course code/name, faculty name, room label, slot time) depends on OQ#4. Until resolved, the table shall render the identifiers as returned.
- **FR-4.4** Default page size and max page size shall follow the backend/API standard (default 20, max 100) unless overridden [OQ#5].

### FR-5: Soft-Constraint Violations Display
- **FR-5.1** The screen shall fetch `GET /api/v1/timetables/{draftId}/violations` and display each violation's `constraintType`, `affectedEntityType`, `affectedEntityId`, `description`, and `relaxationReason` (BRD 6.10).
- **FR-5.2** An empty violations list shall render an explicit "no soft-constraint violations" empty state.

### FR-6: Timed-Out (Partial) Result Display
- **FR-6.1** On `TIMED_OUT`, the screen shall clearly indicate a **partial** result distinct from a complete draft.
- **FR-6.2** The screen shall display the draft summary and placed sessions as in FR-3/FR-4, and shall additionally fetch `GET /api/v1/timetables/{draftId}/unplaced` and display the unplaced sessions (course, faculty, batch, session type, required duration, reason).

### FR-7: Infeasible Result Display
- **FR-7.1** On `INFEASIBLE`, the screen shall clearly indicate an **infeasible** outcome.
- **FR-7.2** The screen shall fetch `GET /api/v1/timetables/generate/{requestId}/infeasibility` and display the report `summary`, `detectedAt`, and the list of conflicts (`affectedSessionDescription`, `conflictingConstraints`, `explanation`).

### FR-8: Other Terminal States
- **FR-8.1** On `FAILED`, the screen shall show a user-friendly failure message (no stack traces) and allow re-triggering.
- **FR-8.2** On `CANCELLED`, the screen shall indicate the generation was cancelled. (Triggering cancellation from this screen is governed by OQ#6.)

### FR-9: Error Handling and Data Fetching
- **FR-9.1** All backend calls shall go through the single A4-335 `/api/v1` API client; no component shall construct its own HTTP client.
- **FR-9.2** Server state (status, draft, sessions, violations, unplaced, infeasibility) shall be managed with TanStack Query hooks per the A4-335 foundation.
- **FR-9.3** API errors shall surface as user-friendly messages (toast or inline), never raw errors or stack traces.
- **FR-9.4** All navigation between the trigger view and result views shall occur via React Router without a full page reload.

## 6. Constraints Owned by This Document

This is a frontend display story and **owns no business constraints**. All scheduling constraints (hard/soft), scoring logic, timeout, and infeasibility detection are owned by A4-11 and A4-12.

The UI-level rules this document owns:
- **UI-1** Progress must remain visible while `status = IN_PROGRESS` (no black box).
- **UI-2** Partial (`TIMED_OUT`) and infeasible (`INFEASIBLE`) outcomes must be visually distinct from a complete (`COMPLETED`) draft.
- **UI-3** No raw backend error, ID-only internal detail, or stack trace may be shown to the user as an error message.

## 7. Constraints Referenced from Other Documents

| Referenced item | Owner | How consumed here |
|---|---|---|
| Feasibility score, quality score, soft-constraint violations | A4-11 | Displayed read-only (FR-3, FR-5) |
| 2-minute generation target (Section 8 NFR) | A4-11 | Backend behavior; this screen only shows progress/elapsed time |
| Timeout → partial draft + unplaced sessions; infeasibility report | A4-12 | Displayed read-only (FR-6, FR-7) |
| `GenerationStatus` values (`IN_PROGRESS`, `COMPLETED`, `TIMED_OUT`, `INFEASIBLE`, `FAILED`, `CANCELLED`) and `GenerationPhase` values | A4-11 / A4-12 | Drive polling and result routing (FR-2, FR-3, FR-6, FR-7, FR-8) |
| SPA foundation (router, `/api/v1` client, TanStack Query, Zustand, app shell, design system) | A4-335 | Base for all components (FR-9) |
| Drag-and-drop editing, real-time (< 2 s) conflict feedback | A4-15 | Explicitly out of scope (Section 15) |

## 8. Validation Rules

Client-side validation on the generate form (allowlist-based, before submission; the backend re-validates):

| Field | Rule |
|---|---|
| `departmentId` | Required; must be a selected department |
| `semester` | Required; non-blank |
| `academicYear` | Required; non-blank |
| `seed` | Optional; if present, numeric |

Form validation shall use Zod per the frontend standards (no TypeScript types).

## 9. Non-Functional Requirements

- **NFR-1 (Usability, BRD Section 8):** The screen must be operable by coordinators with minimal training — clear labels for scores, progress, and outcome states.
- **NFR-2 (Responsiveness):** Coordinator view is desktop-first per UI standards; the sessions table may scroll horizontally on narrower widths.
- **NFR-3 (Security):** No `dangerouslySetInnerHTML`; all backend-provided strings (violation descriptions, infeasibility explanations) rendered as text. CSP compatibility per A4-335. No secrets in frontend code.
- **NFR-4 (Accessibility, WCAG AA):** Progress and outcome state changes announced to screen readers; the sessions table is keyboard-navigable; icon-only controls have ARIA labels; outcome status color is paired with a text label (not color-only).
- **NFR-5 (Performance):** Heavy views lazy-loaded via route-based code splitting; background refetches do not block the UI; skeleton placeholders during initial loads (no layout shift).

## 10. Acceptance Criteria

1. **Given** master data is configured, **When** the coordinator selects a department/semester/academic year and clicks Generate, **Then** `POST /api/v1/timetables/generate` is submitted and a progress indicator appears.
2. **Given** a generation is in progress, **When** the coordinator views status, **Then** best-so-far session count, elapsed time, and phase are displayed by polling the status endpoint, and the display updates until a terminal status is reached.
3. **Given** a `COMPLETED` draft, **When** the coordinator views it, **Then** placed sessions appear in a sortable, paginated table and the feasibility and quality scores are displayed.
4. **Given** a `COMPLETED` draft, **When** the coordinator opens the violations view, **Then** the soft-constraint violations are listed with descriptions and relaxation reasons; **and** when there are none, an explicit empty state is shown.
5. **Given** a `TIMED_OUT` result, **When** it is returned, **Then** the screen labels it as partial and displays both the placed sessions and the unplaced sessions list.
6. **Given** an `INFEASIBLE` result, **When** it is returned, **Then** the screen labels it as infeasible and displays the infeasibility summary and the conflicting-constraints list.
7. **Given** a `503` (queue full) or other API error on any call, **When** it occurs, **Then** a user-friendly message is shown (no stack trace) and the coordinator can retry without a full page reload.

## 11. Data Model (Conceptual — View Models Only)

No persistent data is created by this story. The screen maps backend DTOs to view models:

- **GenerationStatusView** ← `GenerationStatusDto`: requestId, status, phase, progress, elapsedSeconds, bestSoFarCount, totalSessions, draftId, cancellable, draftUrl, statusUrl.
- **DraftSummaryView** ← `TimetableDraftDto`: id, feasibilityScore, qualityScore, totalSessionsRequired, totalSessionsPlaced, version, status, violationCount, generatedAt.
- **SessionRowView** ← `ScheduledSessionDto` (paginated): id, dayOfWeek, slotDefinitionId, courseId, facultyId, batchId, sectionId, roomId, sessionType, isLocked.
- **ViolationView** ← `SoftConstraintViolationDto`: constraintType, affectedEntityType, affectedEntityId, description, relaxationReason.
- **UnplacedSessionView** ← `UnplacedSessionDto`: courseCode, courseName, facultyName, batchName, sessionType, requiredDurationMinutes, reason.
- **InfeasibilityReportView** ← `InfeasibilityReportDto`: summary, detectedAt, conflicts[{ affectedSessionDescription, conflictingConstraints, explanation }].

## 12. Dependencies

- **A4-335** — SPA foundation (router, `/api/v1` client, TanStack Query, Zustand, app shell, design system). **Hard dependency.** The `/scheduling/generation` route placeholder is replaced by this story's screen.
- **A4-11 / A4-12** — backend generation, status, draft, sessions, violations, unplaced, and infeasibility endpoints. **Already exist; no backend work in this story.**
- **Master-data lookup APIs** — needed to resolve the department/semester selection lists (OQ#1) and, potentially, session IDs → names (OQ#4). Ownership and availability to be confirmed.

## 13. Assumptions

1. The backend endpoints in Section 4 are stable and deployed (confirmed present in `SchedulingController`, A4-11/A4-12).
2. The design-system foundation (Tailwind + Shadcn/ui per UI standards) is finalized and available from A4-335's design phase.
3. The POST `/generate` response places the request at a pollable state immediately (`202 Accepted`); there is no separate client-side queued state to model.
4. There is no separate `PARTIAL` status — a timed-out generation is represented by `status = TIMED_OUT` plus a draft with `feasibilityScore < 1.0` and unplaced sessions (per A4-12 design).

## 14. Consistency Notes (Contradiction Check)

- **Status set alignment:** The result-routing logic (FR-3, FR-6, FR-7, FR-8) uses exactly the six backend `GenerationStatus` values. No invented status (e.g., `PARTIAL`, `QUEUED`) is used — Assumption 4 documents this explicitly.
- **Names vs. IDs:** FR-4.2 lists ID fields because `ScheduledSessionDto` returns IDs, while `UnplacedSessionDto` (FR-6.2) returns names. This asymmetry is real in the backend contract and is surfaced as OQ#4 rather than silently assuming a name-resolution mechanism.
- **Cancel capability:** The status DTO exposes `cancellable`, and the backend has a cancel endpoint, but the story scope is "trigger + read-only display." Whether this screen offers cancel is deferred to OQ#6 rather than assumed either way.
- **2-minute NFR:** Referenced (Section 7), not owned — this screen displays elapsed time but does not enforce the limit.

## 15. Out of Scope

- Drag-and-drop timetable editing and real-time (< 2 s) conflict feedback — owned by **A4-15**.
- Manual session placement, locking/unlocking, and partial re-generation — owned by **A4-14** (the backend endpoints exist on the same controller but are not consumed by this screen).
- Approval workflow, publication, and calendar export.
- Any change to backend generation, scoring, timeout, or infeasibility logic (A4-11 / A4-12).
- Master-data CRUD screens (departments, courses, faculty, rooms, time-slots).

## 16. Open Questions

| # | Question | Impact | Proposed default (pending confirmation) |
|---|---|---|---|
| OQ#1 | What is the source of the **department / semester / academic-year selection lists** on the generate form? Is a master-data lookup API available to the frontend yet? | Blocks the trigger form's selection controls (FR-1.1) | Use master-data list endpoints when available; until then, accept typed/known values [TBD — confirm with stakeholder] |
| OQ#2 | What **polling interval** should the status endpoint use, and is there a client-side max polling duration? | Affects FR-2.1 progress UX and backend load | [TBD] Propose 2 s interval, stop on terminal status; no BRD value exists |
| OQ#3 | How should **feasibility/quality scores** be presented — raw decimal (e.g., 0.87) or percentage (87%)? Backend returns `BigDecimal`/`Double`. | Affects FR-3.1 display | [TBD] Propose percentage with one decimal; confirm score range semantics |
| OQ#4 | The `/sessions` endpoint returns **IDs, not names** (`courseId`, `facultyId`, `roomId`, `slotDefinitionId`). How should the table render human-readable course/faculty/room/time? Is there a lookup API, or should the sessions DTO be enriched (backend change, out of scope here)? | Affects FR-4.2/FR-4.3 readability and coordinator usability | [TBD] Render IDs until a name-resolution source is confirmed |
| OQ#5 | Confirm **default and max page size** for the sessions table (API standard says default 20 / max 100). | Affects FR-4.4 | [TBD] Use API-standard defaults unless product specifies otherwise |
| OQ#6 | Is **cancel generation** in scope for this screen (the status DTO exposes `cancellable` and a cancel endpoint exists), or strictly out of scope per the "trigger + read-only display" story framing? | Adds/removes FR-8.2-related cancel action | [TBD] Treat as out of scope for A4-345 unless product confirms otherwise |

## 17. Traceability

| BRD / Source | Requirement here | Notes |
|---|---|---|
| BRD 6.2 — generate weekly timetable per batch/section | FR-1 (trigger), FR-4 (placed sessions table) | Frontend surfaces the trigger and the produced sessions |
| BRD 6.10 — feasibility/quality score + unresolved soft-constraint violations | FR-3 (scores), FR-5 (violations) | Direct display of A4-11 outputs |
| BRD Section 8 — auto-generate within 2 minutes (progress visibility) | FR-2 (progress polling), NFR-1 | Displays progress/elapsed; NFR itself owned by A4-11 |
| A4-11 endpoints (generate, status, draft, sessions, violations) | FR-1, FR-2, FR-3, FR-4, FR-5 | Contract consumed as-is |
| A4-12 endpoints (status extension, infeasibility, unplaced) + terminal states | FR-6, FR-7, FR-8 | Contract consumed as-is |
| A4-335 SPA foundation | FR-9 | Router, API client, query/state layers, design system |
| Story AC-1..AC-5 | AC-1..AC-7 (this doc) | Expanded with explicit error/empty-state criteria (AC-4, AC-7) |
