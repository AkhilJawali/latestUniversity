# Task 2.5: Exam Schema

## Jira Reference
- **Issue Key**: SPTV2-133
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-133
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-128 — Database Schema and Migrations
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 2: Database Schema and Migrations
- **Complexity**: M
- **Requirements**: 5.1, 5.3, 5.4
- **Design Reference**: Database schema

## Description
- Create tables: exam_schedules, exam_sessions, seating_plans, invigilation_duties
- Define relationships to courses, rooms, faculty, and batches

## Dependencies
- **Depends on**: Task 2.2 — SPTV2-130, Task 2.3 — SPTV2-131
- **Blocks**: Task 6.12 (exam timetable generation)
- **Cross-role**: No

## Definition of Done
- [ ] Migration file created and runs successfully
- [ ] Indexes created
- [ ] Foreign keys properly named
- [ ] Reversible migration
