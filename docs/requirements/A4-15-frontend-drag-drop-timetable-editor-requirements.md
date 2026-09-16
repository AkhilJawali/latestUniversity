# A4-15 — Frontend: Drag-and-Drop Timetable Editor with Real-Time Conflict Feedback — Requirement Document

| Field | Value |
|-------|-------|
| Story | A4-15 — Drag-and-Drop Timetable Editor with Real-Time Conflict Feedback |
| Requirement Subtask | A4-68 |
| Epic | A4 — UTMS |
| Role / Type | Frontend (React 18 + plain JSX, NO TypeScript) — story is labelled fullstack; see §14/§16 for the backend gaps |
| Consumes backend | A4-16 (real-time conflict detection — REST + WebSocket, Done), A4-11/A4-12 (draft, sessions, violations — Done, already wired by A4-345) |
| Builds on | A4-345 (Frontend Timetable Generation & Draft Viewer, Done) |
| BRD Requirements | 6.2, 6.9, 6.10, Section 8 (≤2s conflict feedback; 50+ concurrent coordinators) |
| Story Points | 8 |

---

## 1. Introduction

This document specifies the **web UI** for a Department Coordinator to visually review and refine an auto-generated draft timetable: a grid-based weekly view where sessions can be repositioned, with real-time conflict feedback (target < 2 seconds) and ranked alternative-slot suggestions. It builds directly on **A4-345** (which already renders a draft's sessions, scores, and violations read-only) and consumes the **A4-16** conflict engine over its verified REST + WebSocket (STOMP) contract.

It is primarily a **frontend** story. Two capabilities the story wording implies — **persisting a moved session** (AC-4 "saved to the draft") and **concurrency-safe editing** (AC-5 "prevent silent overwrites") — have **no backend endpoint today** (verified: `SchedulingController` exposes generate/status/sessions/violations/regenerate/lock/unlock but **no session-move/update and no version/ETag**). These are raised as **blocking Open Questions (OQ-1, OQ-2)**, not invented into requirements.

---

## 2. User Story

*As a* Department Coordinator,
*I want* a visual grid-based timetable editor where I can drag-and-drop sessions to different slots and receive real-time conflict feedback within 2 seconds,
*so that* I can intuitively review and refine the auto-generated draft before submitting for approval.

---

## 3. Actors

- **Department Coordinator** — opens a draft, repositions sessions, reviews conflicts and alternatives. (RBAC not enforced yet — backend endpoints are `permitAll`; the UI assumes an authenticated coordinator — see §15.)

---

## 4. User Journeys

**J1 — Open the editor for a draft.**
1. From the generation/draft viewer (A4-345), the coordinator opens the editor for a `draftId`.
2. The UI loads the draft's placed sessions (`GET /timetables/{draftId}/sessions`) and the current full-draft conflict list (`GET /drafts/{draftId}/conflicts`), and renders a weekly grid (days × time slots) with session cards positioned by their day + slot.

**J2 — Drag a session to a new slot (conflict-free).**
1. The coordinator drags a session card to a target day/slot cell.
2. On drop, the UI sends a placement check (WebSocket `/app/drafts/{draftId}/conflict-check`, REST fallback `POST /drafts/{draftId}/conflict-check`) with the proposed placement (self-excluded via `sessionId`).
3. The engine returns an empty conflict list within the 2-second target; the UI shows the placement as valid and (pending OQ-1) persists it / marks it staged.

**J3 — Drag a session to a conflicting slot.**
1. On drop, the check returns one or more `ConflictDto`s.
2. The UI flags the target cell/card with a conflict indicator within 2 seconds (AC-2) and lists the conflicts (type + human-readable description).
3. The coordinator can view **ranked alternative slots** for the session (AC-3, BRD 6.9 — see OQ-3 for the alternatives source).

**J4 — Keyboard-only placement (accessibility).**
1. A keyboard user selects a session (focus + Enter), navigates to a target cell with arrow keys, and confirms placement with Enter (AC-6).
2. The same conflict check runs; the same feedback is announced to assistive tech.

**J5 — Concurrent editing.**
1. Two coordinators edit the same draft.
2. When one saves a change the other's view is stale, the system must prevent a silent overwrite (AC-5) — **pending a backend concurrency mechanism (OQ-2)**.

**J6 — Review full-draft conflicts.**
1. The coordinator opens a conflicts panel showing all current draft conflicts (`GET /drafts/{draftId}/conflicts`), each linking to the involved session(s) on the grid.

---

## 5. Functional Requirements

### FR-1 — Grid-based weekly editor view
- **FR-1.1** The UI shall render a weekly grid with a **day axis** (working days) and a **time-slot axis** (the campus time-slot grid from A4-10 / A4-445), placing each draft session as a card in its (day, slot) cell (AC-1).
- **FR-1.2** Session cards shall show the essential identity per ui-standards (course, faculty, room, batch/section) using the fields present on the sessions payload (`GET /timetables/{draftId}/sessions`).
- **FR-1.3** Multi-slot sessions (duration spanning multiple periods) shall render across the corresponding rows where slot metadata allows; otherwise the card shows its duration.
- **FR-1.4** Loading uses skeletons; an empty/'no sessions' draft shows an empty state.

### FR-2 — Drag-and-drop repositioning
- **FR-2.1** A session card shall be draggable to another (day, slot) cell.
- **FR-2.2** On drop, the UI shall issue a **placement conflict check** for the target using the A4-16 contract (§12), passing `facultyId, roomId, batchId, sectionId?, dayOfWeek, slotDefinitionId, durationMinutes, sessionId` (the moved session's id, so it is self-excluded).
- **FR-2.3** While the check is in flight, the UI shall show a pending indicator on the affected card and shall not present the move as confirmed.

### FR-3 — Real-time conflict feedback (< 2s target)
- **FR-3.1** The UI shall use the **WebSocket** channel as the primary transport: send to `/app/drafts/{draftId}/conflict-check`, subscribe to `/topic/drafts/{draftId}/conflicts` for the result (§12).
- **FR-3.2** If the WebSocket is unavailable, the UI shall fall back to the REST endpoint `POST /drafts/{draftId}/conflict-check`.
- **FR-3.3** On a non-empty conflict list, the UI shall flag the target cell/card with a conflict indicator and render each conflict's `type` (mapped to a friendly label) and `description` (AC-2 — the AC names faculty/room/batch clash specifically; these are the actively-detected types per §7). The 2-second target (BRD Section 8) is a backend SLA the UI surfaces; the UI adds no artificial delay.
- **FR-3.4** On an empty conflict list, the UI shall present the placement as valid (AC-4 — persistence pending OQ-1).

### FR-4 — Alternative slot suggestions
- **FR-4.1** When a conflict is flagged, the UI shall offer **ranked alternative slots** for the session (AC-3, BRD 6.9). The source of alternatives is **OQ-3** — A4-17 (Alternative Slot Suggestions) is a separate open story and no alternatives endpoint is verified yet. Until it lands, the UI may derive candidate slots client-side by probing empty cells with the same conflict-check (advisory) — confirm in OQ-3.

### FR-5 — Save a valid placement
- **FR-5.1** On a conflict-free drop, the UI shall persist the new placement to the draft (AC-4). **There is no session-move/update endpoint today (verified) — OQ-1.** Until it exists, the editor operates in a **staged (client-side) mode**: moves are held in view state and clearly marked as unsaved, with no false claim of persistence.

### FR-6 — Concurrency safety
- **FR-6.1** The UI shall prevent silent overwrites when two coordinators edit the same draft (AC-5). **No backend concurrency mechanism (version/ETag/lock-on-move) is verified — OQ-2.** The existing per-session `lock`/`unlock` endpoints (`POST /timetables/{draftId}/sessions/{sessionId}/lock|unlock`) may be leveraged; confirm in OQ-2.

### FR-7 — Keyboard-accessible placement
- **FR-7.1** The UI shall provide a keyboard-only alternative to drag-and-drop: select a session, move focus across cells with arrow keys, and confirm placement with Enter (AC-6, NFR-2). The same conflict check and feedback apply, announced via ARIA live regions.

### FR-8 — Full-draft conflict panel
- **FR-8.1** The UI shall show the current full-draft conflict list (`GET /drafts/{draftId}/conflicts`), each entry linking/scrolling to the involved session(s) on the grid (J6).

### FR-9 — Cross-cutting UI behaviors
- **FR-9.1** Skeletons on load; empty states; a persistent error region for check/transport failures.
- **FR-9.2** All destructive or ambiguous actions confirmed; pending states disable the relevant control.
- **FR-9.3** No raw API errors/stack traces surfaced; all mapped via the shared `mapApiError`.

---

## 6. Constraints Owned by This Document (UI-level only)

- **UC-1 (Advisory client feedback):** the UI renders whatever conflicts the A4-16 engine returns; it does not compute its own conflict verdicts. The engine is authoritative.
- **UC-2 (No false persistence):** the UI must never present a move as saved unless a backend save actually succeeded (ties to OQ-1). In staged mode, unsaved moves are explicitly marked.
- **UC-3 (Draft-scoped):** all editor operations are scoped to a single `draftId`.

---

## 7. Constraints Referenced from Other Documents

- **A4-16 (owner of conflict types):** `ConflictType` enum {FACULTY_DOUBLE_BOOKING, ROOM_DOUBLE_BOOKING, BATCH_CLASH, ROOM_CAPACITY, FACULTY_DAILY_HOURS, FACULTY_WEEKLY_HOURS, FACULTY_CONSECUTIVE_HOURS, ROOM_HARD_BLOCK, FACULTY_HARD_BLOCK, TRAVEL_TIME, PREREQUISITE_SEQUENCE}. **Actively detected today:** faculty double-booking, room double-booking, batch clash, room capacity, and (when faculty limits supplied) daily / weekly / consecutive hours (these three are distinct types). **Defined but detection DEFERRED:** ROOM_HARD_BLOCK, FACULTY_HARD_BLOCK, TRAVEL_TIME, PREREQUISITE_SEQUENCE. The UI must render all types it receives but must not promise conflicts the engine does not yet raise (§14).
- **A4-11/A4-12 (draft/sessions/violations):** consumed read-only, already wired by A4-345 (`useGeneration` hooks).
- **A4-10 / A4-445 (time-slot grid):** supplies the slot axis for the grid.

---

## 8. Validation Rules (client-side, mirroring A4-16 `ProposedPlacementRequest`)

| Field | Rule | Source |
|-------|------|--------|
| facultyId | required, positive | `ProposedPlacementRequest.facultyId` |
| roomId | required, positive | `roomId` |
| batchId | required, positive | `batchId` |
| sectionId | optional | `sectionId` |
| dayOfWeek | required (non-blank) | `dayOfWeek` |
| slotDefinitionId | required, positive | `slotDefinitionId` |
| durationMinutes | required, positive | `durationMinutes` |
| sessionId | optional (the moved session, self-excluded from occupancy) | `sessionId` |

The UI derives these from the dragged session + target cell; it does not free-type them. The backend re-validates (400 on malformed).

---

## 9. Non-Functional Requirements

- **NFR-1 (No TypeScript):** all files `.jsx`/`.js`; PropTypes; Zod for any client validation. Hard rule.
- **NFR-2 (Accessibility, WCAG AA):** keyboard placement alternative (FR-7); ARIA live announcements for conflict results; visible focus; conflict color indicators meet contrast and are not color-only (icon/text too).
- **NFR-3 (Performance):** route-based lazy loading; the < 2s conflict SLA is the backend's (BRD Section 8); the UI adds no artificial latency and updates optimistically on the pending/valid states.
- **NFR-4 (Security):** no `dangerouslySetInnerHTML`; never render raw engine errors; WebSocket payloads treated as untrusted data.
- **NFR-5 (Conventions):** reuse the established feature layout, the shared table/modal/confirm components where applicable, `mapApiError`, `apiClient`, TanStack Query, and the existing A4-345 generation hooks. A WebSocket/STOMP client dependency is introduced for FR-3 (the first in the codebase — call out in design).

---

## 10. Acceptance Criteria (Given / When / Then)

1. **Grid view (AC-1):** *Given* a draft, *When* the editor opens, *Then* sessions render in a weekly grid with day/time axes.
2. **Conflict on drop (AC-2):** *Given* a session dragged to a slot that clashes (faculty/room/batch), *When* dropped, *Then* a conflict indicator appears within the 2-second target and the conflict is described.
3. **Alternatives (AC-3):** *Given* a flagged conflict, *When* the coordinator views details, *Then* ranked alternative slots are shown (source per OQ-3).
4. **Valid drop saved (AC-4):** *Given* a conflict-free drop, *When* placed, *Then* it is saved to the draft — **pending OQ-1**; in staged mode it is retained and clearly marked unsaved.
5. **Concurrency (AC-5):** *Given* concurrent coordinators, *When* conflicting edits occur, *Then* silent overwrites are prevented — **pending OQ-2**.
6. **Keyboard alternative (AC-6):** *Given* a keyboard-only user, *When* they place a session via keyboard, *Then* placement works with the same conflict feedback, announced accessibly.
7. **Transport fallback:** *Given* the WebSocket is unavailable, *When* a check is needed, *Then* the UI falls back to the REST conflict-check and still shows feedback.

---

## 11. Data Model (conceptual — display/consume only)

- **ConflictDto** (from A4-16): `type` (ConflictType), `involvedFacultyId`, `involvedRoomId`, `involvedBatchId`, `involvedSectionId`, `involvedSessionId`, `dayOfWeek`, `slotDefinitionId`, `description`. Transient (not persisted).
- **ProposedPlacementRequest** (to A4-16): see §8.
- **Session** (from A4-11 `GET /timetables/{draftId}/sessions`): consumed as A4-345 already shapes it (course/faculty/room/batch/section, day, slot, duration).

---

## 12. Dependencies / Verified Backend Contract

- **A4-16 conflict engine (Done):**
  - REST: `POST /api/v1/drafts/{draftId}/conflict-check` (body `ProposedPlacementRequest`) → `200 [ConflictDto]`; `GET /api/v1/drafts/{draftId}/conflicts` → `200 [ConflictDto]`. (Returns a bare list, **not** the `{data:[...]}` envelope — verified in `ConflictController`.)
  - WebSocket (STOMP): send `/app/drafts/{draftId}/conflict-check` (payload `ProposedPlacementRequest`), subscribe `/topic/drafts/{draftId}/conflicts` → `[ConflictDto]`.
- **A4-11/A4-12 (Done, wired by A4-345):** `GET /timetables/{draftId}`, `/sessions`, `/violations`, `/unplaced`; `POST /timetables/{draftId}/sessions/{sessionId}/lock|unlock`.
- **Shared infra:** `@/lib/api-client`, `@/lib/api-error`, TanStack Query, `AppShell`, router; A4-345 `useGeneration` hooks.
- **NEW dependency:** a STOMP/WebSocket client library for FR-3 (design to specify).

---

## 13. Assumptions

1. The editor is entered with a known `draftId` (from A4-345).
2. The sessions payload carries enough placement data (day, slot, faculty, room, batch) to position cards and build a `ProposedPlacementRequest`.
3. The STOMP endpoint is reachable at the app's WebSocket base (design to confirm the URL/handshake).
4. Coordinator is authenticated; RBAC deferred (§15).

---

## 14. Consistency Notes (contradiction check)

- **"Real-time feedback" vs. deferred conflict types.** A4-16 defines 11 conflict types but actively detects only faculty double-booking, room double-booking, batch clash, room capacity, and (conditionally) daily/weekly/consecutive faculty hours. ROOM_HARD_BLOCK, FACULTY_HARD_BLOCK, TRAVEL_TIME, PREREQUISITE_SEQUENCE are **defined but not raised** yet. The UI renders whatever it receives and must not imply detection of the deferred types (no false assurance). No UI change is needed when they later activate.
- **AC-4 "saved to the draft" vs. no save endpoint.** Verified: no session-move/update endpoint exists. Reconciled by FR-5.1 staged mode + **OQ-1**; the UI will not claim persistence it cannot perform (UC-2).
- **AC-5 "prevent silent overwrites" vs. no concurrency mechanism.** Verified: no version/ETag; only per-session lock/unlock. Reconciled by **OQ-2**.
- **AC-3 alternatives vs. no alternatives endpoint.** A4-17 is a separate open story; **OQ-3** governs the source.
- **Response envelope difference.** The conflict endpoints return a bare list (not `{data:[...]}`) — the hooks must read the array directly, unlike the master-data features.

---

## 15. Out of Scope

- Any backend/API/schema change (owned by A4-16 for conflicts; a new save/concurrency endpoint would be a scheduling-module story — see OQ-1/OQ-2).
- The alternative-slot **engine** (A4-17), conflict-log persistence (A4-18), approval (A4-19), publication (A4-21).
- RBAC/login; generation triggering and read-only draft display (already delivered by A4-345).
- Detecting the deferred conflict types (owned by A4-16 when it activates them).

---

## 16. Open Questions

| # | Question | Why it matters | Proposed default (pending confirmation) |
|---|----------|----------------|------------------------------------------|
| **OQ-1 (blocking for AC-4)** | There is **no backend endpoint to persist a moved session** (no PUT/PATCH on `/timetables/{draftId}/sessions/{sessionId}`; only lock/unlock). How should a valid drop be saved? | AC-4 cannot be truly met without it. | (a) **Staged client-side mode** for this story (moves held in view state, clearly unsaved), and a backend follow-up to add `PATCH /timetables/{draftId}/sessions/{sessionId}` (move). Alternatively (b) block A4-15 on that backend story. Proposed: (a). |
| **OQ-2 (blocking for AC-5)** | No optimistic-concurrency mechanism (no version/ETag on draft or session). How to prevent silent overwrites for 50+ concurrent coordinators? | AC-5 depends on it. | Backend follow-up to add a version/ETag on session or draft (reject stale writes with 409). Interim: use the existing per-session **lock/unlock** to serialize edits, and/or single-editor advisory. Confirm. |
| **OQ-3** | Where do **ranked alternative slots** (AC-3, BRD 6.9) come from? A4-17 is a separate open story; no alternatives endpoint verified. | Determines whether the UI calls an endpoint or derives candidates client-side. | Until A4-17 lands, derive candidate empty slots client-side and rank by running the same conflict-check (advisory, unranked-by-engine). Confirm — or defer AC-3 to when A4-17 is ready. |
| **OQ-4** | STOMP/WebSocket transport: confirm the endpoint URL/handshake and auth, and the STOMP client library to add (first WebSocket use in the FE). | FR-3 primary transport. | Use `@stomp/stompjs` against the app's `/ws` (or configured) endpoint; REST fallback per FR-3.2. Confirm the base URL. |
| **OQ-5** | Does the sessions payload include `slotDefinitionId`, `dayOfWeek`, `durationMinutes`, `facultyId`, `roomId`, `batchId`, `sectionId` needed to build `ProposedPlacementRequest`? | The check cannot be built without these fields. | Assume yes (A4-345 renders sessions); design verifies exact field names and maps them. |
| **OQ-6** | Feedback pattern: reuse the inline status/banner approach (as in recent FE stories), no new toast system? | Consistency. | Inline regions + ARIA live for conflict results. Confirm. |

---

## 17. Traceability

| BRD / Story AC | Covered by |
|----------------|-----------|
| BRD 6.2 / AC-1 (grid editor view) | FR-1 |
| BRD 6.9 / AC-2 (real-time conflict flag ≤2s; faculty/room/batch clash) | FR-2, FR-3 |
| BRD 6.9 / AC-3 (ranked alternatives) | FR-4 (source per OQ-3) |
| BRD 6.10 / AC-4 (valid drop saved) | FR-5 (**pending OQ-1**) |
| BRD Section 8 / AC-5 (concurrency, 50+) | FR-6 (**pending OQ-2**) |
| BRD Section 8 / AC-6 (keyboard alternative) | FR-7 |
| Transport resilience | FR-3.2, AC-7 |
| Full-draft conflict review | FR-8 |
| Conflict-type contract (A4-16) | §7, §11, §14 |
