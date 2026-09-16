# Requirement Document — Schedulable Asset Master Data Management

## 1. Introduction

This document captures the detailed requirements for managing non-room schedulable assets (movable equipment kits, projector sets, instrument sets, sports facilities, etc.) as master data. Each asset has an owning department, campus association, and an availability/blocking calendar. This entity provides the inventory that the resource blocking workflow (A4-8) operates on — without this master data, A4-8 has nothing to block.

## 2. User Story

**A4-7:** As a System Administrator, I want to manage non-room schedulable assets (movable equipment kits, projector sets, sports facilities) with owning department, campus, and an availability/blocking calendar per asset, so that the scheduling engine and blocking workflow can reference a complete inventory of all schedulable resources.

**Story Points:** 5

## 3. Actors

| Actor | Interaction |
|---|---|
| System Administrator (IT) | Creates, reads, updates, and deletes schedulable assets. BRD Section 4: master data management. |
| Department Coordinator | Reads asset data for their department. Views availability. May request cross-department usage (consumed by A4-36/Story 35). |
| Resource Blocking (A4-8) | References assets by ID when creating hard/soft blocks. |
| Shared Booking (A4-36) | May apply cross-department booking rules to shared assets. |

## 4. User Journeys

### Journey 1: Administrator Creates a Schedulable Asset

**Before:** The owning department and campus exist (A4-2).

**During:**
1. Admin enters: asset name, identifier [format TBD — see Open Question #1], type/category [TBD — see Open Question #2], owning department (FK), campus (FK).
2. Admin optionally configures a default availability calendar (operating hours/days when the asset is generally available).
3. System validates: department exists, campus exists, identifier uniqueness.
4. System persists the asset.

**After:** Asset is available for blocking operations (A4-8), shared booking (A4-36), and scheduling reference. Audit trail records creation.

### Journey 2: Administrator Updates Asset Availability Calendar

**Before:** Asset exists.

**During:**
1. Admin selects the asset.
2. Admin defines or updates availability windows (e.g., "available Monday-Friday 8 AM-6 PM" or "available only during semester").
3. System persists the calendar.

**After:** The scheduling engine and blocking workflow use this calendar to determine when the asset can be scheduled or booked. Times outside the calendar are treated as implicitly unavailable.

### Journey 3: Administrator Transfers Asset Ownership

**Before:** Asset exists under Department A.

**During:**
1. Admin changes owning_department from Department A to Department B.
2. System validates: new department exists.
3. System persists the change.

**After:** Cross-department booking rules (A4-36) now apply relative to the new owner. Audit trail records the transfer.

### Journey 4: Administrator Attempts to Delete an Asset with Active Blocks

**Before:** Asset has active resource blocks (A4-8) or historical references.

**During:**
1. Admin selects asset and chooses "Delete."
2. System checks: active resource blocks, historical block records, any session references.
3. References found - deletion rejected with error.

**After:** Asset unchanged.

### Journey 5: Coordinator Searches Assets by Type and Availability

**Before:** Assets exist with types and calendars configured.

**During:**
1. Coordinator searches for assets of type "projector_set" on Campus A that are available on Wednesday 2-4 PM.
2. System checks asset type, campus, and availability calendar.
3. Returns matching assets.

**After:** Coordinator knows what's available for scheduling or booking.

## 5. Functional Requirements

### FR-1: Asset Creation

- FR-1.1: The system shall allow creation of a schedulable asset with:
  - name (required)
  - identifier (required, unique within [scope TBD — see Open Question #1])
  - type/category (required — [TBD: enum or free-form? See Open Question #2])
  - owning_department (required, must reference existing department from A4-2)
  - campus (required, must reference existing campus from A4-2)
  - availability_calendar (optional at creation — default: always available unless blocked)
- FR-1.2: The system shall reject creation if the owning department does not exist.
- FR-1.3: The system shall reject creation if the campus does not exist.

### FR-2: Asset Reading and Search

- FR-2.1: The system shall allow reading a single asset by ID or identifier.
- FR-2.2: The system shall allow listing assets filtered by: type, owning department, campus, availability for a specific time range, or any combination.
- FR-2.3: The system shall return asset data including availability calendar and ownership.

### FR-3: Asset Update

- FR-3.1: The system shall allow updating: name, type, owning_department (transfer), campus, availability_calendar.
- FR-3.2: On department transfer, the system shall validate the new department exists.
- FR-3.3: Identifier mutability governed by Open Question #1.

### FR-4: Asset Deletion

- FR-4.1: The system shall allow deletion of an asset only if no active resource blocks, historical block records, or session references exist for it.
- FR-4.2: On attempted deletion with active references, the system shall return a clear error identifying referencing entities.
- FR-4.3: Whether deletion is hard-delete or soft-delete is governed by Open Question #3.

### FR-5: Availability Calendar Management

- FR-5.1: The system shall allow defining a default availability calendar per asset (recurring weekly pattern of available windows).
- FR-5.2: Times outside the defined availability calendar are treated as implicitly unavailable (the asset cannot be scheduled/booked during those times).
- FR-5.3: If no calendar is defined, the asset is assumed to be always available (unless explicitly blocked via A4-8).
- FR-5.4: The availability calendar is distinct from ad-hoc resource blocks (A4-8). The calendar represents the asset's regular operating schedule; A4-8 blocks represent exceptions (maintenance, events, etc.). See Open Question #4 for the interaction model.

## 6. Constraints Owned by This Document

| ID | Constraint | Type |
|---|---|---|
| HC-AST-1 | Every asset must reference an existing owning department | Hard |
| HC-AST-2 | Every asset must reference an existing campus | Hard |
| HC-AST-3 | Asset identifier must be unique within [scope TBD — see Open Question #1] | Hard |
| HC-AST-4 | Deletion blocked if any resource blocks or historical records reference the asset | Hard |

## 7. Constraints Referenced from Other Documents

| Constraint | Source |
|---|---|
| Department must exist in hierarchy | Story 1 (A4-2) |
| Campus must exist in hierarchy | Story 1 (A4-2) |
| Resource blocking applies to assets (blocks reference asset ID) | Story 7 (A4-8) |
| Shared resource booking may apply to assets | Story 35 (A4-36) |
| Block frequency/duration reports per resource | Story 7 (A4-8), AC 6 |
| Historical archive may reference assets | Story 41 (A4-42) |

## 8. Validation Rules

| Field | Rule |
|---|---|
| name | Required, non-empty, max length [TBD] |
| identifier | Required, unique within [TBD — see Open Question #1], format [TBD] |
| type/category | Required, [TBD: enum or free-form? See Open Question #2] |
| owning_department | Required, must reference existing department (A4-2) |
| campus | Required, must reference existing campus (A4-2) |
| availability_calendar | Optional, recurring weekly windows with valid times (start < end per window) |

## 9. Non-Functional Requirements

| NFR | Requirement |
|---|---|
| Performance | Asset search/filter queries shall return within [TBD]. BRD does not state asset count targets. |
| Audit | Every create, update, and delete action shall be recorded (Story 42 / A4-43). |
| Security | Only System Administrator may mutate asset data (BRD Section 4). |

## 10. Acceptance Criteria

1. **Given** an admin role, **When** they create a schedulable asset with name, type, owning department, and campus, **Then** the asset is persisted and available for blocking and scheduling operations.
2. **Given** a schedulable asset, **When** an availability calendar is configured (available windows, maintenance periods), **Then** the calendar is stored per asset.
3. **Given** an asset assigned to Department A, **When** Department B needs to use it, **Then** the ownership is visible and cross-department booking rules apply.
4. **Given** a search for assets by type or campus, **When** queried, **Then** matching assets are returned with their availability status.
5. **Given** the blocking workflow (Story 7/A4-8), **When** a block is raised on an asset, **Then** the asset's record exists and can be referenced (data dependency satisfied).

## 11. Data Model (Conceptual)

| Entity | Key Attributes | Relationships |
|---|---|---|
| SchedulableAsset | id, name, identifier, type, owning_department_id (FK), campus_id (FK), created_at, updated_at | belongs to Department; belongs to Campus; has many AvailabilityWindows; referenced by ResourceBlocks |
| AssetAvailabilityWindow | id, asset_id (FK), day_of_week, start_time, end_time | belongs to SchedulableAsset |

## 12. Dependencies

| Dependency | Description |
|---|---|
| Campus Hierarchy (A4-2) | Department and campus must exist before asset can be created |
| Authentication (A4-45) | User must be authenticated |
| RBAC (A4-44) | Role checks |
| Audit Trail (A4-43) | Mutations logged |

## 13. Assumptions

1. An asset belongs to exactly one department and one campus. If an asset is physically shared across campuses, it must be represented as belonging to one (primary) and accessed by others via cross-department booking rules (A4-36).
2. The availability calendar represents the asset's regular operating schedule (recurring weekly). One-off exceptions (specific date closures) are handled by the resource blocking workflow (A4-8), not by the calendar.
3. Assets are not directly assigned to sessions the way rooms are. They are booked or blocked. If a session requires a specific asset (e.g., a portable lab kit), the mechanism for that assignment is [TBD — see Open Question #5].

## 14. Consistency Notes

- FR-5.4 distinguishes the availability calendar (this document) from resource blocks (A4-8). The calendar is the baseline "when is this asset generally available?" and blocks are exceptions "this asset is unavailable on THIS date for THIS reason." Both must be consulted when checking asset availability.
- AC #5 explicitly confirms the data dependency: A4-8 cannot block an asset that doesn't exist here. This document is a prerequisite for A4-8.

## 15. Out of Scope

- Resource blocking workflow (raise, approve, release blocks) is managed by Story 7 (A4-8).
- Shared resource booking across departments is managed by Story 35 (A4-36).
- Room and lab master data (permanent spaces) is managed by Story 5 (A4-6) — this document covers only non-room assets.
- Bulk import of assets is covered by Story 50 (A4-51).

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | What is the uniqueness scope for asset identifiers? Within department? Campus? System-wide? And what format (serial number? institution-assigned code?)? | Affects HC-AST-3, FR-1.1 validation. | IT / Facilities |
| 2 | Is asset type/category a predefined enum (projector_set, sports_facility, instrument_kit, etc.) or free-form text? If enum: what are the values? If free-form: how to prevent inconsistency? | Affects FR-1.1 validation, search/filter reliability. | Academic Affairs / Facilities |
| 3 | Should asset deletion be hard-delete or soft-delete? Same data retention pattern as other master data entities. | Affects FR-4, data model. | IT / Registrar |
| 4 | How do the availability calendar (this document) and resource blocks (A4-8) interact? Is availability checked as: "asset available = within calendar AND not blocked"? Or does A4-8 supersede the calendar entirely? | Affects FR-5.4, scheduling engine logic. | System Design |
| 5 | Are assets ever directly assigned to sessions (e.g., "this lab session requires Portable Chemistry Kit #3"), or are they only available for blocking/reservation? If directly assigned: this document needs an assignment mechanism or the scheduling engine (Story 10) needs to know about assets. | Affects data model, scheduling engine scope, cross-story dependency. | Academic Affairs / System Design |
| 6 | Can an asset be associated with multiple campuses (shared equipment that rotates between sites), or strictly one campus? | Affects FR-1.1, data model, availability logic. | Facilities |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| 6.1 — "Asset / schedulable-resource master: non-room schedulable assets with owning department, campus, and an availability/blocking calendar per resource" | FR-1.1 (creation with all fields), FR-5 (availability calendar) |
| 7.3 — "Asset-level blocking scope: Hard/soft blocking applies to any schedulable asset, not only rooms and labs" | HC-AST-4 (supports blocking by providing the entity), consumed by A4-8 |
