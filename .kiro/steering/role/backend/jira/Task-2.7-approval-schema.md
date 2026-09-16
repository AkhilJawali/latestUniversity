# Task 2.7: Approval Schema

## Jira Reference
- **Issue Key**: SPTV2-135
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-135
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-128 — Database Schema and Migrations
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 2: Database Schema and Migrations
- **Complexity**: M
- **Requirements**: 10.1, 10.2, 10.4
- **Design Reference**: Database schema

## Description
- Create tables: workflow_instances, workflow_steps
- Define status enum and level progression
- Add indexes for pending approval queries

## Dependencies
- **Depends on**: Task 2.4 — SPTV2-132
- **Blocks**: Task 9.1 (approval workflow service)
- **Cross-role**: No

## Definition of Done
- [ ] Migration file created and runs successfully
- [ ] Indexes created
- [ ] Foreign keys properly named
- [ ] Reversible migration
