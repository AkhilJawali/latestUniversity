# Requirement Document — Room and Lab Master Data Management

## 1. Introduction

This document captures the detailed requirements for managing room and lab master data — including capacity, type, equipment tags, and physical location (campus/building/floor). Room data is consumed by the scheduling engine (room assignment based on batch strength, equipment needs, and proximity), conflict detection (room double-booking), resource blocking (unavailability periods), shared resource booking, utilization reporting, and travel-time buffer calculations.

## 2. User Story

**A4-6:** As a System Administrator, I want to manage rooms and labs with capacity, type (classroom/lab/seminar hall/auditorium), equipment tags, and building/floor location, so that the scheduling engine can allocate appropriate spaces based on batch size, course requirements, and proximity.

**Story Points:** 5

## 3. Actors

| Actor | Interaction |
|---|---|
| System Administrator (IT) | Creates, reads, updates, and deletes rooms/labs. BRD Section 4: "IT / System Administrator — Manages master data." |
| Department Coordinator | Reads room data for scheduling. Views real-time availability. Does not mutate room master data. |
| Scheduling Engine (system) | Reads room capacity, type, equipment tags, and location for assignment decisions. |
| Conflict Detection (system) | Detects room double-booking. |
| Resource Blocking (A4-8) | Creates blocks (hard/soft) on rooms — references room by ID. |
| Utilization Reporting (A4-41) | Reads room data and session assignments for utilization calculations. |
| Facilities Team | BRD Section 11: "Room/lab inventory data kept current by campus facilities teams." May have update rights — see Open Question #1. |

## 4. User Journeys

### Journey 1: Administrator Creates a Room

**Before:** The campus exists (A4-2). Building/floor information is known.

**During:**
1. Admin selects the campus.
2. Admin enters: room name/number, room code [uniqueness TBD — see Open Question #2], capacity (positive integer), type (classroom/lab/seminar_hall/auditorium), equipment_tags (optional array), building name, floor number/identifier.
3. System validates: campus exists, capacity > 0, type is valid enum value, code uniqueness (scope per Open Question #2).
4. System persists the room.

**After:** Room is available for scheduling assignment, blocking, and booking. Audit trail records creation.

### Journey 2: Administrator Updates Room Capacity (Post-Renovation)

**Before:** Room exists. May have active sessions assigned.

**During:**
1. Admin selects the room and chooses "Edit."
2. Admin updates capacity (e.g., 60 → 80 after expansion).
3. System validates: new capacity > 0.
4. System persists the update.

**After:** Subsequent scheduling uses new capacity. If capacity decreased and existing sessions have batch strength > new capacity, those are flagged as capacity mismatches (consumed by Story 15/A4-16). Audit trail records change.

### Journey 3: Administrator Adds Equipment Tags to a Lab

**Before:** Room exists with type = lab.

**During:**
1. Admin adds equipment tags (e.g., ["chemistry_fume_hood", "spectrophotometer"]).
2. System persists the tags.

**After:** Scheduling engine and lab scheduling (Story 23/A4-24) can now match courses requiring these tags to this room.

### Journey 4: Coordinator Searches for Available Rooms by Equipment

**Before:** Rooms exist with equipment tags.

**During:**
1. Coordinator searches for rooms with tag "computer_lab" and capacity >= 30.
2. System returns matching rooms with their current availability status.

**After:** Coordinator can manually assign or verify engine's room choice.

### Journey 5: Administrator Attempts to Delete a Room with Active Sessions

**Before:** Room has sessions assigned in a published or draft timetable, or active resource blocks, or historical timetable references.

**During:**
1. Admin selects room and chooses "Delete."
2. System checks: active sessions, resource blocks, historical timetable references.
3. References found → deletion rejected with error listing referencing entities.

**After:** Room unchanged. Admin must deactivate or remove references first (per Open Question #3).

### Journey 6: System Provides Real-Time Room Availability View

**Before:** Rooms exist. Sessions are scheduled. Blocks exist.

**During:**
1. User queries room availability for a specific day/time range.
2. System checks: scheduled sessions in those slots + active resource blocks.
3. System returns: which rooms are free, which are occupied (by whom/what), which are blocked.

**After:** User has visibility for manual scheduling decisions or verification.

## 5. Functional Requirements

### FR-1: Room/Lab Creation

- FR-1.1: The system shall allow creation of a room with:
  - name/number (required)
  - code (required, unique within [scope TBD — see Open Question #2])
  - capacity (required, positive integer)
  - type (required, one of: classroom, lab, seminar_hall, auditorium)
  - equipment_tags (optional, array of string tags)
  - campus_id (required, must reference existing campus from A4-2)
  - building (required — see Open Question #4 for whether this is a text field or FK)
  - floor (required)
- FR-1.2: The system shall reject creation if the referenced campus does not exist.
- FR-1.3: The system shall reject creation if capacity is <= 0.

### FR-2: Room/Lab Reading and Search

- FR-2.1: The system shall allow reading a single room by ID or code.
- FR-2.2: The system shall allow listing rooms filtered by: campus, building, floor, type, equipment tag, minimum capacity, or any combination.
- FR-2.3: The system shall return room data including all equipment tags and full location (campus/building/floor).
- FR-2.4: The system shall provide a real-time availability view showing which rooms are free/occupied/blocked for a given day and time range (BRD 6.4: "Provide real-time room-availability and utilisation views").

### FR-3: Room/Lab Update

- FR-3.1: The system shall allow updating: name/number, capacity, type, equipment_tags (add/remove), building, floor.
- FR-3.2: Code mutability governed by Open Question #2 (same pattern as hierarchy/course codes).
- FR-3.3: On capacity decrease, if existing sessions assigned to this room have batch strength > new capacity, the system shall flag those as capacity mismatch conflicts.
- FR-3.4: On equipment tag removal, if courses assigned to this room require the removed tag, the system shall flag those as equipment mismatch warnings.
- FR-3.5: On type change (e.g., classroom to lab), the system shall [TBD — validate no incompatible sessions assigned? See Open Question #5].

### FR-4: Room/Lab Deletion

- FR-4.1: The system shall allow deletion of a room only if no scheduled sessions, resource blocks, shared bookings, or historical/archived timetables reference it.
- FR-4.2: On attempted deletion with active references, the system shall return a clear error identifying referencing entities by type and count.
- FR-4.3: Whether deletion is hard-delete or soft-delete (deactivation) is governed by Open Question #3.

### FR-5: Equipment Tag Management

- FR-5.1: The system shall store equipment tags as an array of strings on each room.
- FR-5.2: The system shall support search/filter by equipment tag (FR-2.2).
- FR-5.3: Whether equipment tags are free-form strings or selected from a controlled vocabulary is governed by Open Question #6.
- FR-5.4: Equipment tags are consumed by the scheduling engine and lab scheduling (Story 23/A4-24) for course-to-room matching.

## 6. Constraints Owned by This Document

| ID | Constraint | Type |
|---|---|---|
| HC-ROOM-1 | Every room must reference an existing campus | Hard |
| HC-ROOM-2 | Room capacity must be a positive integer (> 0) | Hard |
| HC-ROOM-3 | Room type must be one of: classroom, lab, seminar_hall, auditorium | Hard |
| HC-ROOM-4 | Room code must be unique within [scope TBD — see Open Question #2] | Hard |
| HC-ROOM-5 | Deletion blocked if any entity (sessions, blocks, bookings, historical data) references the room | Hard |
| HC-ROOM-6 | Room capacity must be >= assigned batch/group strength (enforced at scheduling/conflict detection time, not at room creation time) | Hard |

## 7. Constraints Referenced from Other Documents

| Constraint | Source |
|---|---|
| Campus must exist in hierarchy | Story 1 (A4-2) |
| Resource blocking creates hard/soft blocks on rooms | Story 7 (A4-8) |
| Scheduling engine assigns rooms based on capacity + equipment | Story 10 (A4-11) |
| Conflict detection checks room double-booking | Story 15 (A4-16), AC 2 |
| Conflict detection checks capacity mismatch | Story 15 (A4-16), AC 5 |
| Lab scheduling matches equipment tags to course requirements | Story 23 (A4-24), AC 3 |
| Shared resource booking across departments | Story 35 (A4-36) |
| Travel-time buffer uses building/campus location for proximity | Story 34 (A4-35) |
| Room utilization reports | Story 40 (A4-41) |
| Historical archive references rooms in past timetables | Story 41 (A4-42) |

## 8. Validation Rules

| Field | Rule |
|---|---|
| name/number | Required, non-empty, max length [TBD] |
| code | Required, unique within [TBD — see Open Question #2], format [TBD] |
| capacity | Required, positive integer (> 0) |
| type | Required, one of: classroom, lab, seminar_hall, auditorium |
| equipment_tags | Optional, array of non-empty strings [TBD: free-form or controlled vocabulary? See Open Question #6] |
| campus_id | Required, must reference existing campus (A4-2) |
| building | Required, format [TBD — see Open Question #4] |
| floor | Required, format [TBD — integer or string like "Ground", "Basement"?] |

## 9. Non-Functional Requirements

| NFR | Requirement |
|---|---|
| Performance | Room search/filter and real-time availability queries shall return within [TBD] for up to 200+ rooms (BRD Section 8: "200+ rooms"). |
| Audit | Every create, update, and delete action shall be recorded in the audit trail (Story 42 / A4-43). |
| Security | Only System Administrator (and possibly Facilities team — see Open Question #1) may mutate room data. Coordinators and others have read-only access. |

## 10. Acceptance Criteria

1. **Given** an admin role, **When** they create a room with capacity, type (lab/classroom/seminar hall/auditorium), equipment tags, and building/floor/campus location, **Then** all attributes are persisted.
2. **Given** a room with capacity 60, **When** a scheduling check is performed against a batch of strength 70, **Then** the system identifies the capacity mismatch.
3. **Given** a search by equipment tag, **When** "computer_lab" is queried, **Then** all rooms with that equipment tag are returned.
4. **Given** rooms in different buildings, **When** building/floor location is stored, **Then** the system can use it for proximity-based allocation decisions.
5. **Given** a room update (e.g., capacity change after renovation), **When** saved, **Then** subsequent scheduling uses the updated capacity.

## 11. Data Model (Conceptual)

| Entity | Key Attributes | Relationships |
|---|---|---|
| Room | id, name, code, capacity, type, equipment_tags, campus_id (FK), building, floor, created_at, updated_at | belongs to Campus; referenced by Sessions, ResourceBlocks, SharedBookings |

Note: Whether "building" is a text field or a separate entity with its own CRUD is Open Question #4. If separate entity, it would be: Building (id, name, campus_id, address) with Room.building_id as FK.

## 12. Dependencies

| Dependency | Description |
|---|---|
| Campus Hierarchy (A4-2) | Campus must exist before a room can be created |
| Authentication (A4-45) | User must be authenticated |
| RBAC (A4-44) | Role checks determine mutation permissions |
| Audit Trail (A4-43) | Mutations logged |

## 13. Assumptions

1. Room type is a single value — a room is either a classroom OR a lab OR a seminar hall OR an auditorium. Multi-purpose rooms that function as multiple types are not supported in this scope.
2. Equipment tags on a room represent permanent/semi-permanent installations (built-in projectors, fume hoods, computer terminals). Movable equipment (projector carts, portable screens) is managed as schedulable assets in Story 6 (A4-7), not as room equipment tags.
3. "Building" and "floor" together provide enough location granularity for proximity calculations. The scheduling engine (Story 34/A4-35) uses campus + building to determine if travel-time buffers apply.

## 14. Consistency Notes

- HC-ROOM-6 (capacity >= batch strength) is enforced at scheduling/conflict detection time (Story 15, AC 5), not at room creation time. A room with capacity 30 is valid; it just can't be assigned to a batch of 40.
- BRD 6.4 says "real-time room-availability and utilisation views" — FR-2.4 covers the availability view. Utilization calculation/reporting is owned by Story 40 (A4-41), not this document. This document provides the data; that story computes the metrics.
- Equipment tag matching logic (how the engine decides a room satisfies a course's equipment needs) is owned by Story 23 (A4-24). This document just stores the tags.

## 15. Out of Scope

- Resource blocking (hard/soft blocks on rooms) is managed by Story 7 (A4-8).
- Shared resource booking across departments is managed by Story 35 (A4-36).
- Room utilization reporting/metrics is managed by Story 40 (A4-41).
- Schedulable assets (non-room equipment) are managed by Story 6 (A4-7).
- Bulk import of rooms is covered by Story 50 (A4-51).
- Inter-campus room borrowing rules are managed by Story 35 (A4-36).

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Should the Facilities team have direct update rights on room data (e.g., updating capacity after renovation, marking rooms under maintenance)? BRD Section 11 says "Room/lab inventory data kept current by campus facilities teams" — implying they maintain it. But BRD Section 4 assigns master data to IT/Admin. | Affects Actors, RBAC permissions. | IT / Facilities / Academic Affairs |
| 2 | What is the uniqueness scope for room codes? Within building (Room 101 in Building A != Room 101 in Building B)? Within campus? System-wide? | Affects HC-ROOM-4, FR-1.1 validation. | Facilities / IT |
| 3 | Should room deletion be hard-delete or soft-delete? Historical timetables reference rooms. A decommissioned room's data must persist in archives. | Affects FR-4, data model (active/inactive flag). | IT / Registrar |
| 4 | Is "Building" a separate entity with its own CRUD (name, campus, address, coordinates for proximity) or just a text field on the room record? If separate entity: who manages it? What are its attributes? If text field: how does the proximity calculation work without structured building data? | Affects data model, FR-1.1, travel-time buffer logic (Story 34). | System Design / Facilities |
| 5 | Can a room's type change (e.g., classroom to lab after renovation)? If yes, what happens to sessions currently assigned to it that are incompatible with the new type? | Affects FR-3.5, cascading conflicts. | Facilities / Academic Affairs |
| 6 | Should equipment_tags be free-form strings (flexible but prone to typos/inconsistency) or selected from a controlled vocabulary/tag registry (consistent but requires maintaining the registry)? If controlled: who maintains the registry? | Affects FR-5.3, validation rules, matching reliability with course equipment needs. | System Design / Facilities |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| 6.1 — "Room/Lab master: capacity, room type, equipment tags, campus/building/floor location" | FR-1.1 (all fields) |
| 7.3 — "Room capacity: Must be greater than or equal to batch/group strength" | HC-ROOM-6 (enforced at scheduling time by Story 15) |
| 7.3 — "Room type & equipment: Lab vs. classroom vs. seminar hall vs. auditorium; specialised equipment/software needs" | FR-1.1 (type + equipment_tags), HC-ROOM-3 |
| 7.3 — "Location / transit buffer: Minimise back-to-back session travel time between distant buildings/campuses" | FR-1.1 (building/floor/campus location), consumed by Story 34 |
| 6.4 — "Allocate rooms/labs based on batch strength vs. room capacity, required equipment, and proximity preferences" | FR-1.1 (capacity + equipment + location stored), consumed by Story 10 |
| 6.4 — "Provide real-time room-availability and utilisation views" | FR-2.4 (real-time availability view) |
