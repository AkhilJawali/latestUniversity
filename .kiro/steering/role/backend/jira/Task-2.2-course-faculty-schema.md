# Task 2.2: Course Faculty Schema

## Jira Reference
- **Issue Key**: SPTV2-130
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-130
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-128 — Database Schema and Migrations
- **Status**: Done

## Task Details
- **Role**: backend
- **Phase**: Phase 2: Database Schema and Migrations
- **Complexity**: M
- **Requirements**: 1.3, 1.4, 7.1
- **Design Reference**: Database schema

## Description
- Create tables: courses (with L-T-P, credits, type, equipment_tags JSONB), faculty, faculty_availability_windows
- Add GIN index on equipment_tags JSONB column
- Define faculty-course competency junction table
- Add faculty campus associations table

## Dependencies
- **Depends on**: Task 2.1 — SPTV2-129
- **Blocks**: Task 4.3, 4.4 (course/faculty services)
- **Cross-role**: No

## Definition of Done
- [ ] Migration file created and runs successfully
- [ ] Indexes created
- [ ] Foreign keys properly named
- [ ] Reversible migration
