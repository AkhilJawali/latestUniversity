# A4-430 — Frontend Room and Lab Master Data (CRUD): Requirement Document

- **Story:** A4-430 — Frontend Room and Lab Master Data (CRUD)
- **Requirement Generation subtask:** A4-431
- **Consumes backend:** A4-6 (Room and Lab Master Data Management) — `/api/v1/rooms`
- **BRD:** 6.1 (room/lab master: capacity, room type, equipment tags, campus/building/floor), 7.3 (room capacity ≥ batch/group strength; room type & equipment)
- **Date:** 2026-09-04

## 1. Introduction

This document specifies the UI-only requirements for managing rooms and labs: listing/filtering rooms, creating and editing a room (capacity, type, equipment tags, campus/building/floor), and soft-deleting. It is grounded in the verified A4-6 REST contract; a few story assumptions the backend does not support (free-text room search, a building picker backed by master data, multi-tag equipment filtering) are raised as Open Questions (Section 16) rather than invented.

## 2. User Story

*As a* System Administrator, *I want* a web UI to manage rooms and labs — capacity, type, equipment tags, and building/floor/campus location — *so that* the scheduling engine can allocate appropriate spaces and I can maintain room data without database edits.

## 3. Actors

- **System Administrator** — creates, edits, and soft-deletes rooms. (Backend RBAC is not enforced yet — endpoints are `permitAll` with `// TODO` role annotations; this UI does not gate by role.)

## 4. User Journeys

1. **Browse & filter rooms.** Admin opens the Rooms page → sees a paginated table (name, code, campus, capacity, type, equipment tags, building, floor) → filters by campus, type, building, minimum capacity, and/or a single equipment tag → the table updates.
2. **Create a room.** Admin clicks "Add room" → fills name, code, campus, capacity, type, optional equipment tags, optional building/floor → submits → the room is created and listed.
3. **Edit a room.** Admin edits a room → changes name, capacity, type, equipment tags, building, floor → submits. Code and campus are NOT editable (immutable).
4. **Soft-delete a room.** Admin deletes a room → confirms → the record is soft-deleted and leaves the active list.

## 5. Functional Requirements

### FR-1 — Room list and filtering
- **FR-1.1** The page lists rooms from `GET /api/v1/rooms` (paginated `{data, meta}`), showing: name, code, campus name, capacity, room type, equipment tags, building, floor.
- **FR-1.2** The list supports filtering by **campus** (`campusId`), **room type** (`roomType`), **building** (`building`, exact match), **minimum capacity** (`minCapacity`), and a single **equipment tag** (`equipmentTag`, exact member match).
- **FR-1.3** Pagination controls reflect `meta` (page, size, totalElements, totalPages).
- **FR-1.4** Loading skeleton, empty state + "Add room" CTA, and list-error retry.

### FR-2 — Create room
- **FR-2.1** A create form collects: name (required, ≤200), code (required, ≤20, alphanumeric with `-`/`_`), campus (required, chosen from campuses), capacity (required, ≥1), room type (required — one of the fixed values, FR-6), equipment tags (optional, multi-value), building (optional, ≤100), floor (optional, ≤20).
- **FR-2.2** Client-side Zod validation mirrors the A4-6 bounds; an invalid submit shows inline field errors and sends **no** request (AC-4).
- **FR-2.3** On success (201), the form closes and the new room appears; on backend error (e.g. 409 duplicate code within campus, 400 validation), the mapped message/field errors are surfaced.

### FR-3 — Edit room
- **FR-3.1** An edit form collects only the fields `PUT /api/v1/rooms/{id}` accepts: name, capacity, room type, equipment tags, building, floor.
- **FR-3.2** Code and campus are **not** editable (both immutable on update) and are shown read-only for context.
- **FR-3.3** Same validation and error handling as create.

### FR-4 — Equipment tags
- **FR-4.1** Equipment tags are entered as a multi-value list on the create/edit form (free-text tags; the backend stores them as a string list with no fixed vocabulary).
- **FR-4.2** The list filter accepts a **single** equipment tag (exact match) per FR-1.2 (see OQ-3 for multi-tag).

### FR-5 — Soft-delete
- **FR-5.1** Deleting a room calls `DELETE /api/v1/rooms/{id}` (204) behind a confirmation dialog.
- **FR-5.2** On success the record leaves the active list; on failure the reason is surfaced and the dialog stays open.

### FR-6 — Room type options
- **FR-6.1** Room type is chosen from the fixed backend enum: `CLASSROOM`, `LAB`, `SEMINAR_HALL`, `AUDITORIUM` (matching the story's classroom / lab / seminar hall / auditorium). Mirrored as a frontend constant (no lookup endpoint).

## 6. Constraints Owned by This Document

- Client-side form validation for room create/edit (mirrors A4-6 bounds). Backend re-validates authoritatively.
- Client-side presentation of the fixed room-type list.

## 7. Constraints Referenced from Other Documents

- **A4-6 (backend):** name ≤200; code required, ≤20, regex `^[A-Za-z0-9_-]+$`, unique within campus (409); campusId required; capacity ≥1; roomType ∈ {CLASSROOM, LAB, SEMINAR_HALL, AUDITORIUM}; equipmentTags optional string list; building ≤100; floor ≤20; code + campusId immutable on update.
- **A4-2 / A4-410 (campus hierarchy):** used to populate the campus filter and the create-form campus picker.

## 8. Validation Rules (client-side)

| Field | Rule |
|-------|------|
| name | required, ≤200 chars |
| code | required, ≤20 chars, `^[A-Za-z0-9_-]+$` (create only) |
| campusId | required, positive (create only) |
| capacity | required, integer ≥1 |
| roomType | required, one of the 4 fixed values |
| equipmentTags | optional, list of strings |
| building | optional, ≤100 chars |
| floor | optional, ≤20 chars |

## 9. Non-Functional Requirements

- **NFR-1 (security/XSS):** dynamic text as escaped JSX; no `dangerouslySetInnerHTML`.
- **NFR-2 (a11y):** labels on all controls, `aria-label` on icon-only buttons, keyboard-accessible dialogs (native `<dialog>`), inline errors announced.
- **NFR-3 (performance):** route-based code splitting (lazy route); list uses server pagination.
- **NFR-4 (standards):** plain JSX (no TypeScript), PropTypes, enforced import order; reuse the shared table/modal/confirm components + `validateWith` / `mapApiError`.

## 10. Acceptance Criteria

- **AC-1** *Given* the create-room form, *When* capacity, type, equipment tags, and building/floor/campus are submitted, *Then* the room is created (201) and listed.
- **AC-2** *Given* the equipment-tag filter set to "computer_lab", *When* applied, *Then* only rooms with that tag are listed (via `equipmentTag`).
- **AC-3** *Given* an edit to a room's capacity, *When* saved, *Then* the updated capacity is shown on the record.
- **AC-4** *Given* a required field left blank or an invalid capacity (e.g., 0), *When* the form is submitted, *Then* an inline validation error is shown and no request is sent.
- **AC-5** *Given* rooms across campuses, *When* the campus filter is applied, *Then* only that campus's rooms are listed (via `campusId`).

## 11. Data Model (conceptual, frontend view)

- **Room** (RoomDto): id, name, code, campusId, campusName, capacity, roomType, equipmentTags[], building, floor, createdAt, updatedAt. (No `isActive` exposed — see OQ-4.)

## 12. Dependencies

- A4-6 backend running (`/api/v1/rooms`).
- A4-2 / A4-410 campus list (campus filter + create-form campus picker).
- Reused frontend components (table/modal/confirm) + `validateWith` / `mapApiError`, per the A4-410/415/420/425 precedent.

## 13. Assumptions

1. The 4 room-type values are stable enough to mirror in the frontend (no lookup endpoint).
2. "Equipment tags" are free-text on create/edit; the tag filter is single-tag exact match (OQ-3).
3. Building/floor are free-text inputs (no building master data — OQ-2).

## 14. Consistency Notes

- The story mentions room list "search"; the backend list endpoint has **no** free-text search param (only campus/type/building/minCapacity/equipmentTag filters). Reconciled: the UI offers those filters, not a free-text search box (OQ-1). No FR promises free-text search.
- The story says "building/floor/campus" location; campus is a real FK (dropdown), but **building and floor are free-text** — there is no building picker backed by master data (OQ-2).
- The story's equipment-tag filter maps to the single-tag exact-match backend param (OQ-3).
- The story mentions "toasts"; feedback is surfaced via inline message regions consistent with the reused components (no toast system is built here).

## 15. Out of Scope

- Backend/API/schema change (owned by A4-6).
- Schedulable assets (A4-435 / A4-7 frontend).
- RBAC / role gating; scheduling / allocation logic.

## 16. Open Questions

- **OQ-1:** The story mentions room "search". The backend list endpoint has no free-text/name/code search param (only campus/type/building/minCapacity/equipmentTag). Confirm the filter set is sufficient, or raise a backend follow-up for a `?search=` param.
- **OQ-2:** The story says "building/floor/campus". Campus is a real FK (dropdown), but **building and floor are free-text strings** — there is no Building master-data resource for a picker. Proposed: free-text inputs for building/floor, plus a building filter driven by distinct values already present in the loaded rooms. Confirm, or raise a backend follow-up for a Building resource.
- **OQ-3:** The equipment-tag filter is **single-tag, exact, case-sensitive** (`equipmentTag` param). If multi-tag or contains/case-insensitive filtering is expected, that's a backend follow-up. Proposed: single-tag filter for this story.
- **OQ-4:** `RoomDto` does not expose `isActive`. If the table should show an active/inactive column, that's a backend follow-up. Proposed: omit the column (only active rooms are returned anyway).

## 17. Traceability

| BRD / Story | Requirement |
|-------------|-------------|
| BRD 6.1 (capacity, type, equipment tags, campus/building/floor) | FR-2, FR-3, FR-4, FR-6 |
| BRD 7.3 (room capacity, room type & equipment) | FR-2.1 (capacity ≥1), FR-4, FR-6 |
| Story AC-1 | FR-2, AC-1 |
| Story AC-2 (equipment-tag filter) | FR-1.2 / FR-4.2, AC-2 |
| Story AC-3 (edit capacity) | FR-3, AC-3 |
| Story AC-4 (validation) | FR-2.2, AC-4 |
| Story AC-5 (campus filter) | FR-1.2, AC-5 |
| Story "search" | OQ-1 (no backend free-text search) |
| Story "building" picker | OQ-2 (free-text; no building master data) |
