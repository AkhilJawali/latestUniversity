# Cross-Story Open Questions & Follow-ups Log

A single durable tracker for open questions and deferred follow-ups raised during
requirement generation, design, and code review across stories. Per-story detail
still lives in each story's requirement Section 16, design doc, and code-review doc;
this file is the consolidated index so nothing is lost between stories.

Status legend: OPEN (needs decision) · DECIDED (resolution chosen, may need work) ·
DONE (resolved/implemented) · DEFERRED (parked to a later phase/story).

---

## A4-415 — Frontend Course Management

| ID | Item | Status | Resolution / Notes |
|----|------|--------|--------------------|
| A4-415-F1 | Cross-listing panel is add-only — A4-3 backend has no "list cross-listings" endpoint and CourseDto exposes only a boolean `isCrossListed`, so the UI cannot render a removable list of cross-listed departments. | DEFERRED | Backend follow-up: add a list-cross-listings endpoint (or expose the department set on CourseDto). `useRemoveCrossListing` hook already exists for when it lands. Recorded in docs/code-review/A4-415-code-review.md and a header comment in CrossListingPanel.jsx. |

---

## A4-420 — Frontend Faculty Profile Management

Source: docs/requirements/A4-420-frontend-faculty-profile-management-requirements.md §16.

| ID | Item | Status | Resolution / Notes |
|----|------|--------|--------------------|
| OQ-1 | No GET endpoint to read a faculty's current campus associations; campuses absent from FacultyDto. UI can add/remove but not display the current set. | DONE | **Lead decision (2026-09-04):** added A4-4 backend endpoint `GET /api/v1/faculty/{id}/campuses` → `{data:[{campusId,name,code}]}`. New FacultyCampusDto + FacultyCampusAssociationService.getAssociationDtos + controller mapping. NOTE: verified by inspection only — Maven not available in this env to compile. |
| OQ-2 | Story says "search by name" but the list endpoint has no free-text/name param (only departmentId, designation, competencyCourseId). | DECIDED (provisional) | Design proceeds accepting department + designation + competency-course filters (no free-text name search). Lead may override at design approval. |
| OQ-3 | Story lists a "competency-mismatch warning" - no backend flag/endpoint exists anywhere. | DEFERRED (provisional) | Design defers the mismatch warning (no backend surface). Lead may override at design approval. |
| OQ-4 | No endpoint lists valid designations; frontend mirrors the 7 fixed strings. | DECIDED (provisional) | Frontend mirrors the 7 fixed designation strings as a constant. Lead may override at design approval. |
| OQ-5 | Competency read-back returns course IDs only (no names); names resolved via the A4-3 courses list. | DECIDED (provisional) | Names resolved via the loaded A4-3 courses list. Lead may override at design approval. |

---

## A4-425 — Frontend Faculty Availability and Preferences

Source: docs/requirements/A4-425-frontend-faculty-availability-preferences-requirements.md §16. All approved by lead 2026-09-04.

| ID | Item | Status | Resolution / Notes |
|----|------|--------|--------------------|
| A425-OQ-1 | Story wants a per-window hard/soft flag; backend availability window has no such field. | DECIDED | Reconciled at section level: availability windows = hard section, preferences = soft section. No per-window toggle. (Backend follow-up to add a per-window type remains possible later.) |
| A425-OQ-2 | Backend derives BLOCKED/AVAILABLE mode from designation but does not expose it on any A4-5 endpoint. | DECIDED | UI labels windows as "unavailability" by default. (Backend follow-up to expose the mode remains possible later.) |
| A425-OQ-3 | No endpoint lists valid day-of-week values. | DECIDED | Frontend mirrors the 7 uppercase day names as a constant. |
| A425-OQ-4 | Preferred time-of-day supports only MORNING/AFTERNOON/NO_PREFERENCE (no EVENING). | DECIDED | UI offers exactly those three. (Backend follow-up to add EVENING remains possible later.) |
| A425-OQ-5 | Story mentions "preference weighting"; no backend weight field exists. | DEFERRED | No weighting UI. Backend follow-up if the feature is prioritised. |
| A425-OQ-6 | `reasonCode` is a free string (≤50), not an enum. | DECIDED | UI uses a free-text reason code field with a short helper (no curated dropdown). |

---

## A4-430 — Frontend Room and Lab Master Data

Source: docs/requirements/A4-430-frontend-room-and-lab-master-data-requirements.md §16. Approved by lead 2026-09-04.

| ID | Item | Status | Resolution / Notes |
|----|------|--------|--------------------|
| A430-OQ-1 | No free-text search on the rooms list endpoint (only campus/type/building/minCapacity/equipmentTag). | DECIDED | UI offers the available filters; no free-text search box. (Backend `?search=` follow-up possible later.) |
| A430-OQ-2 | building/floor are free-text strings; no Building master-data resource for a picker. | DECIDED | Free-text inputs for building/floor; building filter driven by distinct values in the loaded rooms. (Backend Building resource follow-up possible later.) |
| A430-OQ-3 | equipment-tag filter is single-tag, exact, case-sensitive. | DECIDED | Single-tag filter for this story. (Multi-tag/contains is a backend follow-up.) |
| A430-OQ-4 | RoomDto does not expose isActive. | DECIDED | No active/inactive column (only active rooms are returned). |

---

## A4-435 — Frontend Schedulable Asset Master Data

Source: docs/requirements/A4-435-frontend-schedulable-asset-master-data-requirements.md §16. Approved by lead 2026-09-04.

| ID | Item | Status | Resolution / Notes |
|----|------|--------|--------------------|
| A435-OQ-1 | assetType is a free String (no backend enum/lookup). | DECIDED | Free-text assetType field (short helper). No curated dropdown. |
| A435-OQ-2 | No free-text search on the assets list (only campusId/departmentId/assetType). | DECIDED | Three filters only; no search box. |
| A435-OQ-3 | AssetDto exposes no availability-status field. | DECIDED | Show a client-side window-count indicator; no server status column. |
| A435-OQ-4 | UpdateAssetRequest omits identifier/owningDepartment/campus (immutable). | DECIDED | Edit form shows identifier/department/campus read-only. |
| A435-OQ-5 | Story mentions "maintenance periods"; window model has none. | DEFERRED | No maintenance-period UI. |
| A435-OQ-6 | Backend does not enforce end-after-start on asset windows. | DECIDED | Enforce end-after-start client-side. (Backend follow-up recommended.) |
| A435-OQ-7 | No endpoint lists valid day-of-week values. | DECIDED | Frontend mirrors the 7 uppercase day names. |
| A435-OQ-8 | Saving replaces the entire window set (no per-window endpoints). | DECIDED | In-form window list edited locally; whole-set replacement on asset save. |
---

## A4-440 — Frontend Academic Calendar Management

Source: docs/requirements/A4-440-frontend-academic-calendar-management-requirements.md §16. Approved by lead 2026-09-04.

| ID | Item | Status | Resolution / Notes |
|----|------|--------|--------------------|
| A440-OQ-1 | No client-side overlap validation surface — backend enforces end-after-start and within-semester (exam/orientation only) but never checks date overlaps between holidays/exams/orientation. | DECIDED | Client validation is advisory only (mirror backend rules: end≥start; exam/orientation within semester). No overlap warnings in this story. (Backend overlap-check follow-up possible later.) |
| A440-OQ-2 | Error-envelope shape for 4xx not explicitly documented on every A4-9 endpoint. | DECIDED | Assume the standard error envelope (mapApiError handles both field-level `details` and `message`). |
| A440-OQ-3 | No update endpoints: calendar is create+delete only; holidays add+delete; exam windows and orientation periods are add-only (no GET, no edit, no delete). | DECIDED | Descope in-place edit entirely. Calendar = create + delete. Holidays = add + remove. Exam windows / orientation = add-only. Sub-entities are read via the calendar `GET /{id}` children arrays. (Backend edit/delete endpoints are a follow-up.) |
| A440-OQ-4 | Institution-wide holiday is only a `scope` flag on the holiday — no cross-campus fan-out exists. | DECIDED | Display holidays by their `scope` field (CAMPUS_SPECIFIC / INSTITUTION_WIDE); no UI fan-out or cross-campus propagation. |
| A440-OQ-5 | List endpoints are `GET /campus/{campusId}` and `GET /year/{academicYear}`; AcademicCalendarDto carries `campusId` (Long) with no campus name/object. | DECIDED | Campus-scoped listing only (campus selector via A4-1 `useCampuses` resolves the campus name). Year-based listing view skipped for this story. |
---

## A4-15 — Frontend Drag-and-Drop Timetable Editor

Source: docs/requirements/A4-15-frontend-drag-drop-timetable-editor-requirements.md §16. Approved by lead (chat) 2026-09-09.

| ID | Item | Status | Resolution / Notes |
|----|------|--------|--------------------|
| A415-OQ-1 | No backend endpoint to persist a moved session (SchedulingController has generate/sessions/violations/regenerate/lock/unlock but no session move PUT/PATCH). | DECIDED | Staged client-side mode: moves held in view state, clearly marked unsaved; disabled "Save layout" with explanatory tooltip (no false persistence). Backend follow-up: add `PATCH /timetables/{draftId}/sessions/{sessionId}` (move). |
| A415-OQ-2 | No optimistic-concurrency mechanism (no version/ETag on draft or session). | DECIDED | Interim: advisory per-session lock/unlock on pick-up/drop + surface draft `version` as a staleness hint. Backend follow-up: version/409 rejection of stale writes. |
| A415-OQ-3 | No alternatives endpoint (A4-17 separate/open). | DECIDED | Client-side derivation: probe nearby empty cells with the same conflict-check, list clean ones by proximity, labelled "suggested (not engine-ranked)". Upgrades to A4-17 when available. |
| A415-OQ-4 | STOMP transport would add a new dependency (@stomp/stompjs). | DECIDED | REST-first (`POST /drafts/{id}/conflict-check`); no new dependency. WebSocket/STOMP deferred as a future enhancement (transport isolated behind one hook). |
| A415-OQ-5 | Session payload carries placement fields except `durationMinutes`. | DECIDED | Resolve duration from the campus slot grid when available, else a default (60); backend follow-up to add `durationMinutes` to the session DTO. |
| A415-OQ-6 | Feedback pattern. | DECIDED | Inline status region + ARIA live for conflict results; no new toast system. |

---

## A4-19 — Multi-Level Approval Workflow (backend)

Source: docs/requirements/A4-19-multi-level-approval-workflow-requirements.md §16. Approved by lead (chat) 2026-09-09 with proposed defaults accepted.

| ID | Item | Status | Resolution / Notes |
|----|------|--------|--------------------|
| A419-OQ-1 | Expose a pipeline-configuration API (CRUD for pipelines/levels), or seed + DB edit for "configurable" (AC-5)? | DECIDED | Seed a default pipeline (Coordinator → HOD → Dean/Registrar) + read endpoints now. Full pipeline CRUD API is a follow-up (config screen is a frontend story). |
| A419-OQ-2 | Is the approval pipeline global or scoped per department/campus? | DECIDED | One global active pipeline for this story; per-scope selection added later. |
| A419-OQ-3 | RBAC not built (permitAll) — how is "who may act at this level" enforced now? | DECIDED | Store `actorUserId` from request context + advisory `requiredRole` on each level; enforce when the Auth module lands. userId source (header/principal) confirmed at design. |
| A419-OQ-4 | Where is the `PUBLISHED` transition written — on final approval here, or entirely in A4-21? | DECIDED | This story sets `DraftStatus=APPROVED` and emits a final-approval event; A4-21 writes PUBLISHED + side-effects (notifications, calendar feed). Clean A4-19/A4-21 boundary (HC-AW-6). |
| A419-OQ-5 | Optimistic concurrency: new `@Version` on workflow_instances, the draft `version`, or both? | DECIDED | Add `@Version` to `workflow_instances`; reject stale concurrent actions with 409. |
| A419-OQ-6 | Should a "withdraw submission" action exist (submitter pulls a draft out of review)? | DECIDED | Not in scope for this story; the `WITHDRAWN` state value is reserved for a later follow-up. |
---

## A4-21 — Timetable Publication and Notification Trigger (backend)

Source: docs/requirements/A4-21-timetable-publication-notification-requirements.md §16. Approved by lead (chat) 2026-09-09 with proposed defaults accepted.

| ID | Item | Status | Resolution / Notes |
|----|------|--------|--------------------|
| A421-OQ-1 | A4-37 (notification delivery engine) is not built. Does A4-21 deliver notifications or just trigger? | DECIDED | A4-21 emits `TimetablePublishedEvent` (carrying affected faculty/batch ids); actual multi-channel delivery (preferred channel, digests, retries) is deferred to A4-37, which consumes the event. |
| A421-OQ-2 | A4-39 (iCal calendar feed) is not built. Does A4-21 refresh feeds or just signal? | DECIDED | Feed refresh is signalled via the same `TimetablePublishedEvent`; actual .ics regeneration is deferred to A4-39. |
| A421-OQ-3 | No per-student entity exists — how are "students" addressed for AC-3? | DECIDED | Address students at batch/section granularity: the event carries `affectedBatchIds`/`sectionIds`. Per-student fan-out belongs to A4-37 when a student/enrolment model exists. |
| A421-OQ-4 | Persist a dedicated `timetable_publications` log, or reuse the generic `AuditEvent`? | DECIDED | Reuse `AuditEvent` for the publish audit — **no new table/migration** this story. A publication log can be added later if reporting needs it. |
| A421-OQ-5 | Re-publishing an already-PUBLISHED draft: idempotent no-op or 422? | DECIDED | Return **422 "already published"** (explicit) to avoid a duplicate event emission / duplicate active version. |
| A421-OQ-6 | Manual publish endpoint placement. | DECIDED | `POST /api/v1/timetables/{draftId}/publish` — co-located with the existing draft resource (SchedulingController), consistent with the scheduling module. |
