# Requirement Document — Resource Blocking and Availability Workflow

## 1. Introduction

This document captures the detailed requirements for the resource blocking workflow — raising, approving, activating, and releasing hard or soft blocks on rooms, labs, and schedulable assets. When a hard block impacts already-published sessions, it requires approval and triggers automatic conflict detection, alternative proposals, and user notifications. This is the operational layer that makes resources temporarily unavailable, while A4-6 (rooms) and A4-7 (assets) provide the inventory being blocked.

## 2. User Story

**A4-8:** As a Department Coordinator, I want to raise hard or soft blocks on rooms, labs, and schedulable assets (for maintenance, events, or tentative holds) with an approval workflow when blocks impact published sessions, so that the scheduling engine respects resource unavailability and impacted sessions are proactively managed.

**Story Points:** 8

## 3. Actors

| Actor | Interaction |
|---|---|
| Department Coordinator | Raises blocks on resources within their scope. BRD 7.3 implies coordinators as block raisers. |
| System Administrator | May raise blocks on any resource. Configures reason codes. |
| Approver (HOD / Registrar) | Approves blocks that impact already-published sessions. BRD 7.3: "approval rules where already-published sessions are impacted." Which role approves is [TBD — see Open Question #1]. |
| Scheduling Engine (system) | Reads active hard blocks as inviolable constraints. Avoids soft blocks by default. |
| Conflict Detection (system) | Re-runs on hard block activation to identify impacted sessions. |
| Notification Service (A4-37) | Notifies affected users after reallocation is confirmed. |
| Facilities Team | May raise maintenance/breakdown blocks. See Open Question #1. |

## 4. User Journeys

### Journey 1: Coordinator Raises a Hard Block (No Published Session Impact)

**Before:** Resource (room or asset) exists (A4-6 or A4-7). No published sessions occupy the blocked period.

**During:**
1. Coordinator selects a resource and chooses "Raise Block."
2. Coordinator enters: block type (hard), date range, time range, reason code (e.g., "Maintenance," "Institutional Event"), optional recurrence pattern, reason text.
3. System validates: resource exists, dates are valid (start <= end), times valid.
4. System checks: are there published sessions for this resource in the blocked period? No.
5. Block is activated immediately (no approval needed since no published sessions impacted).

**After:** Resource is unschedulable during the block period. Scheduling engine respects the block. Audit trail records the block creation and activation.

### Journey 2: Coordinator Raises a Hard Block Impacting Published Sessions

**Before:** Resource exists. Published sessions occupy the blocked period.

**During:**
1. Coordinator selects resource and enters block details (same as Journey 1).
2. System checks: published sessions exist in the blocked period. Yes.
3. Block moves to "pending_approval" status. System identifies which sessions are impacted and presents them to the coordinator.
4. Block is submitted for approval to an authorized role [TBD — see Open Question #1].
5. Approver reviews: sees impacted sessions, reason for block.
6. Approver approves (or rejects with reason).

**After (on approval):** Block activates. System automatically:
- Re-runs conflict detection on impacted sessions.
- Lists all impacted sessions.
- Proposes alternative rooms/slots for each impacted session.
- Once reallocation is confirmed (by coordinator), notifies affected faculty and students.
Audit trail records: block creation, approval, activation, impacted sessions, alternatives proposed, reallocation, notifications.

### Journey 3: Coordinator Raises a Soft Block (Tentative Hold)

**Before:** Resource exists.

**During:**
1. Coordinator selects resource and raises a soft block with reason (e.g., "Tentative event booking," "Preferred-use hold").
2. System persists as soft block (no approval needed regardless of published sessions).

**After:** Scheduling engine avoids this slot by default but may override with an explicit warning and recorded justification (BRD 7.3). The override justification is persisted. Audit trail records the block.

### Journey 4: Block with Recurrence Pattern

**Before:** Resource exists.

**During:**
1. Coordinator raises a block with recurrence (e.g., "Every Monday 9-11 AM for 8 weeks starting [date]" for weekly maintenance).
2. System generates block instances for all matching dates.
3. Each instance follows the same approval logic (if impacting published sessions on that specific date).

**After:** All recurring instances are active. Each can be individually released if needed.

### Journey 5: Coordinator Releases a Block Early

**Before:** Active block exists. The reason has resolved (e.g., maintenance completed ahead of schedule).

**During:**
1. Coordinator selects the active block and chooses "Release."
2. System marks block status as "released" with release timestamp.

**After:** Resource becomes available again for scheduling. Existing schedules are NOT auto-modified — freed time is available for future runs. Audit trail records the release.

### Journey 6: Approver Rejects a Block Request

**Before:** Block is in "pending_approval" status.

**During:**
1. Approver reviews the block and rejects with a reason (e.g., "Cannot displace these sessions this close to exams").
2. Block status moves to "rejected."

**After:** Resource remains available. Coordinator is notified of rejection with reason. They can modify and resubmit or abandon.

## 5. Functional Requirements

### FR-1: Block Creation (Raise)

- FR-1.1: The system shall allow raising a block on any room (A4-6) or schedulable asset (A4-7) with:
  - resource_type (required: room or asset)
  - resource_id (required, must reference existing resource)
  - block_type (required: hard or soft)
  - start_date, end_date (required, start <= end)
  - start_time, end_time (required, start < end within a day)
  - reason_code (required — [TBD: predefined list or configurable? See Open Question #2])
  - reason_text (optional, free-form additional detail)
  - recurrence_pattern (optional — e.g., weekly, biweekly, specific day pattern)
- FR-1.2: The system shall validate that the referenced resource exists.
- FR-1.3: For hard blocks: if published sessions exist for the resource in the blocked period, the block shall require approval before activation (status = "pending_approval").
- FR-1.4: For hard blocks with no impacted published sessions: the block shall activate immediately (status = "active").
- FR-1.5: For soft blocks: the block shall activate immediately regardless of published sessions (no approval required).
- FR-1.6: For recurring blocks: the system shall generate individual block instances for each matching date based on the recurrence pattern.

### FR-2: Block Approval Workflow

- FR-2.1: When a hard block requires approval (FR-1.3), the system shall present: the resource, block period, reason, and a list of impacted published sessions.
- FR-2.2: An authorized approver [role TBD — see Open Question #1] shall be able to approve or reject the block.
- FR-2.3: On rejection, the system shall record the rejection reason and notify the raiser.
- FR-2.4: On approval, the system shall activate the block and trigger the impact handling workflow (FR-3).

### FR-3: Impact Handling on Hard Block Activation

- FR-3.1: On activation of a hard block impacting published sessions, the system shall automatically re-run conflict detection on the impacted sessions (consumed from Story 15/A4-16).
- FR-3.2: The system shall list all impacted sessions with details (course, faculty, batch, time slot).
- FR-3.3: The system shall propose alternative rooms/slots for each impacted session (consumed from Story 16/A4-17).
- FR-3.4: Once reallocation is confirmed [TBD: by coordinator manually accepting alternatives? See Open Question #3], the system shall notify all affected faculty and students (consumed from Story 36/A4-37).
- FR-3.5: If no alternatives are available for an impacted session, the system shall flag it for manual resolution by the coordinator.

### FR-4: Soft Block Engine Behavior

- FR-4.1: The scheduling engine shall avoid placing sessions in soft-blocked slots by default.
- FR-4.2: If the engine must override a soft block (no other option), it shall record an explicit justification for the override and produce a warning.
- FR-4.3: The override justification shall be visible in the draft review and persisted in the audit trail.

### FR-5: Block Release

- FR-5.1: The system shall allow releasing an active block (marking it as released with a timestamp).
- FR-5.2: On release, the resource becomes available for future scheduling. Existing schedules are NOT auto-modified.
- FR-5.3: Blocks that reach the end of their blocked period (end_date + end_time) are automatically marked as expired.

### FR-6: Block Reading and Search

- FR-6.1: The system shall allow listing blocks filtered by: resource, date range, status (pending/active/released/rejected/expired), block type (hard/soft), or any combination.
- FR-6.2: The system shall support viewing all active blocks for a resource to determine current availability.

### FR-7: Block Reporting

- FR-7.1: The system shall report block frequency per resource (how many blocks over a period).
- FR-7.2: The system shall report block duration per resource (total blocked time over a period).
- FR-7.3: Reports are consumed by Story 40 (A4-41) for utilization analysis.

### FR-8: Audit Trail

- FR-8.1: Every block action (raise, approve, reject, activate, release, modify) shall be recorded in the audit trail with: who, when, resource, action, reason.
- FR-8.2: The complete lifecycle of each block shall be traceable.

## 6. Constraints Owned by This Document

| ID | Constraint | Type |
|---|---|---|
| HC-BLK-1 | No session shall be placed in a hard-blocked resource's blocked period | Hard |
| HC-BLK-2 | Hard blocks impacting published sessions require approval before activation | Hard |
| HC-BLK-3 | On hard block activation, conflict detection must re-run on impacted sessions | Hard |
| HC-BLK-4 | Block must reference an existing resource (room or asset) | Hard |
| HC-BLK-5 | start_date must be <= end_date; start_time must be < end_time | Hard |
| SC-BLK-1 | Scheduling engine avoids soft-blocked slots by default (override with justification allowed) | Soft |

## 7. Constraints Referenced from Other Documents

| Constraint | Source |
|---|---|
| Rooms must exist (blocking target) | Story 5 (A4-6) |
| Assets must exist (blocking target) | Story 6 (A4-7) |
| Conflict detection re-runs on impacted sessions | Story 15 (A4-16) |
| Alternative slot suggestions | Story 16 (A4-17) |
| Notifications to affected users | Story 36 (A4-37) |
| Published timetable sessions that may be impacted | Story 20 (A4-21) |
| Audit trail records all actions | Story 42 (A4-43) |
| Block reports consumed by utilization analysis | Story 40 (A4-41) |

## 8. Validation Rules

| Field | Rule |
|---|---|
| resource_type | Required, one of: room, asset |
| resource_id | Required, must reference existing Room (A4-6) or SchedulableAsset (A4-7) based on type |
| block_type | Required, one of: hard, soft |
| start_date | Required, valid date, <= end_date |
| end_date | Required, valid date, >= start_date |
| start_time | Required, valid time, < end_time |
| end_time | Required, valid time, > start_time |
| reason_code | Required, [TBD: from predefined list or free? See Open Question #2] |
| reason_text | Optional, max length [TBD] |
| recurrence_pattern | Optional, valid pattern definition |

## 9. Non-Functional Requirements

| NFR | Requirement |
|---|---|
| Performance | Impact detection (checking which published sessions are affected) shall complete within [TBD] — should be fast enough that the raiser gets immediate feedback on submission. |
| Audit | Every block lifecycle action recorded (Story 42 / A4-43). |
| Security | Block raising permitted for [TBD — see Open Question #1]. Block approval restricted to authorized approvers. |

## 10. Acceptance Criteria

1. **Given** a coordinator raises a hard block on a room for a date range with a reason code, **When** submitted, **Then** the room becomes unschedulable for that period after approval (if required).
2. **Given** a soft block (tentative hold) on a resource, **When** the scheduling engine evaluates that slot, **Then** it avoids the slot by default but may override with a recorded justification and explicit warning.
3. **Given** a hard block raised on a resource with already-published sessions in that window, **When** the block is submitted, **Then** it requires approval from an authorized role before activation.
4. **Given** an approved hard block impacting published sessions, **When** activated, **Then** the system automatically identifies impacted sessions, proposes alternative rooms/slots, and notifies affected users after reallocation.
5. **Given** any block action (raise, approve, release), **When** completed, **Then** a full audit trail entry is created (who, when, why, resource, duration, reason code).
6. **Given** reporting needs, **When** block frequency and duration reports are requested per resource, **Then** the system generates accurate reports.
7. **Given** configurable recurrence patterns for blocks, **When** a recurring block is created (e.g., every Monday 9-11 AM for maintenance), **Then** it applies to all matching dates.

## 11. Data Model (Conceptual)

| Entity | Key Attributes | Relationships |
|---|---|---|
| ResourceBlock | id, resource_type, resource_id, block_type (hard/soft), start_date, end_date, start_time, end_time, reason_code, reason_text, recurrence_pattern, status (pending_approval/active/released/rejected/expired), raised_by, raised_at, activated_at, released_at | references Room or Asset; has many BlockApprovalActions |
| BlockApprovalAction | id, block_id (FK), action (approve/reject), actor_id, timestamp, comments | belongs to ResourceBlock |
| SoftBlockOverride | id, block_id (FK), session_id, justification, overridden_by, timestamp | records when engine overrides a soft block |

## 12. Dependencies

| Dependency | Description |
|---|---|
| Room Master Data (A4-6) | Rooms must exist before they can be blocked |
| Asset Master Data (A4-7) | Assets must exist before they can be blocked |
| Conflict Detection (A4-16) | Re-runs on block activation to find impacted sessions |
| Alternative Suggestions (A4-17) | Proposes alternatives for impacted sessions |
| Notification Service (A4-37) | Notifies affected users after reallocation |
| Published Timetable (A4-21) | Must be able to identify published sessions in the blocked period |
| Authentication (A4-45) | User must be authenticated |
| RBAC (A4-44) | Role checks for raising and approving |
| Audit Trail (A4-43) | All actions logged |

## 13. Assumptions

1. "Impacting published sessions" means: the blocked period overlaps with a time when the resource has an active published session assigned to it. Draft (unpublished) sessions do not trigger the approval workflow — they are simply flagged as conflicts.
2. Soft block overrides are rare — the engine uses them as last-resort when no un-blocked alternative exists. Every override is recorded and visible to the reviewer.
3. Recurring blocks generate independent instances. Each instance has its own status and can be individually released.
4. Block approval is binary (approve/reject) — there is no "partial approval" or "approve with conditions." Conditions would be communicated as comments.

## 14. Consistency Notes

- FR-1.3 says hard blocks impacting published sessions need approval. FR-1.5 says soft blocks never need approval. This distinction is intentional: soft blocks are "tentative" by nature — the engine can override them — so there's no need to gate them.
- FR-3.4 says "once reallocation is confirmed" — the trigger for confirmation is Open Question #3. Until resolved, the notification timing is undefined.
- BRD 6.4 says "notify affected users once reallocation is confirmed" — this explicitly says "once confirmed," not "immediately on block activation."

## 15. Out of Scope

- Room and asset CRUD (A4-6, A4-7) — this document assumes resources exist.
- Scheduling engine logic for avoiding blocks during generation (Story 10/A4-11).
- Alternative suggestion ranking algorithm (Story 16/A4-17).
- Notification delivery mechanics (Story 36/A4-37).
- Shared resource booking priority rules (Story 35/A4-36) — that's a reservation system; this is an unavailability system.
- Post-publication change management for the rescheduled sessions (Story 47/A4-48).

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Which roles can raise blocks, and which can approve them? BRD 7.3 says "Roles permitted to raise/approve blocks" but doesn't name specific roles. Can coordinators raise? Can HOD approve? Can Facilities raise maintenance blocks directly? | Affects Actors, FR-2.2, RBAC permissions. | Registrar / Academic Affairs |
| 2 | Are reason codes a predefined configurable list (Maintenance, Inspection, Breakdown, Institutional Event, External Booking) or free-form text? If configurable: who manages the list? | Affects FR-1.1, validation rules, reporting consistency. | Academic Affairs / IT |
| 3 | How is "reallocation confirmed" triggered? Does the coordinator manually review and accept each alternative? Or is it automatic once alternatives are proposed? Or does the coordinator confirm a batch of reallocations at once? | Affects FR-3.4, notification timing, user workflow. | Academic Affairs / System Design |
| 4 | Can an active block be modified (e.g., extended by 2 days, or shortened)? If yes, does modification of a hard block on published sessions re-trigger the approval workflow? | Affects FR lifecycle, potential re-approval logic. | Academic Affairs |
| 5 | Can multiple blocks overlap on the same resource at the same time (e.g., two different reasons for the same period)? Or should overlapping blocks be merged/rejected? | Affects FR-1.1 validation, data model. | System Design |
| 6 | When a soft block is overridden by the engine, who is notified? The block raiser? The coordinator? Nobody (just recorded)? | Affects FR-4.2, notification scope. | Academic Affairs |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| 6.4 — "Capture room, lab, and asset availability as hard or soft constraints" | FR-1.1 (block creation with hard/soft type) |
| 6.4 — "Apply the same hard/soft blocking model to any schedulable asset, not only rooms and labs" | FR-1.1 (resource_type supports room and asset) |
| 6.4 — "Provide a resource-blocking workflow with approval for blocks that impact already-published sessions" | FR-1.3, FR-2 (approval workflow) |
| 6.4 — "On activation of a hard block, the system shall automatically re-run conflict detection, list impacted sessions, propose alternative rooms/slots, and notify affected users once reallocation is confirmed" | FR-3 (impact handling) |
| 6.4 — "Maintain a complete audit trail for every block and report block frequency and duration per resource" | FR-8 (audit), FR-7 (reporting) |
| 7.3 — "Maintenance / blackout windows (hard blocks)" | FR-1.1 (hard block type), HC-BLK-1 |
| 7.3 — "Soft resource holds: Tentative reservations or preferred-use windows the engine avoids by default but may override with an explicit warning and recorded justification" | FR-4 (soft block engine behavior), SC-BLK-1 |
| 7.3 — "Block authorisation & workflow: Roles permitted to raise/approve blocks, reason codes, recurrence patterns, and approval rules" | FR-1 (raise), FR-2 (approval), FR-1.6 (recurrence), Open Questions #1, #2 |
