# Task 2.8: Audit Schema

## Jira Reference
- **Issue Key**: SPTV2-136
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-136
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-128 — Database Schema and Migrations
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 2: Database Schema and Migrations
- **Complexity**: M
- **Requirements**: 1.8, 21.1, 21.5
- **Design Reference**: Database schema

## Description
- Create audit_events table with immutable design (no UPDATE/DELETE grants)
- Partition by month for query performance
- Add indexes on entity_type, entity_id, timestamp, user_id
- Create database trigger to prevent audit row modification

## Dependencies
- **Depends on**: Task 1.2 — SPTV2-123
- **Blocks**: Task 4.9 (audit trail service)
- **Cross-role**: No

## Definition of Done
- [ ] Migration file created and runs successfully
- [ ] Indexes created
- [ ] Foreign keys properly named
- [ ] Reversible migration
