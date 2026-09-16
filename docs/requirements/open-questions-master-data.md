# Open Questions — Master Data Stories (A4-2 through A4-10)

This file consolidates all open questions from the 9 master data requirement documents. Each question needs stakeholder confirmation before design can proceed.

---

## A4-2: Campus Hierarchy Master Data Management

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Should Department Coordinators have create/update rights on batches and sections within their own department, or is all hierarchy mutation restricted to the System Administrator role? BRD Section 4 assigns "master data" to IT/Admin. | Affects Actors table, RBAC permissions, and Security NFR. | Registrar / Academic Affairs |
| 2 | Can entity codes (campus code, department code, program code) be changed after creation? Immutability simplifies referential integrity but reduces flexibility. If codes are mutable, what happens to external references (reports, exports) that used the old code? | Affects FR-1.3, FR-2.3, FR-3.3, and whether a code-change audit entry is needed. | IT / System Administrator |
| 3 | Should deletion be hard-delete or soft-delete (deactivation)? The BRD requires retaining historical timetables for years (Section 8 — Data Retention). Archived timetables reference departments/batches. Hard-deleting a "childless" entity could orphan historical data. Soft-delete (marking inactive) preserves referential integrity for archives while hiding from active use. | Affects FR-1.4, FR-2.4, FR-3.4, FR-4.6, FR-4.7, and the data model (needs active/inactive flag). | Registrar / IT |
| 4 | Who owns the "elective basket" entity that Batch.elective_basket references? The BRD says "elective basket enrolled" as a batch attribute. Is this a reference to a basket entity defined in the elective registration module (Stories 24/25), or is it a simple text label? If it's a FK, the basket must exist before a batch can reference it — affecting creation order and validation. | Affects FR-4.1 validation, cross-story data dependency, creation sequencing. | Academic Affairs / System Design |

---

## A4-3: Course Management

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Should Coordinators have create/update rights on courses within their department, or is all course mutation restricted to the System Administrator? BRD Section 4 assigns "master data" to IT/Admin, but the story is written from Coordinator perspective. | Affects Actors table, RBAC permissions, Security NFR. | Registrar / Academic Affairs |
| 2 | What is the uniqueness scope for course codes? System-wide (no two courses anywhere share a code) or within owning department? | Affects HC-CRS-4, FR-1.1 validation. | Academic Affairs |
| 3 | Should course deletion be hard-delete or soft-delete (deactivation)? Same data retention concern as hierarchy (A4-2, Open Question #3): historical timetables reference courses for years. | Affects FR-4.1, FR-4.3, data model (needs active/inactive flag). | Registrar / IT |
| 4 | If a course's L-T-P split is updated while sessions for that course exist in an active/unpublished draft, what should happen? Invalidate those sessions? Flag them for re-generation? Allow the inconsistency until next generation? | Affects FR-3.1 behavior, user journey 2 "after" phase. | Academic Affairs / System Design |
| 5 | If a course's type changes from elective to core (or vice versa), what happens to existing elective registrations for that course? Are they invalidated? Grandfathered? | Affects FR-3.4, cross-story impact on Story 24 (A4-25). | Academic Affairs |
| 6 | How is cross-listing implemented at the data level? A flag with a multi-department reference list? A junction table? Does a cross-listed course have a "primary" department and "secondary" departments? | Affects FR-5, data model, how Story 26 (A4-27) queries synchronized courses. Design decision, but ownership structure affects requirements. | System Design |
| 7 | When a course that serves as a prerequisite for other courses is deleted (or deactivated), what happens to the prerequisite links pointing to it? Remove them silently? Block the deletion? Notify owners of dependent courses? | Affects FR-6.3, deletion semantics. | Academic Affairs |
| 8 | Can credits be fractional (e.g., 1.5 credits for a half-credit course)? Or always integers? | Affects validation rule for credits field. | Academic Affairs |

---

## A4-4: Faculty Profile Management

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Who creates/updates faculty profiles? Admin only (BRD Section 4: master data = IT/Admin) or also HOD (BRD Section 4: HOD "allocates faculty to courses")? Does "allocates" include profile management or only session assignment? | Affects Actors, RBAC permissions, Security NFR. | Registrar / Academic Affairs |
| 2 | What is the faculty identifier format? Employee ID from HR? System-generated UUID? Unique system-wide or within department? | Affects FR-1.1, validation rules, uniqueness constraint. | IT / HR |
| 3 | Should faculty deletion be hard-delete or soft-delete? Same data retention concern: historical timetables reference faculty for years. Departing faculty's records need to persist in archives. | Affects FR-4, data model (active/inactive flag). | Registrar / IT |
| 4 | Can a faculty member's home department change (transfer between departments)? If yes, what happens to their existing session assignments in the old department? | Affects FR-3.2, cascading effects on scheduling. | Academic Affairs |
| 5 | Is min/max weekly load stored per-faculty (individual override), or inherited from cadre designation norms (configured in Story 31/A4-32), or both with per-faculty overriding cadre default? | Affects FR-1.1, FR-3.6, interaction with Story 31. | Academic Affairs / Compliance Officer |
| 6 | When a campus association is removed from a faculty member, and that faculty has active sessions on that campus, should the system block the removal or allow it with a warning? | Affects FR-3.5, scheduling consistency. | Academic Affairs |
| 7 | When a competency link is removed (faculty no longer qualified for a course), and that faculty is currently assigned to active sessions of that course, should the system block removal, warn, or silently allow? | Affects FR-5.4, scheduling consistency. | Academic Affairs |
| 8 | Is competency-based assignment a hard constraint (cannot assign faculty to course outside competency) or a soft warning (system warns but allows override)? AC #3 says "flags a mismatch warning" — confirming it's a warning, not a block. But should there be an option to make it a hard block per institution policy? | Affects HC constraints, scheduling engine behavior. | Academic Affairs |
| 9 | What is the full enumeration of designation values? BRD mentions: designation broadly, and "Visiting / Adjunct Faculty" as a specific role. Indian academic designations typically include: Professor, Associate Professor, Assistant Professor, Lecturer, Visiting Faculty, Adjunct Faculty, Professor Emeritus. Confirm the exact list. | Affects HC-FAC-9 allowed values. | Academic Affairs / HR |

---

## A4-5: Faculty Availability and Preference Management

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Can System Administrators create/edit availability on behalf of a faculty member (e.g., during bulk data setup), or is it strictly self-service by the faculty? | Affects Actors, RBAC permissions. | Academic Affairs / IT |
| 2 | Must availability window times align with the campus time-slot grid boundaries, or can they be arbitrary times (e.g., 2:15 PM-3:45 PM even if no slot boundary exists there)? | Affects FR-1.2 validation, interaction with Story 9 (A4-10). | System Design |
| 3 | Should "reason" for hard unavailability be free-text or selected from a predefined list (e.g., Research, Administrative, External Commitment, Personal)? A predefined list enables reporting; free-text is more flexible. | Affects FR-1.1, validation rules, reporting capability. | Academic Affairs |
| 4 | Are date-specific exceptions supported (e.g., "I'm normally unavailable Tuesday 2-4, but THIS Tuesday I'm available")? Or only recurring weekly patterns? | Affects FR-1.7, data model (needs date field for exceptions). | Academic Affairs |
| 5 | For part-time/visiting faculty, is the "available only during declared windows" model triggered automatically by designation (Visiting/Adjunct), or does the user explicitly choose between "declare blocked" vs "declare available" models? | Affects FR-3.2, RBAC logic, user experience. | Academic Affairs / System Design |
| 6 | Does "Preference weighting" (BRD 7.2) mean numeric weights (importance 1-10) that the engine uses for trade-off decisions, or simply categorical preferences (morning/afternoon)? If numeric, what scale? How does the engine compare weights across faculty? | Affects FR-2.1, data model (needs weight field), engine optimization logic (Story 10). | Academic Affairs / System Design |

---

## A4-6: Room and Lab Master Data Management

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Should the Facilities team have direct update rights on room data (e.g., updating capacity after renovation, marking rooms under maintenance)? BRD Section 11 says "Room/lab inventory data kept current by campus facilities teams" — implying they maintain it. But BRD Section 4 assigns master data to IT/Admin. | Affects Actors, RBAC permissions. | IT / Facilities / Academic Affairs |
| 2 | What is the uniqueness scope for room codes? Within building (Room 101 in Building A != Room 101 in Building B)? Within campus? System-wide? | Affects HC-ROOM-4, FR-1.1 validation. | Facilities / IT |
| 3 | Should room deletion be hard-delete or soft-delete? Historical timetables reference rooms. A decommissioned room's data must persist in archives. | Affects FR-4, data model (active/inactive flag). | IT / Registrar |
| 4 | Is "Building" a separate entity with its own CRUD (name, campus, address, coordinates for proximity) or just a text field on the room record? If separate entity: who manages it? What are its attributes? If text field: how does the proximity calculation work without structured building data? | Affects data model, FR-1.1, travel-time buffer logic (Story 34). | System Design / Facilities |
| 5 | Can a room's type change (e.g., classroom to lab after renovation)? If yes, what happens to sessions currently assigned to it that are incompatible with the new type? | Affects FR-3.5, cascading conflicts. | Facilities / Academic Affairs |
| 6 | Should equipment_tags be free-form strings (flexible but prone to typos/inconsistency) or selected from a controlled vocabulary/tag registry (consistent but requires maintaining the registry)? If controlled: who maintains the registry? | Affects FR-5.3, validation rules, matching reliability with course equipment needs. | System Design / Facilities |

---

## A4-7: Schedulable Asset Master Data Management

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | What is the uniqueness scope for asset identifiers? Within department? Campus? System-wide? And what format (serial number? institution-assigned code?)? | Affects HC-AST-3, FR-1.1 validation. | IT / Facilities |
| 2 | Is asset type/category a predefined enum (projector_set, sports_facility, instrument_kit, etc.) or free-form text? If enum: what are the values? If free-form: how to prevent inconsistency? | Affects FR-1.1 validation, search/filter reliability. | Academic Affairs / Facilities |
| 3 | Should asset deletion be hard-delete or soft-delete? Same data retention pattern as other master data entities. | Affects FR-4, data model. | IT / Registrar |
| 4 | How do the availability calendar (this document) and resource blocks (A4-8) interact? Is availability checked as: "asset available = within calendar AND not blocked"? Or does A4-8 supersede the calendar entirely? | Affects FR-5.4, scheduling engine logic. | System Design |
| 5 | Are assets ever directly assigned to sessions (e.g., "this lab session requires Portable Chemistry Kit #3"), or are they only available for blocking/reservation? If directly assigned: this document needs an assignment mechanism or the scheduling engine (Story 10) needs to know about assets. | Affects data model, scheduling engine scope, cross-story dependency. | Academic Affairs / System Design |
| 6 | Can an asset be associated with multiple campuses (shared equipment that rotates between sites), or strictly one campus? | Affects FR-1.1, data model, availability logic. | Facilities |

---

## A4-8: Resource Blocking and Availability Workflow

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Which roles can raise blocks, and which can approve them? BRD 7.3 says "Roles permitted to raise/approve blocks" but doesn't name specific roles. Can coordinators raise? Can HOD approve? Can Facilities raise maintenance blocks directly? | Affects Actors, FR-2.2, RBAC permissions. | Registrar / Academic Affairs |
| 2 | Are reason codes a predefined configurable list (Maintenance, Inspection, Breakdown, Institutional Event, External Booking) or free-form text? If configurable: who manages the list? | Affects FR-1.1, validation rules, reporting consistency. | Academic Affairs / IT |
| 3 | How is "reallocation confirmed" triggered? Does the coordinator manually review and accept each alternative? Or is it automatic once alternatives are proposed? Or does the coordinator confirm a batch of reallocations at once? | Affects FR-3.4, notification timing, user workflow. | Academic Affairs / System Design |
| 4 | Can an active block be modified (e.g., extended by 2 days, or shortened)? If yes, does modification of a hard block on published sessions re-trigger the approval workflow? | Affects FR lifecycle, potential re-approval logic. | Academic Affairs |
| 5 | Can multiple blocks overlap on the same resource at the same time (e.g., two different reasons for the same period)? Or should overlapping blocks be merged/rejected? | Affects FR-1.1 validation, data model. | System Design |
| 6 | When a soft block is overridden by the engine, who is notified? The block raiser? The coordinator? Nobody (just recorded)? | Affects FR-4.2, notification scope. | Academic Affairs |

---

## A4-9: Academic Calendar Management

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Can a semester have multiple exam windows (e.g., mid-sem in Week 8, end-sem in Week 16)? Or only one per semester? | Affects FR-3.1, data model (one-to-many vs. one-to-one). | Academic Affairs |
| 2 | Is the "orientation/induction period" a full block (no scheduling at all), or can special orientation sessions be scheduled during it? If special sessions are allowed, how are they distinguished from regular classes? | Affects HC-CAL-4, FR-4.2. | Academic Affairs |
| 3 | When a new holiday is added after sessions are already scheduled on that date, should the system: (a) only flag/report the impacted sessions, (b) auto-cancel them and notify, or (c) flag and require manual resolution before the calendar change is committed? | Affects FR-7.1 behavior, FR-2.5 workflow. | Academic Affairs / Registrar |
| 4 | Should past-semester calendars be retained indefinitely (for historical timetable context), or can they be archived/deleted after the data retention period? | Affects FR-8.4, storage, historical reporting. | Registrar / IT |
| 5 | Does the institution use only semesters, or also trimesters/quarters/summer terms? How many "terms" per year must the calendar support? | Affects FR-1.3, semester_identifier values. | Academic Affairs |
| 6 | How is "alternate Saturdays" precisely defined? "1st and 3rd Saturday of each month are working" or "odd-numbered Saturdays from semester start" or an explicit manually-curated list? | Affects FR-5.3, pattern definition format, implementability. | Academic Affairs / Registrar |
| 7 | Who manages campus-specific calendar entries? Registrar centrally for all campuses, or can each campus have a designated admin who manages their own calendar entries (holidays, working pattern)? BRD Section 4 says Registrar "defines academic calendar" but with multiple campuses, delegation may be needed. | Affects Actors, RBAC permissions, Security NFR. | Registrar / Academic Affairs |
| 8 | Should the system enforce uniqueness of calendars — at most one calendar per (campus + academic_year + semester)? Or can multiple calendar objects exist for the same scope? If unique: what happens if a Registrar tries to create a second calendar for the same campus/semester? | Affects FR-1.4, validation rules, data model. | System Design / Registrar |
| 9 | Can holidays be defined outside semester date bounds (e.g., inter-semester breaks, summer holidays)? If the system only supports holidays within semester bounds, how are breaks between semesters represented? | Affects FR-2.6, validation rule for holiday dates. | Academic Affairs |

---

## A4-10: Time-Slot Grid Configuration

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Can a campus have multiple active grids simultaneously? E.g., a "Weekday Grid" and a "Saturday Grid" with different structures? Or does one campus always have exactly one active grid? | Affects FR-1.6, data model (is_active flag or separate grids per day-type), scheduling engine grid selection logic. | Academic Affairs / System Design |
| 2 | Can the grid change mid-semester (e.g., after mid-sem exams, the afternoon schedule shifts)? Or is it fixed for the entire semester? | Affects FR-3 (when can updates happen), impact on existing sessions. | Academic Affairs |
| 3 | When a slot is removed from a grid and existing sessions occupy that slot, should the system: (a) block removal until sessions are rescheduled, (b) allow removal and flag impacted sessions, or (c) force reschedule before allowing? | Affects FR-3.2, user workflow. | Academic Affairs / System Design |
| 4 | Should old/replaced grid configurations be archived (for historical timetable context) or hard-deleted? Past timetables were built on a specific grid — understanding them requires knowing what the grid looked like at that time. | Affects FR-4.2, data model, historical reporting. | IT / Registrar |
| 5 | Can different days of the week have different slot structures within the same campus? E.g., Monday has 8 periods but Friday has only 6 (half-day)? If yes, the grid must be day-specific rather than universal. | Affects data model (SlotDefinition needs day_of_week), FR-2.2, grid complexity. | Academic Affairs |
| 6 | Are teaching slot durations restricted to exactly 60, 90, and 180 minutes (BRD 7.7 names these three), or can any duration be defined (e.g., 45 min, 120 min)? BRD 7.7 says the grid "must accommodate" those three — it doesn't say "only" those three. | Affects validation rules (restrict to enum or allow any positive duration). | Academic Affairs / System Design |

---

## Summary

| Story | Open Questions Count |
|---|---|
| A4-2: Campus Hierarchy | 4 |
| A4-3: Course Management | 8 |
| A4-4: Faculty Profile | 9 |
| A4-5: Faculty Availability | 6 |
| A4-6: Room and Lab | 6 |
| A4-7: Schedulable Asset | 6 |
| A4-8: Resource Blocking | 6 |
| A4-9: Academic Calendar | 9 |
| A4-10: Time-Slot Grid | 6 |
| **Total** | **60** |

---

## Cross-Cutting Themes (Questions That Repeat Across Stories)

These themes appear in multiple stories and should be answered once, consistently:

| Theme | Appears In | Resolution Needed |
|---|---|---|
| **Hard-delete vs Soft-delete** | A4-2 #3, A4-3 #3, A4-4 #3, A4-6 #3, A4-7 #3, A4-10 #4 | One institutional decision applies to all entities. |
| **Who mutates master data (Admin only vs Coordinator/HOD)** | A4-2 #1, A4-3 #1, A4-4 #1, A4-5 #1, A4-6 #1, A4-9 #7 | Confirm RBAC model for master data mutation per role. |
| **Code/identifier uniqueness scope** | A4-2 #2 (hierarchy codes), A4-3 #2 (course codes), A4-4 #2 (faculty ID), A4-6 #2 (room codes), A4-7 #1 (asset IDs) | Confirm scope per entity type. |
| **Code mutability after creation** | A4-2 #2, A4-3 #2 | One decision for all code fields. |
| **Impact of master data changes on existing sessions** | A4-3 #4 (L-T-P change), A4-5 #4 (availability change), A4-6 #5 (type change), A4-9 #3 (holiday added), A4-10 #3 (slot removed) | Consistent "flag but don't auto-modify" pattern, or different per case? |
| **Controlled vocabulary vs free-form for tags/categories** | A4-5 #3 (reasons), A4-6 #6 (equipment tags), A4-7 #2 (asset types), A4-8 #2 (reason codes) | One approach for all or per-field? |
