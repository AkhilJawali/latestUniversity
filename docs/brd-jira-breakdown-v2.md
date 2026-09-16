# UTMS — BRD to Jira Breakdown (v2)

## Epic

**Name:** University Timetable Management System (UTMS)

**Goal:** Build a University Timetable Management System that serves as the single source of truth for all academic scheduling — class/lecture timetables, examination timetables, room and lab allocation, and faculty workload planning — across multiple campuses and departments. The system provides semi-automated timetable generation with constraint-based scheduling, real-time conflict detection, multi-level approval workflows, and role-based access for all stakeholders.

**BRD Reference:** Full document — Sections 1–17

---

## User Stories

---

### Story 1: Campus Hierarchy Master Data Management

**As a** System Administrator,
**I want** to manage the hierarchical master data (Campus → Department → Program → Batch/Section) with full CRUD operations and referential integrity enforcement,
**so that** the institution's organizational structure is accurately represented as the foundation for all scheduling activities.

**Story Points:** 5

**BRD Requirements:** 6.1 — "Maintain hierarchical master data: Campus → Department → Program → Batch/Section"; 6.1 — "Batch/section master: strength, program, elective basket enrolled"

**Acceptance Criteria:**

1. **Given** a user with admin role, **When** they create a campus with name, code, and location, **Then** the campus is persisted and visible in the hierarchy.
2. **Given** an existing campus, **When** a department is created under it with a valid campus reference, **Then** the department appears as a child of that campus.
3. **Given** a department with active programs referencing it, **When** a user attempts to delete the department, **Then** the system rejects the deletion with a referential integrity error message.
4. **Given** a batch/section, **When** it is created with strength, program reference, and elective basket enrolled, **Then** all fields are persisted and the batch is correctly associated.
5. **Given** an invalid parent reference (e.g., non-existent campus ID for a department), **When** creation is attempted, **Then** the system returns a validation error and does not create the entity.
6. **Given** a hierarchy, **When** queried, **Then** the full tree (Campus → Department → Program → Batch → Section) is traversable.

---

### Story 2: Course Management

**As a** Department Coordinator,
**I want** to manage courses with their credit structure (L-T-P split), type classification (core/elective/audit), prerequisite references, and equipment tags,
**so that** the scheduling engine has accurate course data for session generation and room matching.

**Story Points:** 5

**BRD Requirements:** 6.1 — "Course master: credit hours, contact hours (L-T-P split), course type (core/elective/audit), prerequisite courses"; 7.1 — "Course credit structure (L-T-P)"; 7.1 — "Core vs. elective classification"; 7.1 — "Prerequisite mapping"

**Acceptance Criteria:**

1. **Given** a coordinator role, **When** they create a course with L-T-P split (e.g., 3-1-2), credits, and type (core/elective/audit), **Then** the course is persisted with all fields correctly stored.
2. **Given** a course with a prerequisite reference, **When** the referenced prerequisite course does not exist in the system, **Then** creation is rejected with a validation error.
3. **Given** a course, **When** equipment tags are specified (e.g., ["projector", "chemistry_fume_hood"]), **Then** they are stored and retrievable for room-matching during scheduling.
4. **Given** a search by course type, **When** "elective" is filtered, **Then** only elective-classified courses are returned.
5. **Given** a course marked as cross-listed across departments, **When** viewed from either department, **Then** it appears with the shared/cross-listed designation (BRD 7.1: "Cross-listed / cross-department courses").

---

### Story 3: Faculty Profile Management

**As an** HOD,
**I want** to manage faculty profiles including designation, home department, qualifications, subject competency list, and multi-campus associations,
**so that** faculty can be correctly assigned to courses they are qualified to teach across campuses.

**Story Points:** 5

**BRD Requirements:** 6.1 — "Faculty master: qualification, designation, home department, max/min weekly teaching load, subjects competent to teach, campus(es) of association"; 7.2 — "Subject competency mapping"; 7.2 — "Multi-department / multi-campus load"

**Acceptance Criteria:**

1. **Given** an HOD role, **When** they create a faculty profile with designation, home department, and subject competency list, **Then** the profile is persisted with all fields.
2. **Given** a faculty member, **When** campus associations are configured (e.g., associated with Campus A and Campus B), **Then** both associations are stored and visible.
3. **Given** a faculty member's competency list, **When** a course assignment is attempted for a course not in their competency list, **Then** the system flags a mismatch warning.
4. **Given** a faculty profile, **When** min/max weekly teaching load is configured per cadre designation, **Then** the values are stored and available for workload validation.
5. **Given** a search for faculty by subject competency, **When** "Data Structures" is searched, **Then** all faculty with that competency are returned.

---

### Story 4: Faculty Availability and Preference Management

**As a** Faculty member,
**I want** to declare my availability windows (hard unavailability and soft time preferences),
**so that** the scheduling engine respects my constraints and preferences when assigning sessions.

**Story Points:** 5

**BRD Requirements:** 6.5 — "Capture faculty time-preferences/unavailability as soft or hard constraints"; 7.2 — "Availability / blocked slots (Research time, administrative duties, part-time/visiting constraints)"; 7.2 — "Preference weighting (preferred time-of-day, consecutive vs. spread sessions)"

**Acceptance Criteria:**

1. **Given** a faculty member, **When** they declare a hard unavailability window (e.g., Tuesday 2–4 PM for administrative duties), **Then** it is stored as a hard constraint that the scheduling engine must never violate.
2. **Given** a faculty member, **When** they set a soft time preference (e.g., prefers morning slots, prefers spread sessions over consecutive), **Then** it is stored as a soft constraint that the engine will try to satisfy.
3. **Given** a hard unavailability window, **When** the scheduling engine runs, **Then** no session is placed in that window for that faculty member.
4. **Given** a part-time/visiting faculty member, **When** their limited availability is declared, **Then** only their declared available windows are considered for scheduling.
5. **Given** a faculty member updates their availability, **When** the change is saved, **Then** it is reflected in subsequent scheduling runs and any existing conflicts are flagged.

---

### Story 5: Room and Lab Master Data Management

**As a** System Administrator,
**I want** to manage rooms and labs with capacity, type (classroom/lab/seminar hall/auditorium), equipment tags, and building/floor location,
**so that** the scheduling engine can allocate appropriate spaces based on batch size, course requirements, and proximity.

**Story Points:** 5

**BRD Requirements:** 6.1 — "Room/Lab master: capacity, room type, equipment tags, campus/building/floor location"; 7.3 — "Room capacity (must be ≥ batch/group strength)"; 7.3 — "Room type & equipment (Lab vs. classroom vs. seminar hall vs. auditorium; specialised equipment/software needs)"

**Acceptance Criteria:**

1. **Given** an admin role, **When** they create a room with capacity, type (lab/classroom/seminar hall/auditorium), equipment tags, and building/floor/campus location, **Then** all attributes are persisted.
2. **Given** a room with capacity 60, **When** a scheduling check is performed against a batch of strength 70, **Then** the system identifies the capacity mismatch.
3. **Given** a search by equipment tag, **When** "computer_lab" is queried, **Then** all rooms with that equipment tag are returned.
4. **Given** rooms in different buildings, **When** building/floor location is stored, **Then** the system can use it for proximity-based allocation decisions.
5. **Given** a room update (e.g., capacity change after renovation), **When** saved, **Then** subsequent scheduling uses the updated capacity.

---

### Story 6: Schedulable Asset Master Data Management

**As a** System Administrator,
**I want** to manage non-room schedulable assets (movable equipment kits, projector sets, sports facilities) with owning department, campus, and an availability/blocking calendar per asset,
**so that** the scheduling engine and blocking workflow can reference a complete inventory of all schedulable resources.

**Story Points:** 5

**BRD Requirements:** 6.1 — "Asset / schedulable-resource master: non-room schedulable assets with owning department, campus, and an availability/blocking calendar per resource"; 7.3 — "Asset-level blocking scope: Hard/soft blocking applies to any schedulable asset"

**Acceptance Criteria:**

1. **Given** an admin role, **When** they create a schedulable asset with name, type, owning department, and campus, **Then** the asset is persisted and available for blocking and scheduling operations.
2. **Given** a schedulable asset, **When** an availability calendar is configured (available windows, maintenance periods), **Then** the calendar is stored per asset.
3. **Given** an asset assigned to Department A, **When** Department B needs to use it, **Then** the ownership is visible and cross-department booking rules apply.
4. **Given** a search for assets by type or campus, **When** queried, **Then** matching assets are returned with their availability status.
5. **Given** the blocking workflow (Story 7), **When** a block is raised on an asset, **Then** the asset's record exists and can be referenced (data dependency satisfied).

---

### Story 7: Resource Blocking and Availability Workflow

**As a** Department Coordinator,
**I want** to raise hard or soft blocks on rooms, labs, and schedulable assets (for maintenance, events, or tentative holds) with an approval workflow when blocks impact published sessions,
**so that** the scheduling engine respects resource unavailability and impacted sessions are proactively managed.

**Story Points:** 8

**BRD Requirements:** 6.4 — "Capture room, lab, and asset availability as hard or soft constraints"; 6.4 — "Apply the same hard/soft blocking model to any schedulable asset, not only rooms and labs"; 6.4 — "Provide a resource-blocking workflow with approval for blocks that impact already-published sessions"; 6.4 — "On activation of a hard block, the system shall automatically re-run conflict detection, list impacted sessions, propose alternative rooms/slots, and notify affected users once reallocation is confirmed"; 6.4 — "Maintain a complete audit trail for every block and report block frequency and duration per resource"; 7.3 — "Maintenance / blackout windows (hard blocks)"; 7.3 — "Soft resource holds"; 7.3 — "Block authorisation & workflow (Roles permitted to raise/approve blocks, reason codes, recurrence patterns, and approval rules)"

**Acceptance Criteria:**

1. **Given** a coordinator raises a hard block on a room for a date range with a reason code, **When** submitted, **Then** the room becomes unschedulable for that period after approval (if required).
2. **Given** a soft block (tentative hold) on a resource, **When** the scheduling engine evaluates that slot, **Then** it avoids the slot by default but may override with a recorded justification and explicit warning.
3. **Given** a hard block raised on a resource with already-published sessions in that window, **When** the block is submitted, **Then** it requires approval from an authorized role before activation.
4. **Given** an approved hard block impacting published sessions, **When** activated, **Then** the system automatically identifies impacted sessions, proposes alternative rooms/slots, and notifies affected users after reallocation.
5. **Given** any block action (raise, approve, release), **When** completed, **Then** a full audit trail entry is created (who, when, why, resource, duration, reason code).
6. **Given** reporting needs, **When** block frequency and duration reports are requested per resource, **Then** the system generates accurate reports.
7. **Given** configurable recurrence patterns for blocks, **When** a recurring block is created (e.g., every Monday 9–11 AM for maintenance), **Then** it applies to all matching dates.

---

### Story 8: Academic Calendar Management

**As a** Registrar,
**I want** to define and manage the academic calendar including semester start/end dates, holidays, exam windows, orientation/induction periods, and working-day patterns with per-campus variations,
**so that** the scheduling engine correctly excludes non-working days and respects term boundaries.

**Story Points:** 5

**BRD Requirements:** 6.1 — "Academic calendar: semester start/end dates, holidays, exam windows, orientation/induction periods, per-campus calendar variation"; 7.4 — "Academic calendar (Semester dates, holidays, exam windows, induction/orientation blocks)"; 7.4 — "Working days pattern (5-day/6-day week, alternate Saturdays, campus-specific variations)"; 6.12 — "Support campus-specific calendars, time-slot grids, and holiday lists within one unified system"

**Acceptance Criteria:**

1. **Given** a registrar role, **When** they define a semester with start/end dates, holidays, exam windows, and orientation periods, **Then** the calendar is persisted.
2. **Given** a campus-specific holiday (e.g., regional festival), **When** added to that campus's calendar, **Then** it only affects scheduling for that campus, not others.
3. **Given** institution-wide holidays, **When** defined, **Then** they apply to all campuses simultaneously.
4. **Given** a working-day pattern (e.g., alternate Saturdays off), **When** configured per campus, **Then** the scheduling engine excludes non-working days for that campus.
5. **Given** a change to the calendar after timetable generation (e.g., adding a new holiday), **When** saved, **Then** the system flags sessions already scheduled on the newly added non-working day.
6. **Given** exam windows in the calendar, **When** the regular class scheduling engine runs, **Then** no lectures are placed during exam windows.

---

### Story 9: Time-Slot Grid Configuration

**As a** System Administrator,
**I want** to configure time-slot grids per campus with mixed slot durations (60, 90, 180 minutes), break/lunch windows, and period definitions,
**so that** each campus has its own daily schedule structure accommodating lectures, tutorials, and practicals simultaneously.

**Story Points:** 5

**BRD Requirements:** 6.2 — "Support configurable time-slot grids per campus"; 7.4 — "Time-slot grid (Period duration, number of periods/day, break/lunch windows, configurable per campus)"; 7.7 — "Mixed slot durations: A single time-slot grid must accommodate 1-hour lecture, 1.5-hour, and 3-hour practical slot lengths simultaneously"

**Acceptance Criteria:**

1. **Given** an admin role, **When** they define a time-slot grid for a campus with mixed durations (60 min lectures, 90 min tutorials, 180 min labs), **Then** the grid is persisted and all durations coexist without overlap.
2. **Given** a time-slot grid, **When** two slot definitions would overlap in time, **Then** the system rejects the configuration with a validation error.
3. **Given** break/lunch windows defined in the grid, **When** the scheduling engine runs, **Then** no sessions are placed during those windows.
4. **Given** two campuses with different grids (e.g., Campus A starts at 8:00, Campus B at 9:00), **When** scheduling runs, **Then** each campus uses its own grid independently.
5. **Given** a 180-minute lab slot defined in the grid, **When** a 3-hour lab session is scheduled, **Then** it occupies exactly one 180-minute slot cleanly.

---

### Story 10: Timetable Generation Engine

**As a** Department Coordinator,
**I want** to trigger auto-generation of a first-draft weekly timetable for my department based on all configured master data, hard constraints, and soft constraints,
**so that** I have a valid starting point to review and refine rather than building from scratch.

**Story Points:** 8

**BRD Requirements:** 6.10 — "Engine shall auto-generate a first-draft timetable using a constraint-based algorithm considering hard constraints and soft constraints"; 6.2 — "System shall generate a proposed weekly timetable per batch/section based on course credit structure, faculty assignment, and room availability"; 6.10 — "Provide a feasibility/quality score and a list of unresolved soft-constraint violations for each generated draft"; 8 — "Auto-generate a full department timetable within 2 minutes"; 7.7 — "Day-pattern saturation balancing"; 7.7 — "Institution-level common slots (CCC / UWE)"; 7.3 — "Location / transit buffer: Minimise back-to-back session travel time between distant buildings/campuses" (room proximity as soft constraint); 7.2 — "Preference weighting: Soft preferences such as preferred time-of-day, consecutive vs. spread sessions" (gap minimization as soft constraint)

**Soft constraints explicitly optimized by the engine (BRD 6.10, 7.2, 7.3):**
- Faculty time preferences (preferred time-of-day, consecutive vs. spread)
- Soft resource holds (tentative reservations the engine avoids by default)
- Room proximity (minimize travel between distant buildings for back-to-back sessions)
- Gap minimization (reduce idle gaps in a faculty's or batch's daily schedule)

**Acceptance Criteria:**

1. **Given** master data is configured (courses, faculty, rooms, time-slots, calendar, constraints), **When** the coordinator triggers generation for their department, **Then** the system produces a draft timetable within 2 minutes (BRD Section 8 NFR).
2. **Given** a generated draft, **When** presented, **Then** it includes a feasibility score and a quality score indicating overall constraint satisfaction levels.
3. **Given** hard constraints (faculty double-booking, room double-booking, batch clashes, capacity, hard blocks, faculty max daily/weekly load, faculty max consecutive teaching hours), **When** the engine completes successfully, **Then** zero hard constraints are violated in the output.
4. **Given** soft constraints that cannot all be satisfied, **When** the engine completes, **Then** it provides a list of unresolved soft-constraint violations with descriptions of what was relaxed — including which of the four named soft-constraint types (preferences, soft holds, proximity, gaps) were affected.
5. **Given** institution-level common slots (CCC/UWE), **When** the engine runs, **Then** those slots are treated as hard constraints placed before department-level scheduling (BRD 7.7).
6. **Given** day-pattern saturation on preferred days, **When** the engine runs, **Then** it balances load across alternative patterns rather than compounding clashes on saturated days (BRD 7.7).
7. **Given** back-to-back sessions for a faculty member or batch, **When** the engine assigns rooms, **Then** it prefers rooms in proximity (same building/floor) to minimize travel between consecutive sessions (BRD 7.3 — room proximity soft constraint).
8. **Given** a faculty member's or batch's daily schedule, **When** the engine assigns sessions, **Then** it minimizes idle gaps between sessions where possible (gap minimization soft constraint).

---

### Story 11: Timetable Generation — Timeout and Infeasibility Handling

**As a** Department Coordinator,
**I want** the system to handle generation timeouts by returning the best partial solution found so far, and to clearly report when the problem is infeasible with an explanation of which constraints conflict,
**so that** I am never stuck waiting indefinitely and always have actionable information to adjust inputs.

**Story Points:** 5

**BRD Requirements:** 8 — "Auto-generate a full department timetable within 2 minutes" (implies: what happens when it CAN'T complete in 2 minutes?); 6.10 — (implied) engine must handle cases where no valid solution exists

**Acceptance Criteria:**

1. **Given** a generation request, **When** the engine exceeds the time limit (BRD: 2 minutes), **Then** it returns the best partial solution found so far with a clear indication that it is incomplete.
2. **Given** an infeasible constraint set (no valid solution exists), **When** the engine detects this, **Then** it reports infeasibility with an explanation of which constraints are in conflict.
3. **Given** a partial solution returned on timeout, **When** the coordinator views it, **Then** they can see which sessions were successfully placed and which remain unplaced.
4. **Given** infeasibility, **When** conflicting constraints are reported, **Then** the coordinator can adjust inputs (relax a constraint, add a room, etc.) and re-run.
5. **Given** a generation in progress, **When** the coordinator checks status, **Then** progress information is available (not a silent black box).

---

### Story 12: Fortnightly and Alternate-Week Scheduling Patterns

**As a** Department Coordinator,
**I want** to define sessions with fortnightly or alternate-week recurrence patterns (e.g., Lab Group A on Week 1, Lab Group B on Week 2),
**so that** the system supports scheduling patterns beyond simple weekly repetition as required for split labs and special courses.

**Story Points:** 5

**BRD Requirements:** 6.2 — "Support recurring weekly patterns as well as fortnightly/alternate-week patterns"

**Acceptance Criteria:**

1. **Given** a session, **When** a fortnightly/alternate-week pattern is configured (e.g., "every other Monday"), **Then** the session repeats on that pattern rather than every week.
2. **Given** two sub-groups of a lab batch, **When** alternate-week scheduling is applied, **Then** Group A occupies Week 1 and Group B occupies Week 2 in the same slot without conflict.
3. **Given** a fortnightly session, **When** conflict detection runs, **Then** it only checks for conflicts on the weeks the session actually occurs (not every week).
4. **Given** a fortnightly session, **When** the published timetable is viewed by a student/faculty, **Then** the recurrence pattern is clearly indicated (which weeks it occurs).
5. **Given** a calendar feed for a user with fortnightly sessions, **When** exported, **Then** events appear only on the correct alternating weeks.

---

### Story 13: Locked Slot Preservation and Partial Re-Generation

**As a** Department Coordinator,
**I want** to lock specific sessions in fixed slots before running the engine, and re-run generation on a subset of the timetable without disturbing already-approved or locked sections,
**so that** I retain control over pre-decided placements and can iteratively refine parts of the schedule.

**Story Points:** 5

**BRD Requirements:** 6.2 — "Allow department coordinators to lock specific sessions before running the auto-suggestion engine"; 6.10 — "Support re-running the engine on a subset of the timetable without disturbing already-approved sections"

**Acceptance Criteria:**

1. **Given** a coordinator locks 3 sessions in specific slots, **When** the engine runs, **Then** those 3 sessions remain exactly in their locked positions — never moved.
2. **Given** an already-approved section of the timetable, **When** partial re-generation is triggered for a different section, **Then** the approved section is completely untouched.
3. **Given** a subset of batches selected for re-generation, **When** the engine runs, **Then** only those batches' sessions are regenerated; all others remain fixed.
4. **Given** locked slots that make the problem infeasible, **When** the engine cannot find a solution, **Then** it reports infeasibility indicating the locks are part of the conflict.
5. **Given** a combination of locked slots and approved sections, **When** re-generation runs, **Then** both locked slots AND approved sections are preserved.

---

### Story 14: Drag-and-Drop Timetable Editor with Real-Time Conflict Feedback

**As a** Department Coordinator,
**I want** a visual grid-based timetable editor where I can drag-and-drop sessions to different slots and receive real-time conflict feedback within 2 seconds,
**so that** I can intuitively review and refine the auto-generated draft before submitting for approval.

**Story Points:** 8

**BRD Requirements:** 6.2 — "Provide drag-and-drop manual adjustment of the auto-generated draft with real-time conflict flagging"; 6.10 — "Coordinators/HODs shall be able to review, override, and manually re-arrange the proposed draft before submission for approval"; 8 — "Drag-and-drop timetable editor" (Usability NFR); 8 — "Conflict checks return in under 2 seconds" (Performance NFR)

**Acceptance Criteria:**

1. **Given** a generated or in-progress draft timetable, **When** the coordinator opens the editor, **Then** sessions are displayed in a grid-based weekly view with day/time axes.
2. **Given** a session being dragged to a new slot, **When** the drop would cause a conflict (faculty/room/batch clash), **Then** a conflict indicator appears within 2 seconds (BRD Section 8).
3. **Given** a conflict is flagged, **When** the coordinator views details, **Then** ranked alternative slot suggestions are displayed (BRD 6.9).
4. **Given** a valid drop (no conflicts), **When** the session is placed, **Then** it is saved to the draft.
5. **Given** concurrent coordinators editing, **When** two users attempt conflicting edits on the same draft, **Then** the system prevents silent overwrites (BRD Section 8: 50+ concurrent coordinators).
6. **Given** accessibility requirements, **When** a user cannot use drag-and-drop (keyboard-only), **Then** an accessible alternative for session placement is provided.

---

### Story 15: Real-Time Conflict Detection Engine

**As a** Department Coordinator,
**I want** the system to detect all scheduling conflicts in real-time — faculty double-booking, room double-booking, student/batch clashes, capacity mismatches, daily/weekly hour limits exceeded, hard-block violations, and travel-time violations,
**so that** I am immediately aware of issues and can resolve them before submission.

**Story Points:** 8

**BRD Requirements:** 6.9 — "Real-time detection of faculty double-booking, room double-booking, student/batch clashes, and exceeding max daily/weekly teaching hours"; 8 — "Conflict checks return in under 2 seconds"; 7.3 — "Room capacity must be ≥ batch/group strength"; 7.3 — "Location / transit buffer"; 7.4 — "max consecutive teaching hours for faculty"; 7.1 — "Prerequisite mapping: Ensures dependent courses are not scheduled in conflicting sequence across semesters"

**This story OWNS all conflict-type definitions.** Other stories (14, 10, 17) consume conflict detection.

**Acceptance Criteria:**

1. **Given** a session placement, **When** it would double-book a faculty member in the same time slot, **Then** the conflict is detected and reported within 2 seconds.
2. **Given** a session placement, **When** it would double-book a room, **Then** the room clash is detected and reported.
3. **Given** a batch with core and elective courses, **When** a session clashes with a student's registered elective, **Then** the batch/student clash is flagged.
4. **Given** a faculty member at or exceeding max daily/weekly teaching hours (per configured norms), **When** another session is assigned, **Then** the workload limit violation is flagged.
5. **Given** a room with capacity less than the batch strength, **When** the session is placed there, **Then** the capacity mismatch is flagged.
6. **Given** a faculty member with back-to-back sessions on different campuses, **When** the travel-time buffer would be violated, **Then** the travel-time conflict is flagged.
7. **Given** a hard-block on a resource, **When** a session is placed in the blocked window, **Then** the hard-block violation is detected.
8. **Given** a faculty member with sessions exceeding the max consecutive teaching hours limit (e.g., 4 back-to-back hours when the configured limit is 3), **When** detected, **Then** the consecutive-hours violation is flagged as a distinct conflict type — separate from the daily/weekly cap (BRD 7.4: "max consecutive teaching hours for faculty").
9. **Given** two courses where one is a prerequisite of the other and both are offered to the same batch in the same semester, **When** the schedule places them in a conflicting sequence (e.g., the dependent course before the prerequisite completes), **Then** the system flags the prerequisite sequencing conflict. **[STAKEHOLDER CONFIRMATION NEEDED: BRD 7.1 says "not scheduled in conflicting sequence across semesters" — this AC interprets it as a within-semester scheduling check. If the BRD intent is a catalog/curriculum-level check (prerequisite must be offered in an earlier semester), this AC should be rewritten as a master-data validation rather than a real-time conflict detection. Confirm with Registrar/Academic Affairs.]**

---

### Story 16: Alternative Slot Suggestions on Conflict

**As a** Department Coordinator,
**I want** ranked alternative-slot suggestions whenever a conflict is detected,
**so that** I can quickly resolve conflicts by choosing a feasible alternative rather than searching manually.

**Story Points:** 3

**BRD Requirements:** 6.9 — "Provide ranked alternative-slot suggestions when a conflict is detected"

**Acceptance Criteria:**

1. **Given** a detected conflict, **When** the coordinator requests alternatives, **Then** the system provides a ranked list of feasible alternative slots.
2. **Given** the ranked list, **When** alternatives are ordered, **Then** they are sorted by suitability/feasibility (fewest constraint violations, best fit).
3. **Given** an alternative is selected, **When** the coordinator confirms, **Then** the session moves to the new slot and all conflicts are re-checked.
4. **Given** no feasible alternatives exist, **When** queried, **Then** the system clearly reports that no conflict-free slot is available and suggests which constraint to relax.

---

### Story 17: Conflict Log Persistence and Reporting

**As a** Registrar,
**I want** all detected conflicts and their resolutions to be logged persistently with type, affected entities, resolution action, and timestamp,
**so that** I can review patterns, track resolution turnaround, and continuously improve scheduling quality.

**Story Points:** 3

**BRD Requirements:** 6.9 — "Maintain a conflict log for audit and continuous improvement"; 6.16 — "Conflict-log and resolution-turnaround reports"

**Acceptance Criteria:**

1. **Given** a conflict is detected, **When** logged, **Then** the entry includes conflict type, affected entities (faculty/room/batch), severity, and timestamp.
2. **Given** a conflict is resolved, **When** the resolution is recorded, **Then** it includes the resolution action, who resolved it, and resolution timestamp.
3. **Given** a date range, **When** a conflict-log report is requested, **Then** all conflicts within that period are returned with resolution status.
4. **Given** reporting needs, **When** conflict frequency by type and resolution turnaround time are queried, **Then** accurate aggregated data is presented.

---

### Story 18: Multi-Level Approval Workflow

**As an** HOD,
**I want** a configurable multi-level approval workflow (Coordinator → HOD → Dean/Registrar) with review comments, rejection with reason, and full audit trail,
**so that** timetable drafts are properly reviewed at each level before publication.

**Story Points:** 8

**BRD Requirements:** 6.8 — "Configurable multi-level workflow: Department Coordinator drafts → HOD reviews/approves → Dean/Registrar final sign-off"; 6.8 — "Support review comments, rejection with reason"; 6.8 — "Maintain a full approval audit trail"

**Acceptance Criteria:**

1. **Given** a coordinator submits a draft, **When** it enters the workflow, **Then** it moves to "under review" at the HOD level and is visible to the HOD.
2. **Given** an HOD approving a draft, **When** approved, **Then** it progresses to the next level (Dean/Registrar) or publishes if final level.
3. **Given** a reviewer rejecting a draft, **When** they provide a rejection reason and review comments, **Then** the draft returns to the previous level with feedback visible to the submitter.
4. **Given** any approval/rejection action, **When** it occurs, **Then** a full audit trail entry is created (who, when, action, comments, level).
5. **Given** the pipeline is configurable, **When** the institution changes the number or order of approval levels, **Then** the workflow adapts without code changes [configurable, per BRD 6.8].
6. **Given** a draft in approval, **When** checked, **Then** its current status and level are visible to all relevant stakeholders.

---

### Story 19: Version Comparison Between Draft Iterations

**As an** HOD,
**I want** to compare any two versions of a timetable draft side-by-side to see what changed between iterations,
**so that** I can efficiently review what the coordinator modified after my feedback.

**Story Points:** 5

**BRD Requirements:** 6.8 — "version comparison between draft iterations"

**Acceptance Criteria:**

1. **Given** a draft with multiple iterations (v1, v2, v3), **When** the reviewer selects two versions, **Then** a diff/comparison view shows what changed (added, removed, moved sessions).
2. **Given** the comparison view, **When** a session was moved from Slot A to Slot B, **Then** the change is clearly highlighted.
3. **Given** the comparison view, **When** new sessions were added or removed, **Then** additions and removals are distinctly shown.
4. **Given** any draft submission, **When** it enters the workflow, **Then** the previous version is stored for future comparison.

---

### Story 20: Timetable Publication and Notification Trigger

**As a** Registrar,
**I want** approved timetables to transition to "published" status with automatic notifications to all affected faculty and students and calendar feed refresh,
**so that** everyone has immediate access to the finalized schedule through their preferred channel.

**Story Points:** 5

**BRD Requirements:** 6.8 — "final sign-off" (leads to publication); 6.13 — "Automated notifications (email/SMS/in-app) to affected faculty and students on publication"; 6.14 — (calendar feeds update on publication)

**Acceptance Criteria:**

1. **Given** a draft receives final approval at the last workflow level, **When** published, **Then** the timetable status changes to "published" and becomes the active schedule.
2. **Given** publication, **When** affected faculty are identified, **Then** each receives a notification via their preferred channel.
3. **Given** publication, **When** affected students are identified, **Then** each receives a notification about their updated schedule.
4. **Given** publication, **When** calendar feeds exist for affected users, **Then** the feeds are refreshed to reflect the new schedule.
5. **Given** a published timetable already exists, **When** a new version is published, **Then** the previous published version is archived (for historical comparison per BRD 6.16).

---

### Story 21: Exam Timetable Generation

**As an** Examination Controller,
**I want** to generate exam schedules for all exam types (internal/mid-semester, semester-end, and supplementary/re-exams) factoring minimum gaps between exams per student/batch, room seating capacity, and invigilator availability,
**so that** exam scheduling is conflict-free, fair, and supports all institutional exam windows.

**Story Points:** 8

**BRD Requirements:** 6.3 — "Generate exam schedules factoring minimum gap between two exams for a student/batch, room seating capacity, and invigilator availability"; BRD 3.1 — "Examination timetable generation (internal, semester-end, supplementary/re-exams)"; 7.4 — "Minimum gap rules (Minimum gap between two exams for the same student/batch)"; 6.3 — "Flag and prevent exam clashes for students registered in cross-department electives"

**Acceptance Criteria:**

1. **Given** exam parameters (courses, batches, rooms, exam window from calendar), **When** generation is triggered for a specific exam type (internal, semester-end, or supplementary), **Then** a draft exam schedule is produced for that type within its configured window.
2. **Given** a student registered in multiple courses, **When** the schedule is generated, **Then** a configurable minimum gap [TBD — confirm with stakeholder] is maintained between their exams.
3. **Given** an exam requiring 100 seats, **When** rooms are allocated, **Then** the total allocated seating meets or exceeds 100.
4. **Given** students registered in cross-department electives, **When** the schedule is generated, **Then** no exam clashes occur for those students (BRD 6.3).
5. **Given** invigilator availability (including declared leave/unavailability from faculty profiles), **When** the schedule allocates invigilators, **Then** only available faculty are assigned.
6. **Given** the three exam types, **When** each is generated, **Then** it uses its own configured exam window from the academic calendar.

---

### Story 22: Exam Seating Plan and Invigilation Roster

**As an** Examination Controller,
**I want** to generate seating plans per exam room and invigilation duty rosters with equitable distribution across faculty,
**so that** exam logistics are automated and duty allocation is fair.

**Story Points:** 5

**BRD Requirements:** 6.3 — "Support seating-plan generation and invigilation duty roster generation with equitable distribution across faculty"; 7.6 — "Invigilation duty norms: Equitable distribution rules for exam invigilation duties"

**Acceptance Criteria:**

1. **Given** an exam with assigned rooms, **When** seating plan generation is triggered, **Then** students are allocated to specific seats within room capacity limits.
2. **Given** invigilation duties to assign, **When** the roster is generated, **Then** duties are distributed equitably across eligible faculty (variance within acceptable bounds across the semester).
3. **Given** a faculty member with a declared hard unavailability window overlapping an exam slot, **When** roster generation runs, **Then** that faculty member is excluded from duty in that slot.
4. **Given** a faculty member already assigned to invigilate at the same time slot, **When** another exam needs invigilation, **Then** they are not double-assigned.
5. **Given** a generated roster, **When** the exam controller reviews it, **Then** they can manually adjust before finalizing.

---

### Story 23: Lab and Practical Session Scheduling

**As a** Department Coordinator,
**I want** the system to handle batch-splitting for lab/practical sessions, allocate contiguous time blocks (2–3 periods), match equipment requirements, and respect lab technician/support-staff availability as a hard constraint,
**so that** practical sessions are scheduled within compressed windows with all resource constraints satisfied.

**Story Points:** 8

**BRD Requirements:** 6.6 — "Support batch-splitting for practical sessions with corresponding multi-slot, multi-room allocation"; 6.6 — "Support block scheduling for labs requiring 2–3 contiguous periods"; 6.6 — "Track lab-specific equipment/software prerequisites for allocation matching"; 7.7 — "Compressed practical windows: Practical/lab sessions are compressed into a narrow afternoon window; the engine must optimise lab-group rotation and room usage within that window"; 7.7 — "Support-staff availability bounds: Lab sessions are bounded by lab technician/support-staff working hours and treated as a hard constraint on lab slot placement"

**Acceptance Criteria:**

1. **Given** a batch of 60 students needing a lab session, **When** batch-splitting is configured for groups of 30, **Then** two sub-groups are created with separate lab slots allocated.
2. **Given** a lab requiring 3 contiguous periods (block scheduling), **When** the engine schedules it, **Then** all 3 periods are adjacent with no gaps between them.
3. **Given** a lab requiring specific equipment (e.g., "chemistry_fume_hood"), **When** a room is allocated, **Then** the room has matching equipment tags.
4. **Given** lab technician/support-staff working hours (e.g., end at 5 PM), **When** the engine schedules labs, **Then** no lab session extends beyond those hours — treated as a hard constraint (BRD 7.7).
5. **Given** compressed practical windows (e.g., 2 PM–5 PM), **When** multiple lab groups need scheduling, **Then** the engine optimizes rotation and room usage within that window (BRD 7.7).
6. **Given** a sub-group assigned to a lab room at a specific time, **When** another sub-group of the same batch needs scheduling at the same time, **Then** a different room is allocated (no room collision).

---

### Story 24: Elective Registration with Clash Prevention

**As a** Student,
**I want** to register for elective courses during the registration window and be prevented from registering if the elective clashes with my core course schedule,
**so that** my personal timetable is conflict-free and I can see real-time seat availability.

**Story Points:** 8

**BRD Requirements:** 6.7 — "Support student elective basket registration and generate elective-group timetables that avoid clashes with each student's core courses"; 6.17 — "Elective registration window with real-time seat-availability display"; 7.5 — "Elective basket enrolment (min/max enrolment per elective)"

**Acceptance Criteria:**

1. **Given** a student viewing available electives during the registration window, **When** they select one, **Then** the system checks for time clashes with their core course schedule.
2. **Given** a time clash exists between the elective and a core course, **When** registration is attempted, **Then** it is rejected with a clear explanation of the conflict.
3. **Given** no clash and seats available, **When** registration is submitted, **Then** the student is enrolled and their personal timetable updates immediately.
4. **Given** real-time seat availability, **When** the student views electives, **Then** current available seats per section are displayed.
5. **Given** an elective at maximum capacity, **When** registration is attempted, **Then** the student is placed on a waitlist automatically (BRD 6.7).
6. **Given** a minimum enrolment threshold, **When** an elective is below minimum by a configurable deadline, **Then** the coordinator receives an alert (BRD 6.7).

---

### Story 25: Waitlist Management and Elective Re-Balancing

**As a** Department Coordinator,
**I want** automatic waitlist management for oversubscribed electives and alerts when sections are imbalanced or under-enrolled,
**so that** elective sections are fairly managed and coordinators are informed of enrolment issues.

**Story Points:** 5

**BRD Requirements:** 6.7 — "Support minimum/maximum enrolment thresholds per elective, with waitlisting and automatic elective-group re-balancing"

**Acceptance Criteria:**

1. **Given** an elective at max capacity with a waitlist, **When** a registered student drops, **Then** the next waitlisted student is automatically enrolled and notified.
2. **Given** an elective below minimum enrolment threshold, **When** the configured deadline passes, **Then** the coordinator receives an under-enrolment alert.
3. **Given** multiple sections of the same elective with imbalanced enrolment, **When** reviewed, **Then** the system proposes rebalancing moves to the coordinator.
4. **Given** a waitlisted student, **When** their position changes or they are enrolled, **Then** they receive a notification.
5. **Given** cross-department electives, **When** students from multiple departments register, **Then** enrolment tracking and thresholds work across department boundaries.

---

### Story 26: Cross-Department and Cross-Program Elective Scheduling

**As a** Department Coordinator,
**I want** cross-department, cross-program (UG/PG/PhD) electives and cross-listed courses to be scheduled without clashing with any registered student's core courses,
**so that** students taking courses across departments and programs have a conflict-free schedule.

**Story Points:** 5

**BRD Requirements:** 6.7 — "Support cross-department and cross-program electives"; 7.1 — "Cross-listed / cross-department courses: Courses shared across programs/departments requiring synchronized slots"; 7.5 — "Cross-program overlap: Avoiding clashes for students taking courses across UG/PG/PhD offerings"

**Acceptance Criteria:**

1. **Given** a cross-department elective, **When** scheduled, **Then** the system checks for clashes against ALL registered students' core courses regardless of their home department.
2. **Given** a cross-listed course (same course offered to two departments), **When** scheduled, **Then** both departments see it synchronized at the same time slot.
3. **Given** students from UG, PG, and PhD programs taking the same elective, **When** the schedule is generated, **Then** none have a clash with their respective program's core courses.
4. **Given** a cross-department elective, **When** displayed in the catalogue, **Then** students from all eligible departments/programs can see and register for it.
5. **Given** a cross-department or cross-program clash detected, **When** flagged, **Then** details include which programs/batches/departments are affected.

---

### Story 27: Add/Drop and Roster Management

**As a** Department Coordinator,
**I want** any elective add/drop or section-change action to immediately update the student's timetable, class roster, and headcount, with alerts when approaching room capacity,
**so that** the system always reflects current enrolment state accurately in real-time.

**Story Points:** 5

**BRD Requirements:** 6.15 — "Any elective add/drop or section-change action shall automatically and immediately update the affected student's personal timetable, class roster, and headcount-driven room/capacity checks"; 6.15 — "Maintain a real-time, authoritative class-roster view per session"; 6.15 — "Flag and alert coordinators when add/drop volume for a session approaches room capacity"; 7.5 — "Add/drop reconciliation window: Cut-off timing and real-time roster update rule"

**Acceptance Criteria:**

1. **Given** a student adds a course/section, **When** the action completes, **Then** their personal timetable, the class roster, and the session headcount are all updated immediately.
2. **Given** a student drops a course, **When** the action completes, **Then** roster and headcount decrease immediately.
3. **Given** a session approaching room capacity [threshold TBD — configurable], **When** another add occurs, **Then** the coordinator receives a capacity alert.
4. **Given** a session at max room capacity, **When** an add is attempted, **Then** it is rejected with a suggestion of alternative sections that have space.
5. **Given** per-session view, **When** the authoritative roster is queried, **Then** it reflects all add/drops in real-time (no lag).

---

### Story 28: External Enrolment Data Synchronization

**As a** System Administrator,
**I want** a scheduled or on-demand data-exchange mechanism to synchronize enrolment data with external registration/LMS systems,
**so that** the timetable system stays in sync with the institution's authoritative registration records.

**Story Points:** 3

**BRD Requirements:** 6.15 — "Where the institution's registration/enrolment record lives in a separate system, provide a scheduled or on-demand data-exchange to keep the two in sync"

**Acceptance Criteria:**

1. **Given** an external registration system, **When** a scheduled sync runs, **Then** enrolment data (adds, drops, section changes) is reconciled between systems.
2. **Given** an on-demand sync trigger, **When** an admin initiates it, **Then** reconciliation runs immediately with a result report.
3. **Given** a sync conflict (e.g., student enrolled in external system but session at capacity locally), **When** detected, **Then** the conflict is flagged for coordinator resolution.
4. **Given** a sync operation, **When** completed, **Then** a log of all changes made is available for audit.

---

### Story 29: Faculty Leave and Substitution Proposal

**As a** Faculty member,
**I want** to submit leave and have the system propose eligible substitute faculty based on subject competency and slot availability,
**so that** my classes are covered without manual coordination.

**Story Points:** 5

**BRD Requirements:** 6.5 — "Handle leave and ad-hoc substitution by proposing substitute faculty based on subject competency and availability"; 7.2 — "Leave & substitution rules: Eligible substitute pool by subject competency and availability"

**Acceptance Criteria:**

1. **Given** a faculty member submits leave for specific dates, **When** the system identifies affected sessions, **Then** it proposes substitute faculty who have the subject competency for those courses.
2. **Given** proposed substitutes, **When** availability is checked, **Then** only faculty without scheduling conflicts in the affected slots AND without declared unavailability are proposed.
3. **Given** a substitute is confirmed, **When** finalized, **Then** affected students receive a notification of the substitution.
4. **Given** no eligible substitute is available (none have competency + availability), **When** the system cannot find one, **Then** it alerts the coordinator for manual resolution.
5. **Given** a substitution event, **When** it occurs, **Then** it is recorded in the audit trail and the calendar feed updates.

---

### Story 30: Faculty Workload Computation and Violation Detection

**As an** HOD,
**I want** the system to compute weekly and semester teaching load per faculty (including combined multi-campus/multi-department load) and flag violations against min/max norms per cadre,
**so that** workload compliance is monitored and violations are caught before publication.

**Story Points:** 5

**BRD Requirements:** 6.5 — "Compute weekly/semester teaching load per faculty and validate against minimum/maximum norms"; 6.5 — "Support faculty teaching across multiple departments/campuses with combined workload visibility"; 7.2 — "Min/max weekly teaching load: Institutional and accreditation-mandated workload bounds"; 7.2 — "Multi-department / multi-campus load: Combined workload visibility"

**Acceptance Criteria:**

1. **Given** a faculty member with assigned sessions, **When** workload is computed, **Then** weekly and semester totals accurately reflect all assigned teaching hours.
2. **Given** a faculty member teaching across two campuses, **When** combined workload is computed, **Then** both campuses' sessions are summed together.
3. **Given** a faculty member teaching across two departments, **When** workload is viewed, **Then** the combined multi-department load is visible.
4. **Given** min/max norms configured per cadre (e.g., Professor: 8–12 hrs/week), **When** a faculty member exceeds the maximum or falls below minimum, **Then** the violation is flagged.
5. **Given** a draft submitted for approval, **When** workload compliance check runs, **Then** all violations are listed for the reviewer.

---

### Story 31: Accreditation Norm Configuration

**As an** Accreditation & Compliance Officer,
**I want** to configure NBA/NAAC/UGC-prescribed norms including min/max weekly hours per faculty cadre and school-specific credit-to-contact-hour ratios,
**so that** compliance validation uses the correct, institution-specific parameters.

**Story Points:** 3

**BRD Requirements:** 6.11 — "Configure and validate against NBA/NAAC/UGC-prescribed norms"; 6.11 — "Exact numeric norms vary by accreditation body/state/university statute and must be configurable rather than hard-coded"; 7.6 — "Accreditation workload norms: NBA/NAAC/UGC-prescribed min/max teaching hours by faculty cadre"; 7.6 — "Credit-to-contact-hour mapping: Statutory mapping used in compliance reporting"; 7.7 — "School-specific credit-to-contact-hour ratios: Credit-to-contact-hour conversion differs by school and must be configurable per school"

**Acceptance Criteria:**

1. **Given** a compliance officer role, **When** they configure min/max weekly hours per cadre (Professor, Associate Prof, Assistant Prof), **Then** values are saved and used in compliance checks.
2. **Given** school-specific credit-to-contact-hour ratios, **When** configured per school, **Then** each school's ratio is independently stored and used in workload computation.
3. **Given** accreditation norm changes (e.g., UGC updates a maximum), **When** the officer updates the configuration, **Then** subsequent compliance checks use the new values — no code change required.
4. **Given** configured norms, **When** the scheduling system runs compliance validation, **Then** it references the currently configured values (not hard-coded defaults).

---

### Story 32: Compliance Validation and Publication Gating

**As an** Accreditation & Compliance Officer,
**I want** the system to validate all faculty assignments against configured norms on approval submission and block publication if unresolved violations exist (unless an authorized override is recorded),
**so that** non-compliant timetables cannot be published without explicit accountability.

**Story Points:** 5

**BRD Requirements:** 6.11 — "Flag non-compliant assignments before publication"; (implied by 6.11: publication should be gated on compliance unless overridden)

**Acceptance Criteria:**

1. **Given** a draft submitted for approval, **When** compliance validation runs, **Then** all faculty assignments are checked against configured norms.
2. **Given** non-compliant assignments found (overload or underload per cadre), **When** listed, **Then** each violation shows which faculty, which norm is violated, and by how much.
3. **Given** unresolved compliance violations, **When** publication is attempted, **Then** it is blocked until violations are resolved or an authorized override is recorded.
4. **Given** an authorized override, **When** recorded, **Then** the justification and authorizing user are captured in the audit trail.
5. **Given** all violations resolved or overridden, **When** publication proceeds, **Then** the compliance status is "compliant" or "overridden with justification."

---

### Story 33: Compliance Report Generation

**As an** Accreditation & Compliance Officer,
**I want** to generate compliance-ready reports including faculty workload summaries (actual vs. normative), room utilization, and course-credit mapping,
**so that** accreditation visits can be supported with accurate, formatted data.

**Story Points:** 5

**BRD Requirements:** 6.11 — "Generate compliance-ready reports for accreditation visits"; 6.16 — "Faculty workload distribution and compliance dashboards"

**Acceptance Criteria:**

1. **Given** a compliance officer role, **When** a workload summary report is requested, **Then** it shows actual vs. normative load per faculty and per department.
2. **Given** reporting needs, **When** a room utilization report is requested, **Then** it shows usage percentages by campus, building, and time-slot.
3. **Given** accreditation requirements, **When** a course-credit mapping report is generated, **Then** L-T-P hours are shown against credit norms per school's ratio.
4. **Given** a report, **When** generated, **Then** it can be exported in a format suitable for accreditation submission (BRD 6.16 — implies exportable).
5. **Given** historical data, **When** cross-semester comparison is requested, **Then** comparative data is available.

---

### Story 34: Multi-Campus Faculty Scheduling with Travel-Time Buffers

**As a** Registrar,
**I want** the scheduling engine to enforce configurable travel-time buffers between sessions when a faculty member has back-to-back sessions on different campuses,
**so that** faculty have adequate travel time and are not scheduled impossibly.

**Story Points:** 5

**BRD Requirements:** 6.12 — "Support faculty and shared electives spanning campuses, with travel-time buffer rules"; 7.3 — "Location / transit buffer: Minimise back-to-back session travel time between distant buildings/campuses"; 6.5 — "Support faculty teaching across multiple departments/campuses"

**Acceptance Criteria:**

1. **Given** a faculty member assigned to Campus A and Campus B, **When** back-to-back sessions are on different campuses, **Then** a configurable travel-time buffer [duration TBD — confirm with stakeholder] is enforced between them.
2. **Given** a schedule violating the travel-time buffer, **When** conflict detection runs, **Then** it is flagged as a travel-time violation.
3. **Given** the scheduling engine, **When** generating for a multi-campus faculty, **Then** it respects travel-time constraints and does not place impossible back-to-back cross-campus sessions.
4. **Given** a building-to-building buffer within the same campus (if configured), **When** sessions are in distant buildings, **Then** a campus-internal buffer is applied.

---

### Story 35: Shared Resource Booking Across Departments

**As a** Department Coordinator,
**I want** to book shared resources (auditoriums, seminar halls, common labs) across departments with configurable priority-based reservation rules and inter-campus room borrowing,
**so that** common facilities are fairly allocated and cross-campus needs are supported.

**Story Points:** 5

**BRD Requirements:** 6.4 — "Support shared/common resource booking across departments with a first-come/priority-based reservation rule set"; 6.4 — "Support inter-campus room borrowing rules where a department may need to use another campus's facility"; 7.3 — "Shared-resource priority rules: Booking priority for common facilities used by multiple departments"

**Acceptance Criteria:**

1. **Given** a shared auditorium, **When** two departments request the same slot, **Then** priority rules (configurable) determine which request is granted.
2. **Given** a department needing a room on another campus, **When** they request inter-campus borrowing, **Then** the system applies configured borrowing rules and any required approval.
3. **Given** a shared resource is booked, **When** other departments view availability, **Then** the booked slot shows as unavailable.
4. **Given** a booking is confirmed, **When** completed, **Then** affected parties are notified and the booking is reflected in scheduling.
5. **Given** priority rules need updating, **When** an admin changes them, **Then** subsequent bookings use the new priority order.

---

### Story 36: Notification Delivery Engine (Multi-Channel)

**As a** Faculty member,
**I want** to receive automated notifications via my preferred channel (email, SMS, or in-app) for timetable publications, rescheduling, cancellations, and substitutions,
**so that** I am always informed of schedule changes promptly.

**Story Points:** 8

**BRD Requirements:** 6.13 — "Automated notifications (email/SMS/in-app) to affected faculty and students on publication, rescheduling, cancellation, or substitution"; 6.13 — "Digest/summary notification option in addition to real-time alerts for critical changes"

**Acceptance Criteria:**

1. **Given** a timetable change (publication, rescheduling, cancellation, substitution), **When** affected users are identified, **Then** each receives a notification via their configured preferred channel.
2. **Given** multi-channel support (email, SMS, in-app), **When** a notification is dispatched, **Then** it goes through the user's selected channel.
3. **Given** a user preferring digest/summary notifications, **When** multiple changes occur within a configurable period, **Then** they receive one aggregated summary rather than individual alerts.
4. **Given** a notification delivery failure, **When** delivery cannot complete after retries, **Then** the failure is logged and surfaced for administrative attention.
5. **Given** a user, **When** they update their notification channel preference, **Then** subsequent notifications respect the new preference immediately.
6. **Given** students as recipients, **When** their schedule changes, **Then** they also receive notifications through their preferred channel.

---

### Story 37: User Notification Preferences

**As a** Faculty member or Student,
**I want** to configure my notification preferences (preferred channel, digest frequency, opt-in/out per event type),
**so that** I receive notifications in the way that works best for me.

**Story Points:** 3

**BRD Requirements:** 6.13 — (implied: users need to set preferences for channels and digest vs. real-time)

**Acceptance Criteria:**

1. **Given** a user, **When** they access notification preferences, **Then** they can select their preferred channel (email, SMS, in-app).
2. **Given** a user, **When** they configure digest frequency (daily/weekly) vs. real-time, **Then** the preference is saved and respected.
3. **Given** a preference change, **When** saved, **Then** subsequent notifications follow the new preference.
4. **Given** a preference API, **When** queried, **Then** it returns the user's current settings.

---

### Story 38: iCal Calendar Feed Generation

**As a** Faculty member,
**I want** an automated iCal (.ics) feed of my published timetable that I can subscribe to in my personal calendar app,
**so that** my schedule is always visible in my calendar without manual entry.

**Story Points:** 5

**BRD Requirements:** 6.14 — "Provide one-click, automated calendar export (iCal/.ics feed) of the published timetable for every faculty member and student"; 6.14 — "Support a live subscription feed so that any subsequent change, substitution, or cancellation automatically reflects in personal calendars"; 6.14 — "Implement feed URL generation with secure, non-guessable tokens"

**Acceptance Criteria:**

1. **Given** a faculty member or student with published sessions, **When** they request their calendar feed, **Then** a valid iCal/.ics file is generated with all their sessions (correct day, time, room, course).
2. **Given** a live subscription URL, **When** a session is rescheduled, cancelled, or a substitution occurs, **Then** the feed auto-updates to reflect the change.
3. **Given** security requirements, **When** a feed URL is generated, **Then** it uses a secure, non-guessable token (not predictable from user ID).
4. **Given** a student, **When** they request their personal feed, **Then** it contains their core + elective sessions.
5. **Given** one-click access, **When** a user clicks "Subscribe to Calendar," **Then** the subscription link is provided in a format compatible with major calendar apps.

---

### Story 39: Bulk Calendar Export and Subscription Tracking

**As a** Department Coordinator,
**I want** bulk/admin-level calendar export for my department's faculty and logging of export/subscription status per user,
**so that** I can distribute calendar feeds and track adoption.

**Story Points:** 3

**BRD Requirements:** 6.14 — "Provide a bulk/admin-level calendar export option for coordinators"; 6.14 — "Log export/subscription status per user for adoption and troubleshooting"

**Acceptance Criteria:**

1. **Given** a coordinator role, **When** bulk export is requested for their department, **Then** all faculty calendar feed URLs/files are generated or listed.
2. **Given** export/subscription tracking, **When** an admin checks adoption, **Then** per-user status is visible (who has subscribed, who hasn't, last refresh time).
3. **Given** a troubleshooting need, **When** subscription status shows an issue, **Then** the log provides enough detail to diagnose.

---

### Story 40: Room and Lab Utilization Reports

**As a** Registrar,
**I want** room and lab utilization reports filterable by campus, building, department, and time-slot,
**so that** I can identify underused spaces and make informed infrastructure planning decisions.

**Story Points:** 5

**BRD Requirements:** 6.16 — "Room/lab utilisation reports by campus, building, and time-slot"; 6.4 — "Provide real-time room-availability and utilisation views"

**Acceptance Criteria:**

1. **Given** published timetable data, **When** a room utilization report is requested, **Then** it shows percentage utilization per room across the schedule.
2. **Given** filter options, **When** filtered by campus and/or building, **Then** only rooms in that location are shown.
3. **Given** time-slot filtering, **When** a day/time range is selected, **Then** utilization for those specific slots is displayed.
4. **Given** real-time availability, **When** queried for current/upcoming slots, **Then** which rooms are free vs. occupied is shown.
5. **Given** data across semesters, **When** trend analysis is requested, **Then** utilization trends over time are visible.

---

### Story 41: Historical Timetable Archive and Cross-Semester Comparison

**As a** Registrar,
**I want** historical timetable archives partitioned by academic year/semester with cross-semester version comparison,
**so that** I can track changes over time, support audits, and inform future planning.

**Story Points:** 3

**BRD Requirements:** 6.16 — "Historical timetable archive with version comparison across semesters"; 8 — "Data Retention: Retain historical timetables and compliance reports for a minimum of [X] years per institutional/accreditation policy"

**Acceptance Criteria:**

1. **Given** a published timetable, **When** a new version is published or a new semester begins, **Then** the previous version is archived and accessible.
2. **Given** multiple semesters of data, **When** cross-semester comparison is requested, **Then** differences between two selected semesters are shown.
3. **Given** data retention configuration [duration TBD — per institutional policy], **When** configured, **Then** historical data is retained for at least that period.
4. **Given** an auditor or compliance officer, **When** they request historical data, **Then** it is available in a queryable format.

---

### Story 42: Audit Trail Service

**As a** Registrar,
**I want** a complete, immutable audit trail of all system actions (timetable mutations, approvals, blocks, substitutions, configuration changes),
**so that** every modification is traceable for compliance, grievance handling, and accountability.

**Story Points:** 5

**BRD Requirements:** 8 — "Auditability: Full change log: who changed what, when, and why, for every timetable version"; 6.8 — "Maintain a full approval audit trail"; 6.4 — "Maintain a complete audit trail for every block"

**Acceptance Criteria:**

1. **Given** any data mutation (create, update, delete) on any entity, **When** it occurs, **Then** an audit entry is created recording: previous value, new value, user ID, timestamp, entity type/ID.
2. **Given** audit entries, **When** any user (including admin) attempts to modify or delete them, **Then** the system rejects the action — immutability is enforced.
3. **Given** a date range, **When** an audit report is queried, **Then** all entries within that period are returned with full context.
4. **Given** a specific entity (e.g., a session or a faculty assignment), **When** its history is queried, **Then** the complete chronological change history is displayed.
5. **Given** audit data, **When** used for grievance handling, **Then** it clearly shows the chain of responsibility (who changed what and when).

---

### Story 43: Role-Based Access Control (RBAC)

**As a** System Administrator,
**I want** role-based access control that restricts actions and data visibility based on user roles, with data segregation by campus/department for scoped roles,
**so that** each user can only access and modify data relevant to their responsibilities.

**Story Points:** 8

**BRD Requirements:** 8 — "Security & Access Control: Role-based access control aligned to system roles; data segregation by campus/department where applicable"; BRD Section 4 — 10 defined roles: Registrar, Dean, HOD, Department Timetable Coordinator, Faculty/Teaching Staff, Student, Examination Controller, IT/System Administrator, Accreditation & Compliance Officer, Visiting/Adjunct Faculty

**Acceptance Criteria:**

1. **Given** all 10 BRD-defined roles (Registrar, Dean, HOD, Coordinator, Faculty, Student, Exam Controller, Admin, Compliance Officer, Visiting/Adjunct Faculty), **When** users are assigned roles, **Then** they can only perform actions permitted for that role.
2. **Given** a Coordinator role scoped to a department, **When** they access timetable data, **Then** they see only their department/campus data (data segregation).
3. **Given** a Visiting/Adjunct Faculty role, **When** they access the system, **Then** they can only declare availability and view their own assigned sessions (limited access per BRD Section 4).
4. **Given** an unauthorized action attempt, **When** blocked, **Then** an access-denial entry is logged for audit.
5. **Given** a Student role, **When** they access the system, **Then** they can only view timetables, register for electives, and manage their own preferences — no edit capabilities on schedules.
6. **Given** role configuration, **When** permissions need updating, **Then** roles can be reconfigured without code changes.
7. **Given** a Dean or Registrar role, **When** they exercise cross-department conflict arbitration or scheduling override (per Story 49), **Then** the RBAC system permits the action and enforces that only Dean/Registrar-level roles can perform overrides on other departments' schedules.

---

### Story 44: Authentication and Secure Session Management

**As a** System Administrator,
**I want** secure authentication with session management that enforces password security, session invalidation on logout/password-change/inactivity, and secure cookie handling,
**so that** the system is protected against unauthorized access.

**Story Points:** 5

**BRD Requirements:** 8 — "Security & Access Control" (general); Organization security standards (HttpOnly, Secure, SameSite cookies; strong hashing; session invalidation)

**Note:** This story describes required security behaviors, not implementation choices. Algorithm/library selection belongs in the Design Document.

**Acceptance Criteria:**

1. **Given** a user with valid credentials, **When** they log in, **Then** a session/token is created and they gain access.
2. **Given** a logged-in user, **When** they log out, **Then** the session is invalidated immediately and the token cannot be reused.
3. **Given** password storage, **When** passwords are stored, **Then** they are never stored in plain text — strong adaptive hashing is used.
4. **Given** session inactivity beyond a configurable timeout, **When** the timeout expires, **Then** the session is invalidated automatically.
5. **Given** a password change, **When** completed, **Then** all existing sessions for that user are invalidated.
6. **Given** session cookies, **When** set, **Then** HttpOnly, Secure, and SameSite flags are applied.

---

### Story 45: Consolidated Institution-Wide View

**As a** Registrar,
**I want** a consolidated institution-wide view of all scheduling activities alongside department/campus-scoped views,
**so that** I can oversee the entire institution's timetabling from a single dashboard.

**Story Points:** 5

**BRD Requirements:** 6.12 — "Provide a consolidated, institution-wide view for the Registrar alongside department/campus-scoped views"

**Acceptance Criteria:**

1. **Given** a registrar role, **When** they access the consolidated view, **Then** all campuses' timetable status, approvals, and conflicts are visible in one interface.
2. **Given** the consolidated view, **When** filtered by campus, **Then** only that campus's data is shown.
3. **Given** the consolidated view, **When** filtered by department, **Then** only that department's data is shown.
4. **Given** cross-campus conflicts (shared faculty, shared resources), **When** they exist, **Then** they are highlighted in the consolidated view.
5. **Given** an HOD role, **When** they access the system, **Then** they see only their department's scope (not the institution-wide view).

---

### Story 46: Student Personal Timetable and Course Catalogue View

**As a** Student,
**I want** to view the full course catalogue with timetable information before registration opens, and my personal timetable after registration (web and mobile-responsive),
**so that** I can make informed course choices and always know my schedule.

**Story Points:** 5

**BRD Requirements:** 6.17 — "Provide a single consolidated view of the full course catalogue and the tentative/published timetable together before the registration window opens"; 6.17 — "Personal timetable view (web/mobile) with calendar export/subscription and change notifications"; 8 — "mobile-responsive student/faculty views" (Usability NFR)

**Acceptance Criteria:**

1. **Given** a student, **When** they access the course catalogue before registration opens, **Then** they see all courses with their tentative/published scheduled times and seat availability.
2. **Given** a registered student, **When** they view their personal timetable, **Then** it shows all enrolled sessions (core + electives) in a weekly view.
3. **Given** a personal timetable, **When** the student requests calendar export/subscription, **Then** an iCal link is provided (consumes Story 38).
4. **Given** a mobile device, **When** the student accesses their timetable, **Then** the view is responsive and usable on smartphone/tablet (BRD Section 8 Usability).
5. **Given** a timetable change affecting the student, **When** it occurs, **Then** they receive a notification and their view updates.

---

### Story 47: Post-Publication Change Management

**As a** Department Coordinator,
**I want** to make changes to a published timetable (rescheduling, cancellation) with conflict re-checking, notification to affected users, and audit trail,
**so that** necessary post-publication adjustments are controlled and communicated.

**Story Points:** 5

**BRD Requirements:** 6.13 — "Automated notifications... on... rescheduling, cancellation, or substitution" (implies changes happen post-publication); Story-quality R8 checklist: "Post-publication change management"

**Acceptance Criteria:**

1. **Given** a published timetable, **When** a coordinator needs to reschedule a session, **Then** they can do so with conflict re-checking applied to the new slot.
2. **Given** a session is rescheduled post-publication, **When** confirmed, **Then** all affected faculty and students are notified.
3. **Given** a session is cancelled post-publication, **When** confirmed, **Then** affected users are notified and the session is removed from feeds.
4. **Given** any post-publication change, **When** made, **Then** a full audit trail entry captures the change, reason, and who made it.
5. **Given** a post-publication change, **When** it involves a room change, **Then** the new room's availability and capacity are validated.

---

### Story 48: Data Export (Reports in Standard Formats)

**As a** Registrar,
**I want** to export reports and timetable data in standard formats (CSV, PDF) for distribution and archival,
**so that** data can be shared with stakeholders who don't have system access and retained for offline use.

**Story Points:** 3

**BRD Requirements:** Story-quality R8 checklist: "Data export (reports, CSV, PDF)"; 6.16 — (reports need to be exportable for accreditation and distribution)

**Acceptance Criteria:**

1. **Given** any generated report (utilization, workload, compliance, conflict log), **When** export is requested, **Then** it is available in at least one standard format (CSV or PDF).
2. **Given** a timetable view, **When** export is requested, **Then** the timetable data can be downloaded in a structured format.
3. **Given** an exported file, **When** opened externally, **Then** it is properly formatted and readable without the system.

---

### Story 49: Cross-Department Conflict Arbitration by Dean and Registrar

**As a** Dean,
**I want** to actively arbitrate and resolve inter-department scheduling conflicts (shared faculty disputes, shared resource contention, cross-department elective clashes) by overriding a department's scheduling decision when necessary,
**so that** cross-department conflicts are resolved by an authorized authority rather than remaining in deadlock.

**Story Points:** 5

**BRD Requirements:** BRD Section 4 — Dean: "Approves department-level timetables under their school; arbitrates inter-department clashes"; Registrar: "resolves cross-department conflicts"

**Acceptance Criteria:**

1. **Given** a Dean role, **When** two departments under their school have a scheduling conflict (e.g., both claiming the same shared faculty slot), **Then** the Dean can view the conflict details and the competing department schedules.
2. **Given** a cross-department conflict, **When** the Dean makes an arbitration decision (e.g., assigns the shared faculty to Department A's slot and instructs Department B to use an alternative), **Then** the decision is applied to the timetable and the overridden department is notified with the reason.
3. **Given** a Registrar role, **When** a cross-department or cross-school conflict cannot be resolved by the Dean, **Then** the Registrar can override any department's or school's scheduling decision with recorded justification.
4. **Given** an arbitration/override action, **When** executed, **Then** a full audit trail entry is created (who arbitrated, what was overridden, reason, affected departments, timestamp).
5. **Given** an override applied to a published or in-approval draft, **When** it changes the schedule, **Then** conflict detection re-runs on the affected area and affected users are notified of the change.

---

### Story 50: Bulk Master Data Import

**As a** System Administrator,
**I want** to import master data (campuses, departments, programs, courses, faculty, rooms, batches) in bulk via CSV/Excel upload with validation and error reporting,
**so that** initial data load and semester-to-semester updates can be performed efficiently without manual one-by-one entry.

**Story Points:** 5

**BRD Requirements:** Story-quality R8 checklist: "Bulk data import (CSV/Excel for master data)"; BRD Section 9 (Assumptions): "Master data will be available in a clean, digitised form prior to implementation, or will be cleaned up as part of a data-migration phase" (implies bulk import is needed)

**Acceptance Criteria:**

1. **Given** an admin role, **When** they upload a CSV/Excel file for a master data entity (e.g., courses), **Then** the system parses and validates all rows before committing.
2. **Given** validation errors in the uploaded file (e.g., missing required fields, invalid references), **When** the import is processed, **Then** the system returns an error report listing each invalid row with the specific error — no partial import occurs.
3. **Given** a valid file with no errors, **When** import is confirmed, **Then** all rows are persisted and the total count of imported records is reported.
4. **Given** an existing record with the same unique identifier, **When** the import encounters it, **Then** it either updates (upsert) or rejects the duplicate based on configured behavior.
5. **Given** a large file (e.g., 500+ rows), **When** imported, **Then** the system handles it without timeout and reports progress.

---

## Explicitly Deferred Items

The following BRD requirements are **explicitly deferred** and will NOT be addressed in the current story set. Each deferral is documented with reasoning:

| # | Requirement | BRD Source | Reason for Deferral | Deferred To |
|---|---|---|---|---|
| 1 | **Scalability** — "Support multi-campus scale: 10,000+ students, 500+ faculty, 200+ rooms, concurrent use by 50+ coordinators" | BRD Section 8 | This is an architecture/infrastructure concern validated through performance testing and load testing, not a functional user story. The 2-minute generation time and 2-second conflict response are captured as functional ACs in Stories 10 and 15. Scalability is addressed in the Design Document through architecture decisions (database indexing, connection pooling, caching strategy). Concurrent coordinator access is addressed in Story 14 AC 5. | Design phase / Performance testing phase |
| 2 | **Availability** — "99.5% uptime during academic term; scheduled maintenance windows outside business hours" | BRD Section 8 | This is an operational SLA addressed through deployment infrastructure (load balancing, redundancy, monitoring, alerting), not through application-level user stories. It is validated through infrastructure setup and SLA monitoring, not through functional testing. | Infrastructure/DevOps phase |
| 3 | **Localization** — "Support institution-specific terminology and optionally regional language labels for student-facing views" | BRD Section 8 | BRD explicitly says "optionally." Core scheduling functionality is language-independent. Localization can be layered on top of completed features without redesign. Does not affect data model or business logic. | Phase 2 |
| 4 | **Error Recovery / Undo** — Explicit undo button or action reversal | Story-quality R8 checklist | BRD does not explicitly require an undo mechanism. The draft-based workflow provides implicit recovery: discard a draft, re-generate, or revert to a previous version (Story 19 enables version comparison). Post-publication changes are controlled through Story 47. The audit trail (Story 42) enables identifying what to reverse. An explicit undo UX enhancement can be added later. | Phase 2 (UX enhancement) |

---

## Summary

### Story Point Arithmetic (Verified)

| Point Value | Count | Subtotal |
|---|---|---|
| 8 points | 10 stories (7, 10, 14, 15, 18, 21, 23, 24, 36, 43) | 80 |
| 5 points | 32 stories (1, 2, 3, 4, 5, 6, 8, 9, 11, 12, 13, 19, 20, 22, 25, 26, 27, 29, 30, 32, 33, 34, 35, 38, 40, 42, 44, 45, 46, 47, 49, 50) | 160 |
| 3 points | 8 stories (16, 17, 28, 31, 37, 39, 41, 48) | 24 |
| **Total** | **50 stories** | **264 points** |

### BRD Section → Story Coverage Matrix (Checkable)

| BRD Section | Requirement | Covering Story(ies) |
|---|---|---|
| 6.1 | Hierarchical master data | Story 1 |
| 6.1 | Course master | Story 2 |
| 6.1 | Faculty master | Story 3 |
| 6.1 | Room/Lab master | Story 5 |
| 6.1 | Asset/schedulable-resource master | Story 6 |
| 6.1 | Academic calendar | Story 8 |
| 6.1 | Batch/section master | Story 1 (AC 4) |
| 6.2 | Generate proposed weekly timetable | Story 10 |
| 6.2 | Configurable time-slot grids | Story 9 |
| 6.2 | Fortnightly/alternate-week patterns | Story 12 |
| 6.2 | Lock sessions before engine | Story 13 |
| 6.2 | Drag-and-drop with conflict flagging | Story 14 |
| 6.3 | Exam schedule generation (all 3 types) | Story 21 |
| 6.3 | Seating plan generation | Story 22 |
| 6.3 | Invigilation roster (equitable) | Story 22 |
| 6.3 | Cross-dept exam clash prevention | Story 21 (AC 4) |
| 6.4 | Room allocation (capacity, equipment, proximity) | Stories 5, 10, 15 |
| 6.4 | Shared resource booking (priority-based) | Story 35 |
| 6.4 | Inter-campus room borrowing | Story 35 (AC 2) |
| 6.4 | Real-time room availability | Story 40 (AC 4) |
| 6.4 | Hard/soft blocking model (any asset) | Story 7 |
| 6.4 | Resource-blocking workflow with approval | Story 7 (AC 3) |
| 6.4 | Auto conflict re-detection on block activation | Story 7 (AC 4) |
| 6.4 | Block audit trail and reports | Story 7 (AC 5, 6) |
| 6.5 | Faculty time-preferences/unavailability | Story 4 |
| 6.5 | Weekly/semester load computation + validation | Story 30 |
| 6.5 | Multi-dept/campus combined workload | Story 30 (AC 2, 3) |
| 6.5 | Leave and substitution | Story 29 |
| 6.6 | Batch-splitting for practicals | Story 23 (AC 1) |
| 6.6 | Block scheduling (2-3 contiguous) | Story 23 (AC 2) |
| 6.6 | Equipment/software prerequisite matching | Story 23 (AC 3) |
| 6.7 | Elective registration (clash prevention) | Story 24 |
| 6.7 | Min/max enrolment, waitlisting, rebalancing | Stories 24, 25 |
| 6.7 | Cross-department/cross-program electives | Story 26 |
| 6.8 | Configurable multi-level approval workflow | Story 18 |
| 6.8 | Review comments, rejection with reason | Story 18 (AC 3) |
| 6.8 | Version comparison between iterations | Story 19 |
| 6.8 | Approval audit trail | Story 18 (AC 4) |
| 6.9 | Real-time conflict detection (all types) | Story 15 |
| 6.9 | Ranked alternative-slot suggestions | Story 16 |
| 6.9 | Conflict log persistence | Story 17 |
| 6.10 | Auto-generate first-draft timetable | Story 10 |
| 6.10 | Manual override/re-arrange before approval | Story 14 |
| 6.10 | Feasibility/quality score + violations list | Story 10 (AC 2, 4) |
| 6.10 | Re-run on subset without disturbing approved | Story 13 |
| 6.11 | Configure NBA/NAAC/UGC norms | Story 31 |
| 6.11 | Compliance-ready reports | Story 33 |
| 6.11 | Flag non-compliant before publication | Story 32 |
| 6.12 | Campus-specific calendars/grids/holidays | Stories 8, 9 |
| 6.12 | Faculty spanning campuses + travel-time | Story 34 |
| 6.12 | Consolidated institution-wide view | Story 45 |
| 6.13 | Automated notifications (email/SMS/in-app) | Story 36 |
| 6.13 | Digest/summary option | Stories 36, 37 |
| 6.14 | iCal feed generation (per user) | Story 38 |
| 6.14 | Live subscription (auto-reflect changes) | Story 38 (AC 2) |
| 6.14 | Bulk/admin calendar export | Story 39 |
| 6.14 | Log export/subscription status per user | Story 39 (AC 2) |
| 6.15 | Add/drop → immediate timetable/roster update | Story 27 |
| 6.15 | Real-time authoritative roster per session | Story 27 (AC 5) |
| 6.15 | External system data-exchange (sync) | Story 28 |
| 6.15 | Alert on approaching room capacity | Story 27 (AC 3) |
| 6.16 | Room/lab utilization reports | Story 40 |
| 6.16 | Faculty workload dashboards | Story 33 |
| 6.16 | Conflict-log and turnaround reports | Story 17 |
| 6.16 | Historical archive + version comparison | Story 41 |
| 6.17 | Course catalogue + timetable before registration | Story 46 (AC 1) |
| 6.17 | Elective registration with seat availability | Story 24 (AC 4) |
| 6.17 | Personal timetable (web/mobile) + notifications | Story 46 |
| Sec 4 | Dean arbitrates inter-department clashes | Story 49 |
| Sec 4 | Registrar resolves cross-department conflicts | Story 49 (AC 3) |
| Sec 4 | Visiting/Adjunct Faculty (limited access) | Story 43 (AC 3) |

### NFR Coverage

| NFR | Disposition | Reference |
|---|---|---|
| Performance — generation < 2 min | Covered | Story 10 AC 1, Story 11 (timeout) |
| Performance — conflict < 2 sec | Covered | Story 15 AC 1 |
| Scalability (10K students, 50+ coordinators) | **Explicitly deferred** → Design/Perf testing | Story 14 AC 5 covers concurrency |
| Availability (99.5% uptime) | **Explicitly deferred** → Infrastructure/DevOps | — |
| Security & Access Control (RBAC + segregation) | Covered | Stories 43, 44 |
| Auditability (full change log) | Covered | Story 42 |
| Usability (drag-drop, mobile, minimal training) | Covered | Stories 14, 46 |
| Data Retention (historical archive) | Covered | Story 41 (AC 3) |
| Localization (optional regional labels) | **Explicitly deferred** → Phase 2 | BRD says "optionally" |

### Open Questions for Stakeholder Confirmation

| # | Question | Affects | Source |
|---|---|---|---|
| 1 | Prerequisite mapping: Is the intent within-semester scheduling conflict detection, or catalog-level validation that prerequisites are offered in earlier semesters? | Story 15 AC 9 | BRD 7.1 "across semesters" — ambiguous |
| 2 | Cross-listed course synchronization: How are cross-listed courses (BRD 7.1: "requiring synchronized slots") handled when departments generate independently (Story 10 Assumption: one dept at a time)? Options: (a) first dept claims shared slot, second treats as locked; (b) cross-listed pre-placed institution-wide before dept generation; (c) coordination step after both generate. | Stories 10, 26 | BRD 7.1 vs per-department generation model |
| 3 | L-T-P → session-count mapping with mixed slot durations: 3 lecture hours on a 90-minute grid ≠ 3 sessions. BRD 7.1 says L-T-P "determines number and length of weekly sessions" but doesn't define the formula. Who defines this mapping, and is it per-campus (since grids differ)? | Story 10, Story 9 | BRD 7.1 vs 7.7 (mixed slot durations) |

### Final Counts

- **Total Stories:** 50
- **Total Story Points:** 264 (10×8 + 32×5 + 8×3)
- **Unresolved Gaps:** 0
- **Explicitly Deferred:** 4 items
- **Open Questions for Stakeholders:** 3

