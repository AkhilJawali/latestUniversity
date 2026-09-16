# Task 2.3: Room Resource Schema

## Jira Reference
- **Issue Key**: SPTV2-131
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-131
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-128 — Database Schema and Migrations
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 2: Database Schema and Migrations
- **Complexity**: M
- **Requirements**: 1.5, 1.6, 6.6
- **Design Reference**: Database schema

## Description
- Create tables: buildings, rooms (with capacity, type, equipment_tags JSONB), resource_blocks
- Add composite index on (room_id, day_of_week, start_time)
- Add GIN index on room equipment_tags
- Define resource block status workflow columns

## Dependencies
- **Depends on**: Task 2.1 — SPTV2-129
- **Blocks**: Task 4.5 (room service)
- **Cross-role**: No

## Definition of Done
- [ ] Migration file created and runs successfully
- [ ] Indexes created
- [ ] Foreign keys properly named
- [ ] Reversible migration
