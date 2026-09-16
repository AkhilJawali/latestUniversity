# Design Document — Frontend: Timetable Generation and Draft Viewer (A4-345)

## 1. Introduction

This document derives the technical design for the coordinator-facing **Timetable Generation and Draft Viewer** screen from the approved requirement document (`A4-345-frontend-timetable-generation-draft-viewer-requirements.md`, subtask A4-346 = Approved). It specifies the feature module structure, routing, TanStack Query hooks, components, state handling, and the exact backend contracts consumed. It changes no backend behavior and adds no backend endpoints.

**Source of truth for contracts:** the existing `SchedulingController` (A4-11 / A4-12) and its DTOs, read directly from the codebase. **Source of truth for the frontend base:** the A4-335 foundation as it actually exists in `frontend/` (React 18 + JSX, Vite, React Router v6, TanStack Query, Zustand, axios `/api/v1` client, and **plain CSS with CSS variables** — see PD-1).

## 2. Architecture Overview

```
frontend/src/
├── app/
│   └── router.jsx                         # (edit) replace /scheduling/generation placeholder with lazy GenerationPage
├── features/
│   └── scheduling/
│       └── generation/
│           ├── api/
│           │   └── useGeneration.js        # TanStack Query hooks (all 7 endpoints)
│           ├── components/
│           │   ├── GenerateForm.jsx        # trigger form (dept/semester/year/seed)
│           │   ├── ProgressPanel.jsx       # status/phase/best-so-far/elapsed while IN_PROGRESS
│           │   ├── DraftSummary.jsx         # feasibility/quality scores + counts
│           │   ├── SessionsTable.jsx        # paginated, sortable placed-sessions table
│           │   ├── ViolationsList.jsx       # soft-constraint violations
│           │   ├── UnplacedList.jsx         # unplaced sessions (TIMED_OUT)
│           │   ├── InfeasibilityReport.jsx  # infeasibility summary + conflicts
│           │   └── OutcomeBanner.jsx         # COMPLETED / TIMED_OUT / INFEASIBLE / FAILED / CANCELLED label
│           ├── lib/
│           │   ├── generationSchema.js      # Zod schema for the form
│           │   └── formatScore.js           # score formatting helper (PD-4)
│           └── GenerationPage.jsx           # orchestrates form → progress → result views
```

**Data flow:** `GenerationPage` holds the current `requestId` (page-scoped `useState`, not global — no Zustand store needed for a single screen). Hooks in `useGeneration.js` own all server state via TanStack Query. Polling is driven by the status query's `refetchInterval`. When status becomes terminal, the page renders the matching result view and the interval stops.

## 3. API Contracts Consumed (verified against backend)

Base URL: `/api/v1` (via the shared `apiClient`; Vite proxies `/api` → `localhost:8080`).

| # | Method / Path | Request | Response (fields used) | Requirement |
|---|---|---|---|---|
| 1 | `POST /timetables/generate` | `{ departmentId:number, semester:string, academicYear:string, seed?:number }` | `202` → `{ requestId, status, departmentId, semester, statusUrl }` | FR-1 |
| 2 | `GET /timetables/generate/{requestId}/status` | — | `{ requestId, status, progress, phase, elapsedSeconds, departmentId, semester, draftId, bestSoFarCount, totalSessions, cancellable, draftUrl, statusUrl }` | FR-2 |
| 3 | `GET /timetables/{draftId}` | — | `{ id, departmentId, semester, academicYear, status, version, feasibilityScore, qualityScore, totalSessionsRequired, totalSessionsPlaced, generationRequestId, generatedAt, violationCount }` | FR-3 |
| 4 | `GET /timetables/{draftId}/sessions?page=&size=&sort=` | Spring `Pageable` | `Page<{ id, courseId, facultyId, batchId, sectionId, roomId, dayOfWeek, slotDefinitionId, sessionType, isLocked }>` | FR-4 |
| 5 | `GET /timetables/{draftId}/violations` | — | `[{ id, constraintType, affectedEntityType, affectedEntityId, description, relaxationReason }]` | FR-5 |
| 6 | `GET /timetables/{draftId}/unplaced` | — | `[{ id, draftId, courseId, courseCode, courseName, facultyId, facultyName, batchId, batchName, sessionType, requiredDurationMinutes, reason }]` | FR-6 |
| 7 | `GET /timetables/generate/{requestId}/infeasibility` | — | `{ id, generationRequestId, detectedAt, summary, conflicts:[{ id, affectedSessionDescription, conflictingConstraints, explanation }] }` (404 if not INFEASIBLE) | FR-7 |

**Backend enums referenced (not owned):**
- `GenerationStatus`: `IN_PROGRESS`, `COMPLETED`, `TIMED_OUT`, `INFEASIBLE`, `FAILED`, `CANCELLED`.
- `GenerationPhase`: `LOADING_DATA`, `DERIVING_SESSIONS`, `PROPAGATING`, `SOLVING`, `OPTIMIZING`, `STORING_RESULTS`.

Terminal set = every status except `IN_PROGRESS`.

## 4. Routing

`app/router.jsx` — replace the `/scheduling/generation` placeholder:

```jsx
import { lazy, Suspense } from 'react';
// ...
const GenerationPage = lazy(() => import('@/features/scheduling/generation/GenerationPage'));

{
  path: '/scheduling/generation',
  element: (
    <AppShell>
      <Suspense fallback={<div className="empty-state">Loading…</div>}>
        <GenerationPage />
      </Suspense>
    </AppShell>
  ),
}
```

Route-based code splitting satisfies NFR-5. The `AppShell` and existing global CSS classes (`.page-title`, `.card`, `.empty-state`, etc.) are reused.

## 5. TanStack Query Hooks (`api/useGeneration.js`)

All hooks use the shared `apiClient`. Query keys are namespaced under `['generation', ...]`.

```js
import { useMutation, useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';

const TERMINAL = ['COMPLETED', 'TIMED_OUT', 'INFEASIBLE', 'FAILED', 'CANCELLED'];

// FR-1: trigger
export function useTriggerGeneration() {
  return useMutation({
    mutationFn: (body) =>
      apiClient.post('/timetables/generate', body).then((r) => r.data),
  });
}

// FR-2: poll status (PD-2: 2s interval, stops on terminal)
export function useGenerationStatus(requestId) {
  return useQuery({
    queryKey: ['generation', 'status', requestId],
    queryFn: () =>
      apiClient
        .get(`/timetables/generate/${requestId}/status`)
        .then((r) => r.data),
    enabled: requestId != null,
    refetchInterval: (query) =>
      TERMINAL.includes(query.state.data?.status) ? false : 2000,
  });
}

// FR-3: draft summary
export function useDraft(draftId) {
  return useQuery({
    queryKey: ['generation', 'draft', draftId],
    queryFn: () => apiClient.get(`/timetables/${draftId}`).then((r) => r.data),
    enabled: draftId != null,
  });
}

// FR-4: sessions (paginated, sortable)
export function useSessions(draftId, { page, size, sort }) {
  return useQuery({
    queryKey: ['generation', 'sessions', draftId, page, size, sort],
    queryFn: () =>
      apiClient
        .get(`/timetables/${draftId}/sessions`, { params: { page, size, sort } })
        .then((r) => r.data),
    enabled: draftId != null,
    keepPreviousData: true,
  });
}

// FR-5: violations
export function useViolations(draftId) {
  return useQuery({
    queryKey: ['generation', 'violations', draftId],
    queryFn: () =>
      apiClient.get(`/timetables/${draftId}/violations`).then((r) => r.data),
    enabled: draftId != null,
  });
}

// FR-6: unplaced (TIMED_OUT)
export function useUnplaced(draftId, enabled) {
  return useQuery({
    queryKey: ['generation', 'unplaced', draftId],
    queryFn: () =>
      apiClient.get(`/timetables/${draftId}/unplaced`).then((r) => r.data),
    enabled: enabled && draftId != null,
  });
}

// FR-7: infeasibility (INFEASIBLE) — gated so we never call it unless status is INFEASIBLE (avoids the backend 404)
export function useInfeasibility(requestId, enabled) {
  return useQuery({
    queryKey: ['generation', 'infeasibility', requestId],
    queryFn: () =>
      apiClient
        .get(`/timetables/generate/${requestId}/infeasibility`)
        .then((r) => r.data),
    enabled: enabled && requestId != null,
  });
}
```

## 6. Component Behaviors

### GenerationPage.jsx (orchestrator, < 150 lines)
- Local state: `requestId` (from trigger response), sessions table `page/size/sort`.
- Renders `GenerateForm`. On success, stores `requestId`.
- Subscribes to `useGenerationStatus(requestId)`; renders `ProgressPanel` while `status === 'IN_PROGRESS'`.
- On terminal status, renders `OutcomeBanner` plus the matching result section:
  - `COMPLETED` → `DraftSummary` + `SessionsTable` + `ViolationsList` (FR-3/4/5).
  - `TIMED_OUT` → same as COMPLETED **plus** `UnplacedList` (FR-6); banner labels it partial (UI-2).
  - `INFEASIBLE` → `InfeasibilityReport` (FR-7); banner labels it infeasible (UI-2).
  - `FAILED` / `CANCELLED` → banner message only (FR-8).

### GenerateForm.jsx (FR-1, validation Section 8)
- Fields: `departmentId` (number), `semester` (text), `academicYear` (text), `seed` (optional number). **PD-3:** free inputs until a master-data lookup exists (OQ#1 carried forward).
- Validates with Zod (`generationSchema.js`) before submit; inline errors below fields (ui-standards).
- Submit button shows pending/disabled during the mutation (FR-1.2). On `503`, shows a retry-later inline message (FR-1.4). On other errors, an inline alert (FR-9.3).

### ProgressPanel.jsx (FR-2, UI-1)
- Displays `status`, `phase`, `bestSoFarCount` / `totalSessions`, `elapsedSeconds`.
- Uses a determinate progress bar when `progress` is present, else an indeterminate indicator. `role="status"` + `aria-live="polite"` for screen-reader announcements (NFR-4).

### DraftSummary.jsx (FR-3)
- Shows `feasibilityScore` and `qualityScore` via `formatScore` (PD-4), plus placed/required counts, `version`, `violationCount`, `generatedAt`.

### SessionsTable.jsx (FR-4)
- Paginated (`meta.totalPages`, `page`, `size`) and sortable (column header → `sort=field,dir`).
- Columns: `dayOfWeek`, `slotDefinitionId`, `courseId`, `facultyId`, `batchId`, `sectionId`, `roomId`, `sessionType`, `isLocked` (lock icon/label). **PD-5:** renders IDs as-is; name resolution deferred (OQ#4 carried forward).
- Size selector: 20 (default) / 50 / 100 (PD-6). Keyboard-navigable table; skeleton rows on load (NFR-4/5).

### ViolationsList.jsx (FR-5)
- Lists `constraintType`, affected entity (`affectedEntityType` + `affectedEntityId`), `description`, `relaxationReason`. Empty → explicit "No soft-constraint violations" empty state (FR-5.2), reusing `.empty-state`.

### UnplacedList.jsx (FR-6)
- Lists `courseCode`, `courseName`, `facultyName`, `batchName`, `sessionType`, `requiredDurationMinutes`, `reason`.

### InfeasibilityReport.jsx (FR-7)
- Shows `summary`, `detectedAt`, and the `conflicts` list (`affectedSessionDescription`, `conflictingConstraints`, `explanation`). All rendered as text (NFR-3).

### OutcomeBanner.jsx (UI-2, FR-8)
- Maps status → label + style for all five terminal states; status conveyed by **text**, not color alone (NFR-4). No color-only signalling.

## 7. Security & Accessibility

- **NFR-3:** No `dangerouslySetInnerHTML`; all backend strings (descriptions, explanations) rendered as React text nodes. No secrets in code. CSP-compatible (no inline scripts) — inherited from A4-335.
- **NFR-4 (WCAG AA):** `aria-live` for progress/outcome; ARIA labels on icon-only controls; keyboard-navigable table; status labelled with text.
- **FR-9.1:** All calls go through the single `apiClient`; no component creates its own HTTP client.

## 8. Error Handling

- Each query/mutation exposes `isError`/`error`; components render user-friendly inline messages, never raw errors or stack traces (FR-9.3, NFR-3).
- `POST /generate` `503` → retry-later message (FR-1.4).
- `GET infeasibility` `404` when status is not INFEASIBLE is avoided by gating the query on `status === 'INFEASIBLE'` (the query is disabled otherwise), matching the backend's 404 contract.
- Global `ErrorBoundary` from A4-335 remains the last-resort fallback.

## 9. Provisional Decisions (pending stakeholder ratification)

| PD | Decision | Resolves | Status |
|---|---|---|---|
| PD-1 | Build with the **actual A4-335 foundation: plain CSS + CSS variables** and existing class names — NOT Tailwind/Shadcn. The UI-standards design-system libraries are not present in `package.json`, and the delivered A4-335 foundation uses plain CSS. | Foundation mismatch | Provisional — confirm whether the design-system (Tailwind/Shadcn) will be added later; if so, a follow-up restyle story is needed |
| PD-2 | Status polling interval = **2000 ms**, stopping on any terminal status (via `refetchInterval`). | OQ#2 | Provisional |
| PD-3 | Generate form uses **free number/text inputs** for `departmentId` / `semester` / `academicYear` until a master-data lookup API is wired. | OQ#1 | Provisional |
| PD-4 | Scores displayed as **percentage with one decimal**, with the raw value in a tooltip/secondary text. | OQ#3 | Provisional |
| PD-5 | Sessions table renders **raw IDs** (course/faculty/room/slot); no name resolution in this story. | OQ#4 | Provisional — the key open item; may require a backend DTO enrichment or a lookup API (separate story) |
| PD-6 | Page size default **20**, selectable up to **100** (per API standard). | OQ#5 | Provisional |
| PD-7 | **No cancel action** on this screen (read-only + trigger scope), even though the backend supports it. | OQ#6 | Provisional |

## 10. Testing Strategy

Frontend has no configured test runner yet in `package.json` (only `dev`/`build`/`lint`/`preview`).
- **Build verification (mandatory before push):** `pnpm build` must succeed with zero errors (workflow rule 14).
- **Lint:** `pnpm lint` must pass.
- **Component/unit tests:** deferred — no test runner is configured in the A4-335 foundation. Adding Vitest + React Testing Library is out of scope for this story and should be a foundation follow-up [carried forward]. Stated explicitly rather than assuming a test setup that does not exist.
- **Manual E2E (Testing subtask A4-349):** trigger → progress → each terminal outcome, verified against a running backend.

## 11. Traceability

| Requirement | Design element |
|---|---|
| FR-1 (trigger) + validation | `GenerateForm.jsx`, `useTriggerGeneration`, `generationSchema.js` |
| FR-2 (progress) + UI-1 | `ProgressPanel.jsx`, `useGenerationStatus` (refetchInterval) |
| FR-3 (scores) | `DraftSummary.jsx`, `useDraft`, `formatScore.js` |
| FR-4 (sessions table) | `SessionsTable.jsx`, `useSessions` (page/size/sort) |
| FR-5 (violations) + FR-5.2 empty | `ViolationsList.jsx`, `useViolations` |
| FR-6 (partial/unplaced) + UI-2 | `UnplacedList.jsx`, `useUnplaced`, `OutcomeBanner` |
| FR-7 (infeasible) + UI-2 | `InfeasibilityReport.jsx`, `useInfeasibility`, `OutcomeBanner` |
| FR-8 (FAILED/CANCELLED) | `OutcomeBanner.jsx`, `GenerationPage` terminal routing |
| FR-9 (single client, TanStack Query, no reload, safe errors) | `api/useGeneration.js`, router lazy route, Section 8 |
| UI-3 / NFR-3 (safe rendering) | Section 7 |
| NFR-4 (a11y) | `ProgressPanel` aria-live, `SessionsTable` keyboard nav, `OutcomeBanner` text label |
| NFR-5 (perf) | Route-based lazy load, `keepPreviousData`, skeletons |
| AC-1..AC-7 | GenerationPage orchestration across form → progress → outcome views |

## 12. Open Questions Carried Forward

- **OQ#1** (selection source) → PD-3 provisional; needs master-data lookup.
- **OQ#4** (IDs vs names) → PD-5 provisional; highest-impact item, may need a backend change (separate story).
- **OQ#3 / OQ#5 / OQ#2 / OQ#6** → PD-4 / PD-6 / PD-2 / PD-7 provisional.
- **Frontend test runner** not configured in A4-335 — component tests deferred (Section 10).
- **Design system**: foundation is plain CSS, not Tailwind/Shadcn (PD-1) — confirm intended direction.

## 13. Out of Scope

Same as the requirement doc Section 15: drag-and-drop editing and real-time (< 2 s) conflict feedback (A4-15); manual placement/lock/regenerate (A4-14); approval/publication/calendar export; master-data CRUD screens; any backend change.
