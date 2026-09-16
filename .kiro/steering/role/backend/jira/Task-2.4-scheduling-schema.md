# Task 2.4: Scheduling Schema

## Jira Reference
- **Issue Key**: SPTV2-132
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-132
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-128 — Database Schema and Migrations
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 2: Database Schema and Migrations
- **Complexity**: L
- **Requirements**: 2.1, 3.1, 4.1
- **Design Reference**: Database schema

## Description
- Create tables: academic_calendars, time_slot_grids, time_slot_definitions, timetable_drafts, sessions
- Add composite index on (campus_id, day_of_week, start_time) for slot availability
- Add index on (faculty_id, day_of_week) for faculty conflict checks
- Add partial index on status='published' for active timetable queries

## Dependencies
- **Depends on**: Task 2.1 — SPTV2-129, Task 2.2 — SPTV2-130, Task 2.3 — SPTV2-131
- **Blocks**: Task 4.6, 4.7, 6.1 (calendar, time-slot, scheduling engine)
- **Cross-role**: No

## Definition of Done
- [ ] Migration file created and runs successfully
- [ ] Indexes created
- [ ] Foreign keys properly named
- [ ] Reversible migration
