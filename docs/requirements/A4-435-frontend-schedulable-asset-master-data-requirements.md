# A4-435 — Frontend: Schedulable Asset Master Data (CRUD) — Requirement Document

| | |
|---|---|
| **Issue** | A4-435 (Story) |
| **Epic** | A4-1 — University Timetable Management System (UTMS) |
| **Requirement Generation subtask** | A4-436 |
| **Consumes backend** | A4-7 — Schedulable Asset Master Data Management (`/api/v1/assets`, implemented) |
| **Also consumes** | A4-2 — Campus Hierarchy (`/api/v1/campuses`, `/api/v1/departments`) for the department/campus pickers |
| **BRD** | 6.1 — "Asset / schedulable-resource master: non-room schedulable assets with owning department, campus, and an availability/blocking calendar per resource"; 7.3 — "Asset-level blocking scope" |
| **Role** | frontend |
| **Tech** | React 18 (plain JSX, no TypeScript), React Router v6, TanStack Query, Zustand, Zod, Axios |
| **Date** | 2026-09-09 |

---

## 1. Introduction

This document specifies the **UI-only** requirements for managing non-room schedulable assets (equipment kits, projector sets, sports facilities) with an owning department, campus, and a per-asset availability calendar. It is grounded in the **verified A4-7 REST contract** (read directly from `AssetController`, `CreateAssetRequest`, `UpdateAssetRequest`, `CreateAvailabilityWindowRequest`, `AssetDto`, `AssetAvailabilityWindowDto`, and `AssetService`). All data, validation, and persistence are owned by A4-7; this story consumes those APIs and presents them. It defines no API, schema, or business rule.

Several points in the story wording diverge from what the backend actually exposes. Rather than invent behavior, those divergences are captured as **Open Questions** (Section 16) and reconciled explicitly in **Consistency Notes** (Section 14). The most material ones:

- The backend requires an **`identifier`** field on create (the story does not mention it).
- Availability windows have **no standalone endpoints** — they are managed as a full list embedded in asset create/update (whole-list replacement), so "create/edit/delete per window" is realized as editing the asset's window list, not per-window API calls.
- The list endpoint has **no free-text `search` param** and returns **no computed "availability status"** field.
- Update **cannot** change `identifier`, `owningDepartment`, or `campus` — only `name`, `assetType`, and the window list.
- The window model has only `dayOfWeek/startTime/endTime` — there is **no "maintenance period" concept**.

## 2. User Story

*As a* System Administrator, *I want* a web UI to manage non-room schedulable assets with owning department, campus, and a per-asset availability calendar, *so that* the full inventory of schedulable resources is maintainable through the application.

## 3. Actors

- **System Administrator** — creates, lists, edits, and soft-deletes assets and edits their availability windows. (Backend RBAC is not enforced yet — the A4-7 endpoints carry `// TODO @PreAuthorize("hasRole('ADMIN')")` and are effectively `permitAll`; this UI does not gate by role.)

## 4. User Journeys

1. **Browse assets.** Admin opens the Assets page → a paginated table lists assets (name, identifier, type, owning department, campus). Filters by campus, department, and type narrow the list.
2. **Create an asset.** Admin clicks "Add" → fills name, identifier, type, owning department, campus, and optionally adds availability windows → submits → the asset is created (201) and appears in the list. A duplicate identifier is rejected (409) with a readable message.
3. **Edit an asset.** Admin opens an existing asset → changes name/type and edits its availability windows → saves. Identifier, owning department, and campus are shown read-only (backend `UpdateAssetRequest` does not accept them — OQ-4).
4. **Manage availability windows.** Within the asset form, the admin adds / edits / removes rows in the window list (day, start, end). On save the backend replaces the asset's entire window set with the submitted list.
5. **Delete an asset.** Admin triggers delete → confirms → the asset is soft-deleted (204) and disappears from the list. If the backend later rejects deletion due to active references (A4-8, not yet wired), the error is surfaced and the asset remains.
6. **Handle empty / error states.** No assets → empty state with an "Add" action. API/validation failures → readable inline errors or message region; raw errors/stack traces are never shown.

## 5. Functional Requirements

### FR-1 — Asset list
- **FR-1.1** List assets from `GET /api/v1/assets` using the standard `{ data, meta }` envelope (`meta` = page, size, totalElements, totalPages).
- **FR-1.2** The table SHALL show: name, identifier, assetType, owningDepartmentName, campusName. (These are the fields `AssetDto` exposes.)
- **FR-1.3** Pagination SHALL use the backend `Pageable` contract (`page`, `size`, `sort`); default size follows the UTMS API standard (20, max 100) — not an invented value.
- **FR-1.4** Filters SHALL be provided for **campus** (`campusId`), **department** (`departmentId`), and **asset type** (`assetType`) — the exact query params the backend supports. Applying a filter reloads the matching page (AC-3).
- **FR-1.5** Loading SHALL show skeleton placeholders; an empty result SHALL show an empty state with a primary "Add" action.
- **FR-1.6** The story's "availability status" column is **not backed by any backend field** (`AssetDto` has no status). See OQ-3 — either omit the column or derive a client-side indicator (e.g., "has N windows"). No FR promises a server-computed availability status.
- **FR-1.7** The story's free-text "search" is **not supported** by the list endpoint (no search param). See OQ-2. No FR promises free-text search.

### FR-2 — Create asset
- **FR-2.1** A create form (modal/panel) SHALL capture the fields the backend requires on `CreateAssetRequest`: **name** (required, ≤200), **identifier** (required, ≤50, pattern `^[A-Za-z0-9_-]+$`), **assetType** (required, ≤50), **owning department** (required — picked from the A4-2 departments list), **campus** (required — picked from the A4-2 campuses list).
- **FR-2.2** The form MAY include an initial set of availability windows (FR-4), submitted inline in the same `POST` (the backend accepts `availabilityWindows` on create).
- **FR-2.3** Inputs SHALL be validated client-side with Zod before submit (FR-3); the backend re-validates authoritatively.
- **FR-2.4** On success the asset SHALL be created via `POST /api/v1/assets` (201) and the list SHALL refresh to include it without a full page reload (AC-1).
- **FR-2.5** On a duplicate identifier the UI SHALL surface the backend **409** as a readable message and SHALL NOT create a duplicate.
- **FR-2.6** If the selected department or campus does not exist server-side, the backend **404** SHALL be surfaced readably.

### FR-3 — Client-side validation (Zod)
- **FR-3.1** name: required, ≤200 chars.
- **FR-3.2** identifier: required, ≤50 chars, matches `^[A-Za-z0-9_-]+$` (mirrors the backend `@Pattern`).
- **FR-3.3** assetType: required, ≤50 chars.
- **FR-3.4** owningDepartmentId: required (a department must be selected).
- **FR-3.5** campusId: required (a campus must be selected).
- **FR-3.6** For each availability window: dayOfWeek required and ∈ {MONDAY…SUNDAY}; startTime required; endTime required; endTime **strictly after** startTime (client-side rule — the backend `CreateAvailabilityWindowRequest` enforces only `@NotNull`/day-pattern and does **not** enforce end-after-start; see OQ-6).
- **FR-3.7** A required field left blank (or an invalid pattern) SHALL show an inline error and send **no** request (AC-4).

### FR-4 — Availability windows (per-asset, list-based)
- **FR-4.1** Within the asset create/edit form, a windows sub-section SHALL let the admin **add**, **edit**, and **remove** window rows, each with: day of week (MONDAY…SUNDAY), start time, end time.
- **FR-4.2** These edits operate on the **in-form list**; there are **no per-window endpoints**. On save the full window list is sent as `availabilityWindows` in the asset `POST`/`PUT`, and the backend **replaces the asset's entire window set** with it (verified in `AssetService.update` → `clear()` then re-add). Removing a row and saving deletes that window; adding a row and saving creates it (AC-2).
- **FR-4.3** Existing windows SHALL be loaded from `AssetDto.availabilityWindows` (`GET /assets/{id}`) when opening the edit form.
- **FR-4.4** The windows sub-section SHALL show an empty state when the asset has no windows, plus an "Add window" action.
- **FR-4.5** The story's "maintenance periods" concept is **not modeled** by the backend (windows carry only day/start/end). See OQ-5. No FR promises maintenance periods.

### FR-5 — Edit asset
- **FR-5.1** An edit form SHALL pre-fill current values from `GET /assets/{id}`.
- **FR-5.2** Editable fields SHALL be exactly what `UpdateAssetRequest` accepts: **name**, **assetType**, and the **availabilityWindows** list.
- **FR-5.3** **identifier**, **owning department**, and **campus** SHALL be displayed **read-only** in edit (the backend update contract does not accept them — OQ-4).
- **FR-5.4** On success (`PUT /assets/{id}`, 200) the list/detail SHALL reflect updated values.

### FR-6 — Delete asset (soft-delete)
- **FR-6.1** Delete SHALL be behind a confirmation dialog.
- **FR-6.2** On confirm, call `DELETE /assets/{id}` (204); on success the asset disappears from the list (AC-5).
- **FR-6.3** If the backend rejects deletion with a business-rule error (future A4-8 active-reference check, **422** per `BusinessRuleViolationException`), the message SHALL be surfaced and the asset SHALL remain. (Currently the backend never blocks — the reference check is a `// TODO` pending A4-8.)

## 6. Constraints Owned by This Document

- **Client-side validation** for the asset form (name/identifier/assetType/department/campus) and for each availability window (day allowlist, both times required, end-after-start). The backend re-validates the subset it enforces authoritatively.
- **Client-side presentation lists**: the 7 day-of-week names (mirroring the backend pattern — OQ-7) and, if adopted, a curated `assetType` option list (OQ-1).
- **In-form window list semantics**: add/edit/remove rows locally, then submit the full list for whole-set replacement on save (FR-4.2).

## 7. Constraints Referenced from Other Documents

- **A4-7 (backend):** create fields — name (≤200), identifier (≤50, `^[A-Za-z0-9_-]+$`, unique among active → 409), assetType (≤50), owningDepartmentId (must exist → 404), campusId (must exist → 404), optional `availabilityWindows`; window fields — dayOfWeek ∈ {MONDAY…SUNDAY}, startTime, endTime (LocalTime); update accepts only name/assetType/availabilityWindows and **replaces** the window set; delete is soft (204); list filters — campusId, departmentId, assetType + Pageable.
- **A4-2 (backend):** `GET /api/v1/campuses` and `GET /api/v1/departments` provide the campus and department options for the pickers and filters.

## 8. Validation Rules (client-side)

| Field | Rule |
|-------|------|
| name | required, ≤200 chars |
| identifier | required, ≤50 chars, matches `^[A-Za-z0-9_-]+$` |
| assetType | required, ≤50 chars |
| owningDepartmentId | required (selected from A4-2 departments) |
| campusId | required (selected from A4-2 campuses) |
| window.dayOfWeek | required, one of MONDAY…SUNDAY |
| window.startTime | required (HH:mm) |
| window.endTime | required (HH:mm), strictly after startTime |

## 9. Non-Functional Requirements

- **NFR-1 (security/XSS):** all dynamic text rendered as escaped JSX; no `dangerouslySetInnerHTML` (org standard).
- **NFR-2 (a11y):** labels on all controls; `aria-label` on icon-only buttons; keyboard-accessible confirm/edit dialogs; inline errors announced.
- **NFR-3 (performance):** route-based code splitting (lazy route); list uses server pagination (page size ≤100); dropdown option loads (departments/campuses) cached via TanStack Query.
- **NFR-4 (standards):** plain JSX (no TypeScript), PropTypes for props, enforced import order; reuse shared table/modal/confirm components and the shared `validateWith` / `mapApiError` helpers established in A4-410/415/420/425.

## 10. Acceptance Criteria

- **AC-1** *Given* the create-asset form, *When* name, identifier, type, owning department, and campus are submitted, *Then* the asset is created (201) and listed.
- **AC-2** *Given* an asset being edited, *When* an availability window is added and saved, *Then* it is persisted (via the asset `PUT` window list) and shown under that asset on reload.
- **AC-3** *Given* the type or campus filter, *When* applied, *Then* only matching assets are listed.
- **AC-4** *Given* a required field left blank (or an identifier with invalid characters), *When* the form is submitted, *Then* an inline validation error is shown and no request is sent.
- **AC-5** *Given* an existing asset, *When* deleted with confirmation, *Then* it is removed from the list (204).
- **AC-6** *Given* a create submit with an identifier that already exists, *When* the backend returns 409, *Then* a readable "identifier already exists" message is shown and no duplicate is created.

## 11. Data Model (conceptual, frontend view)

- **Asset** (`AssetDto`): id, name, identifier, assetType, owningDepartmentId, owningDepartmentName, campusId, campusName, availabilityWindows[], createdAt, updatedAt.
- **AvailabilityWindow** (`AssetAvailabilityWindowDto`): id, dayOfWeek, startTime, endTime. (No status/type/maintenance fields.)

## 12. Dependencies

- A4-7 backend running (`/api/v1/assets` CRUD).
- A4-2 backend for campus + department option lists.
- Reused frontend foundation (A4-335 SPA foundation) and shared table/modal/confirm components + `validateWith` / `mapApiError`, per the A4-410/415/420/425 precedent.

## 13. Assumptions

1. The department and campus pickers are populated from the A4-2 list endpoints (no dedicated asset-scoped lookup exists).
2. Availability windows are edited as an in-form list and persisted via whole-set replacement on asset save (there are no per-window endpoints) — Assumption tied to OQ-8.
3. The 7 day-of-week values are mirrored in the frontend as a constant (no backend enum endpoint — OQ-7).
4. `assetType` is a free string on the backend; the UI treats it as free text unless a curated list is adopted (OQ-1).

## 14. Consistency Notes

- The story says "Create/edit form: name, type, owning department, campus" but the backend **omits `identifier` from the story** and **omits department/campus from update**. Reconciled: create captures identifier + department + campus; edit shows identifier/department/campus **read-only** and edits only name/type/windows (FR-2, FR-5, OQ-4).
- The story says the windows sub-section supports "create/edit/delete." The backend has **no per-window endpoints**; the reconciliation is in-form list editing + whole-set replacement on save (FR-4.2, OQ-8). No FR promises per-window API calls.
- The story mentions "search" and an "availability status" column; neither is backed by the backend (no search param, no status field). Not promised by any FR; raised as OQ-2 and OQ-3.
- The story mentions "maintenance periods"; the window model has none. Not promised; raised as OQ-5.
- The story mentions "toasts"; feedback is surfaced via the shared inline message/confirm components consistent with prior frontend stories (no new toast system is built here).
- End-after-start for windows is a **client-side-only** rule (the backend does not enforce it for asset windows — OQ-6).

## 15. Out of Scope

- Backend / API / schema changes (owned by A4-7).
- Resource-blocking workflow (A4-8).
- Room master data frontend (A4-6 frontend).
- RBAC / role gating; scheduling; cross-department booking rules UI.

## 16. Open Questions

- **OQ-1 (assetType options):** `assetType` is a free string (≤50) with no backend enum/lookup. Should the UI offer a free-text field or a curated dropdown (e.g., EQUIPMENT_KIT, PROJECTOR_SET, SPORTS_FACILITY) as a frontend-only convenience? Proposed: free text with a short helper (mirrors A425-OQ-6). Confirm.
- **OQ-2 (search):** The story lists a "search" affordance, but `GET /api/v1/assets` supports only campusId/departmentId/assetType filters — no free-text search param. Proposed: ship the three filters, drop free-text search (mirrors A4-420 OQ-2). Confirm, or request a backend search param.
- **OQ-3 (availability status column):** The story's table shows "availability status," but `AssetDto` exposes no computed status. Proposed: show a client-side indicator such as window count ("3 windows") or omit the column. Confirm the preferred treatment.
- **OQ-4 (immutable-on-edit fields):** `UpdateAssetRequest` accepts only name/assetType/availabilityWindows — identifier, owning department, and campus **cannot** be changed. Proposed: show them read-only in edit. Confirm, or request a backend follow-up to allow editing them.
- **OQ-5 (maintenance periods):** The story mentions "maintenance periods," but windows carry only day/start/end. Proposed: defer (no backend surface). Confirm.
- **OQ-6 (end-after-start enforcement):** For asset windows the backend does **not** enforce end-after-start (only `@NotNull` + day pattern). The UI will enforce it client-side. Confirm that a backend follow-up to add the server-side check is desired (recommended for data integrity).
- **OQ-7 (day-of-week list):** No backend endpoint lists valid day values; the frontend mirrors the 7 uppercase names as a constant (mirrors A425-OQ-3). Acceptable?
- **OQ-8 (window edit semantics):** Because saving replaces the entire window set, an edit that only tweaks one row still resubmits all rows. Confirm this whole-set-replacement UX is acceptable (it matches the backend contract), or request per-window endpoints as a backend follow-up.

## 17. Traceability

| BRD / Story | Requirement |
|-------------|-------------|
| BRD 6.1 (asset master: owning dept, campus, availability calendar) | FR-1, FR-2, FR-4 |
| BRD 7.3 (asset-level blocking scope) | FR-6.3 (surfaces future A4-8 reference-block rejection); otherwise out of scope (A4-8) |
| Story AC-1 (create asset) | FR-2, AC-1 |
| Story AC-2 (add availability window) | FR-4, AC-2 |
| Story AC-3 (filter by type/campus) | FR-1.4, AC-3 |
| Story AC-4 (required-field validation) | FR-3, AC-4 |
| Story AC-5 (delete with confirmation) | FR-6, AC-5 |
| Story "identifier" (backend-required, story-omitted) | FR-2.1, FR-3.2, AC-6 |
| Story "search" | OQ-2 (no backend support) |
| Story "availability status" column | OQ-3 (no backend field) |
| Story "create/edit/delete per window" | FR-4.2, OQ-8 (list-replacement reconciliation) |
| Story "maintenance periods" | OQ-5 (no backend support — deferred) |
