# Task 2.6: Elective Schema

## Jira Reference
- **Issue Key**: SPTV2-134
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-134
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-128 — Database Schema and Migrations
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 2: Database Schema and Migrations
- **Complexity**: M
- **Requirements**: 9.1, 9.3, 9.4
- **Design Reference**: Database schema

## Description
- Create tables: elective_registrations with status enum (registered, waitlisted, dropped)
- Add unique constraint on (student_id, elective_id, semester_id)
- Add index for waitlist position queries

## Dependencies
- **Depends on**: Task 2.2 — SPTV2-130
- **Blocks**: Task 14.1 (elective registration service)
- **Cross-role**: No

## Definition of Done
- [ ] Migration file created and runs successfully
- [ ] Indexes created
- [ ] Foreign keys properly named
- [ ] Reversible migration
