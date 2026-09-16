# A4-15 — Frontend: Drag-and-Drop Timetable Editor with Real-Time Conflict Feedback — Design Document

| Field | Value |
|-------|-------|
| Story | A4-15 — Drag-and-Drop Timetable Editor with Real-Time Conflict Feedback |
| Design Subtask | A4-69 |
| Requirement Subtask | A4-68 (Approved) |
| Epic | A4 — UTMS |
| Role / Type | Frontend (React 18 + plain JSX, NO TypeScript) |
| Consumes | A4-16 conflict engine (REST + WebSocket, Done); A4-11/A4-12 draft/sessions (Done, via A4-345 `useGeneration`) |
| Source requirement | docs/requirements/A4-15-frontend-drag-drop-timetable-editor-requirements.md |

---

## 1. Introduction

Concrete frontend design for the drag-and-drop timetable editor derived from the approved A4-15 requirements. It adds a grid-based weekly editor over an existing draft (from A4-345), running real-time placement conflict checks against the verified A4-16 engine and rendering conflicts + candidate alternatives. It reuses the established feature conventions (feature folder, TanStack Query, `apiClient`, `mapApiError`, inline status/ARIA feedback) and introduces no new runtime dependency (see PD-A15-2 on transport).

---

## 2. Resolved Open Questions (lead-approved in chat, 2026-09-09)

| OQ | Requirement question | Design disposition (as approved) |
|----|----------------------|----------------------------------|
| OQ-1 | No endpoint to persist a moved session. | **Staged client-side mode.** Moves are held in editor view-state and clearly marked **unsaved**; the UI never claims persistence (UC-2). A backend follow-up (`PATCH /timetables/{draftId}/sessions/{sessionId}` move) is logged for a later story; when it lands, a `useMoveSession` hook drops in without redesign. |
| OQ-2 | No optimistic-concurrency mechanism. | **Interim lock/unlock.** Use the existing `POST /timetables/{draftId}/sessions/{sessionId}/lock|unlock` to advisory-lock a session while it is being repositioned; surface the draft `version` (present on the draft summary) as a staleness hint. Full version/409 rejection is a backend follow-up. |
| OQ-3 | No alternatives endpoint. | **Client-side candidate derivation.** On a conflict, the UI probes a bounded set of empty (day, slot) cells with the same conflict-check and lists those that come back clean as candidate alternatives, ordered by proximity to the original slot. Not engine-ranked; upgrades to A4-17 when available. |
| OQ-4 | STOMP transport + new dependency. | **REST-first (no new dependency).** `POST /drafts/{draftId}/conflict-check` is the primary transport (meets the <2s backend SLA and needs no library). WebSocket/STOMP is **deferred** as a future enhancement (would add `@stomp/stompjs`); the requirement's WebSocket primary is reconciled here as REST-primary + WS-later (PD-A15-2, §14). |
| OQ-5 | Does the session payload carry the placement fields? | **Mostly yes; one gap.** Verified session row fields: `id, dayOfWeek, slotDefinitionId, courseId, facultyId, batchId, sectionId, roomId, sessionType, isLocked`. **`durationMinutes` is NOT present** on the session row. Disposition: send a **default duration** for the check (the slot's own duration when resolvable from the A4-10 grid, else a configured default) and log a backend follow-up to include `durationMinutes` on the session DTO (PD-A15-6, §14). |
| OQ-6 | Feedback pattern. | **Inline status + ARIA live**, consistent with recent FE stories. No new toast system. |

---

## 3. Verified Backend Contract Consumed

- **Conflict check (A4-16), bare-list responses (no `{data}` envelope):**
  - `POST /api/v1/drafts/{draftId}/conflict-check` — body `ProposedPlacementRequest` → `200 ConflictDto[]`.
  - `GET /api/v1/drafts/{draftId}/conflicts` → `200 ConflictDto[]` (full-draft).
  - (WebSocket STOMP `/app/drafts/{draftId}/conflict-check` → `/topic/drafts/{draftId}/conflicts` exists but is **not** used this story — PD-A15-2.)
- **`ProposedPlacementRequest`:** `{facultyId!, roomId!, batchId!, sectionId?, dayOfWeek!, slotDefinitionId!, durationMinutes!, sessionId?}` (`!` = required/positive; `sessionId` self-excludes the moved session).
- **`ConflictDto`:** `{type, involvedFacultyId, involvedRoomId, involvedBatchId, involvedSectionId, involvedSessionId, dayOfWeek, slotDefinitionId, description}`.
- **`ConflictType`:** 11 values; actively detected: FACULTY_DOUBLE_BOOKING, ROOM_DOUBLE_BOOKING, BATCH_CLASH, ROOM_CAPACITY, FACULTY_DAILY_HOURS, FACULTY_WEEKLY_HOURS, FACULTY_CONSECUTIVE_HOURS; deferred: ROOM_HARD_BLOCK, FACULTY_HARD_BLOCK, TRAVEL_TIME, PREREQUISITE_SEQUENCE.
- **Draft/sessions (A4-345 `useGeneration`, `.data` payloads):** `GET /timetables/{draftId}` (has `version`, scores), `GET /timetables/{draftId}/sessions?page&size&sort` (rows as in OQ-5), `POST /timetables/{draftId}/sessions/{sessionId}/lock|unlock`.

---

## 4. Module Layout

```
frontend/src/features/scheduling/editor/
├── api/
│   ├── useConflictCheck.js      # checkPlacement (POST), useDraftConflicts (GET), lock/unlock
│   └── (reuses ../generation/api/useGeneration: useDraft, useSessions)
├── constants/
│   └── conflict-types.js        # CONFLICT_TYPE_LABELS, DETECTED vs DEFERRED sets, DAYS
├── lib/
│   ├── grid-model.js            # build {day×slot→session} grid from sessions + slot axis
│   └── placement.js             # session→ProposedPlacementRequest, alternative-slot probing
├── components/
│   ├── EditorGrid.jsx           # day×slot grid, drop targets, keyboard navigation
│   ├── SessionCard.jsx          # draggable card (course/faculty/room/batch), conflict/pending/unsaved state
│   ├── ConflictPanel.jsx        # full-draft + last-placement conflicts, links to cells
│   └── AlternativesPopover.jsx  # candidate slots for a conflicted session
├── pages/
│   └── TimetableEditorPage.jsx  # orchestrator: load draft+sessions, staged moves, feedback
└── timetable-editor.css
```
Route `/scheduling/editor/:draftId` (lazy) + an "Open editor" link from the A4-345 GenerationPage. Reuses `@/features/scheduling/generation/api/useGeneration`, `@/lib/api-client`, `@/lib/api-error`, `@/features/master-data/time-slot-grid` slot data where a campus grid is available.

---

## 5. Key / Provisional Design Decisions

- **PD-A15-1 — Staged move model (OQ-1).** Editor holds a `stagedMoves` map `{sessionId → {dayOfWeek, slotDefinitionId}}` in local state (Zustand not needed; page-level `useReducer`). The rendered grid overlays staged positions on the fetched sessions. A visible "Unsaved changes" banner + per-card "unsaved" marker communicate that moves are not persisted (UC-2). A disabled "Save layout" button carries a tooltip explaining the pending backend endpoint (OQ-1 follow-up), so the limitation is explicit, never silent.
- **PD-A15-2 — REST-first conflict check, WS deferred (OQ-4).** `useConflictCheck` calls `POST /drafts/{draftId}/conflict-check`. No STOMP dependency added. The transport is isolated behind one hook so a WS upgrade later changes only that file. Requirement FR-3.1 (WS primary) is consciously reconciled to REST-primary here and recorded in §14 as a provisional deviation for lead ratification.
- **PD-A15-3 — Self-exclusion on move.** The moved session's `id` is always sent as `sessionId` so a move within/near its own slot is not a self-conflict (per DTO contract).
- **PD-A15-4 — Client-side alternatives (OQ-3).** `placement.js#deriveAlternatives` enumerates empty cells in the current grid (bounded to the same day first, then nearby days), runs `checkPlacement` for each (capped, e.g. ≤8 probes to stay responsive), and returns clean ones ordered by slot proximity. Clearly labelled "suggested (not engine-ranked)".
- **PD-A15-5 — Interim concurrency via lock/unlock (OQ-2).** On drag-start (or keyboard pick-up) the UI issues `lock`; on drop/cancel it issues `unlock`. The draft `version` is displayed; if a refetch shows a changed version, a "draft changed — reload" prompt appears (advisory, not a hard 409 yet). No silent overwrite because nothing is persisted this story (staged mode) — the concurrency guard is about not clobbering another editor's locked session.
- **PD-A15-6 — Duration fallback (OQ-5).** `placement.js` resolves `durationMinutes` from the campus time-slot grid definition matching `slotDefinitionId` when available; otherwise uses a configurable default (e.g. 60) and flags that a backend `durationMinutes` on the session DTO would remove the heuristic (follow-up).
- **PD-A15-7 — Grid axis source.** Day axis = the distinct `dayOfWeek` values across sessions ∪ standard working days; slot axis = distinct `slotDefinitionId`s ordered by the campus grid's slot start times when resolvable, else by id. Keeps the editor usable even before the campus grid is fetched.
- **KD-A15-1 — Conflict rendering is contract-driven.** `conflict-types.js` maps every `ConflictType` to a label; deferred types are labelled but never synthesized by the UI (the UI only shows what the engine returns), so activating them later needs no UI change (§14).

---

## 6. API Hook (design) — `api/useConflictCheck.js`

```js
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';

const draftBase = (draftId) => `/drafts/${draftId}`;
const ttBase = (draftId) => `/timetables/${draftId}`;

// FR-2/FR-3 — single placement check (REST-first, PD-A15-2). Returns ConflictDto[] (bare array).
export function useConflictCheck(draftId) {
  return useMutation({
    mutationFn: async (placement) =>
      (await apiClient.post(`${draftBase(draftId)}/conflict-check`, placement)).data,
  });
}

// FR-8 — full-draft conflicts (bare array).
export function useDraftConflicts(draftId) {
  return useQuery({
    queryKey: ['editor', 'draft-conflicts', draftId],
    queryFn: async () => (await apiClient.get(`${draftBase(draftId)}/conflicts`)).data,
    enabled: draftId != null,
  });
}

// FR-6 (OQ-2 interim) — advisory lock/unlock of the session being moved.
export function useLockSession(draftId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (sessionId) =>
      (await apiClient.post(`${ttBase(draftId)}/sessions/${sessionId}/lock`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['generation', 'sessions', draftId] }),
  });
}
export function useUnlockSession(draftId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (sessionId) =>
      (await apiClient.post(`${ttBase(draftId)}/sessions/${sessionId}/unlock`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['generation', 'sessions', draftId] }),
  });
}
```
Draft summary + sessions are read via the existing `useDraft` / `useSessions` (A4-345) — not re-implemented (cross-doc consistency).

---

## 7. Placement & Grid Helpers (design) — `lib/`

- **`grid-model.js#buildGrid(sessions, stagedMoves, slotAxis, dayAxis)`** → a `Map` keyed `"{day}|{slotId}"` → session, applying staged overrides over fetched positions; plus `emptyCells(...)` for alternative probing.
- **`placement.js#toPlacement(session, targetCell, resolveDuration)`** → `ProposedPlacementRequest` (`facultyId, roomId, batchId, sectionId, dayOfWeek=targetCell.day, slotDefinitionId=targetCell.slotId, durationMinutes=resolveDuration(targetCell.slotId), sessionId=session.id`). Validated by a small Zod schema mirroring §8 of the requirement (required/positive) before the request; invalid → no request.
- **`placement.js#deriveAlternatives(session, grid, checkFn)`** → probes ≤N empty cells (same day first), returns clean cells ordered by proximity (PD-A15-4).

---

## 8. Components (design)

All interactive; keyboard-operable; ARIA live region announces check results (NFR-2). No `dangerouslySetInnerHTML` (NFR-4).

- **`SessionCard.jsx`** — draggable (HTML5 native DnD — no dnd library needed for a grid drop); shows course/faculty/room/batch/section; visual states: normal, **pending** (check in flight), **conflict** (red border + icon + text, not color-only), **unsaved** (staged move marker), **locked**. `role="button"`, `tabIndex=0`, Enter to pick up (keyboard flow, FR-7).
- **`EditorGrid.jsx`** — day×slot table; each cell is a drop target (`onDragOver/onDrop`) and a keyboard target (arrow-key navigation between cells, Enter to drop the picked-up session). Renders `SessionCard`s from `buildGrid`. Conflicted target cell highlighted.
- **`ConflictPanel.jsx`** — lists full-draft conflicts (`useDraftConflicts`) and the last placement's conflicts; each row shows the friendly `type` label + `description` and focuses the involved cell on click (FR-8).
- **`AlternativesPopover.jsx`** — for a conflicted session, shows `deriveAlternatives` results as clickable candidate slots (staged move on click); labelled "suggested". (AC-3 / OQ-3.)
- **`TimetableEditorPage.jsx`** — orchestrates: `useDraft` + `useSessions` load; builds axes + grid; owns `stagedMoves` reducer, selected session, pending/conflict state, notice banner; wires lock/unlock on pick-up/drop; renders grid + panel; "Unsaved changes" banner; disabled "Save layout" (OQ-1) with explanatory tooltip.

---

## 9. Routing & Navigation

- Route `/scheduling/editor/:draftId` — lazy, in `AppShell` + `Suspense`.
- Entry point: an "Open in editor" action on the A4-345 GenerationPage draft view (passes the current `draftId`). No new top-level nav item (the editor is reached from a specific draft, not a standalone menu), consistent with its draft-scoped nature (UC-3).

---

## 10. State, Accessibility, Security

- **State:** server state via TanStack Query (draft, sessions, draft-conflicts, check mutation); editor UI state (`stagedMoves`, selected session, pending cell, notice) in a page-level `useReducer`. No Zustand (nothing shared cross-route).
- **A11y:** full keyboard placement (FR-7); ARIA live region for "checking…/conflict found: …/placement valid"; conflict indication uses icon+text+color (not color alone); visible focus on cards and cells; `aria-label`s on cells (day + slot).
- **Security:** Zod-validate the placement before sending; `mapApiError` for 400/404/500; never render raw errors; conflict `description` rendered as escaped text.

---

## 11. Error Handling

- Placement check failure (network/400/404) → inline error in the notice region; the attempted move reverts (card returns to origin); no staged change recorded.
- Lock failure (session already locked by another editor) → the pick-up is refused with a message ("another coordinator is editing this session"); this is the interim concurrency guard (OQ-2).
- Draft/sessions load failure → list-level error with retry (reusing the generation feature's pattern).
- All mapped via `mapApiError`; nothing raw surfaced (NFR-4).

---

## 12. Decision-Interaction / Consistency Notes (P6)

- **Staged mode (PD-A15-1) × concurrency (PD-A15-5):** because nothing is persisted this story, there is no true write-conflict; the lock/unlock guard exists to avoid two editors grabbing the same session and to set up the future save path. Consistent — no contradiction, and no false "saved" state (UC-2).
- **REST-first (PD-A15-2) × requirement FR-3.1 (WS primary):** a conscious, documented deviation (OQ-4 approved). The transport is isolated so a later WS upgrade is localized. Flagged as provisional for lead ratification at design approval.
- **Duration fallback (PD-A15-6) × check accuracy:** a wrong duration could change an overlap verdict. Mitigated by resolving from the slot grid first; the default only applies when unresolved, and the backend re-validates. Logged as a backend follow-up (add `durationMinutes` to the session DTO).
- **Deferred conflict types (KD-A15-1):** UI shows only returned conflicts; activating deferred types later needs no UI change. No over-promise.
- **Cross-doc consistency:** reuses A4-345 `useGeneration` hooks and query keys verbatim (`['generation','sessions',draftId,...]`) so the editor and viewer share cache; the new hooks use a distinct `['editor',...]` namespace to avoid collision.

---

## 13. Testing Strategy (design-level; execution gated separately by lead)

- **lib:** `toPlacement` builds a correct request incl. self-exclusion; `buildGrid` overlays staged moves; `deriveAlternatives` returns only clean cells, proximity-ordered, capped.
- **hooks:** `useConflictCheck` posts to the right URL and returns the bare array; lock/unlock hit the right endpoints and invalidate sessions.
- **components:** SessionCard renders conflict/pending/unsaved/locked states; EditorGrid drop triggers a check and reverts on conflict/error (AC-2); keyboard pick-up→move→drop works (AC-6); ConflictPanel focuses the involved cell; AlternativesPopover stages a move on click (AC-3).
- Runner: `node node_modules/vitest/vitest.mjs run <path>`; verify Vite build (new lazy chunk).

---

## 14. Requirement Traceability

| Requirement | Design element |
|-------------|----------------|
| FR-1 (grid view) | EditorGrid, grid-model, PD-A15-7; TimetableEditorPage load |
| FR-2 (drag-drop → check) | SessionCard drag, EditorGrid drop, `toPlacement`, useConflictCheck; self-exclude PD-A15-3 |
| FR-3 (<2s feedback; faculty/room/batch clash) | useConflictCheck (REST-first, PD-A15-2/OQ-4); ARIA live; pending/valid/conflict states; conflict-types.js |
| FR-4 (alternatives) | AlternativesPopover + `deriveAlternatives` (OQ-3/PD-A15-4) |
| FR-5 (save valid) | staged mode PD-A15-1 (OQ-1); disabled Save layout w/ explanation (UC-2) |
| FR-6 (concurrency, 50+) | lock/unlock PD-A15-5 + version hint (OQ-2) |
| FR-7 (keyboard) | SessionCard/EditorGrid keyboard flow; ARIA live |
| FR-8 (full-draft conflicts) | ConflictPanel + useDraftConflicts |
| FR-9 (states/errors) | §11; notice banner; skeletons/empty via reused patterns |
| UC-1/2/3 | contract-driven rendering; unsaved markers; draft-scoped route |
| NFR-1..5 | plain JSX/PropTypes/Zod; a11y §10; REST no-new-dep; mapApiError; reuse conventions |
| AC-1..AC-7 | §13 maps each to a component/lib behavior |
| Conflict-type contract | conflict-types.js, KD-A15-1 |

---

## 15. Open Questions (design)

All requirement OQs are resolved (§2). Provisional items for lead ratification at approval: **PD-A15-2** (REST-first instead of WS-primary) and the two backend follow-ups (**session move endpoint** for real persistence; **`durationMinutes` on the session DTO**). None block building the editor in staged mode.

---

## 16. Out of Scope

- Backend changes (session-move endpoint, version/409 concurrency, `durationMinutes` on session DTO, alternatives engine A4-17) — logged as follow-ups.
- Approval (A4-19), publication (A4-21), conflict-log persistence (A4-18), version diff (A4-20).
- WebSocket/STOMP transport (deferred enhancement, OQ-4).
- Detecting the deferred conflict types (owned by A4-16).
- Unit-test/coverage execution unless the lead elects to perform it.
