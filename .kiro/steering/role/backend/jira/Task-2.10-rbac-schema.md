# Task 2.10: RBAC Schema

## Jira Reference
- **Issue Key**: SPTV2-138
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-138
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-128 — Database Schema and Migrations
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 2: Database Schema and Migrations
- **Complexity**: M
- **Requirements**: 20.1, 20.3, 20.4
- **Design Reference**: Database schema

## Description
- Create tables: roles, permissions, role_permissions, user_roles
- Seed default roles (Registrar, Dean, HOD, Coordinator, Faculty, Student, ExamController, Admin, AccreditationOfficer)
- Add access_denial_log table for unauthorized attempt tracking

## Dependencies
- **Depends on**: Task 1.2 — SPTV2-123
- **Blocks**: Task 15.1 (RBAC middleware)
- **Cross-role**: No

## Definition of Done
- [ ] Migration file created and runs successfully
- [ ] Indexes created
- [ ] Foreign keys properly named
- [ ] Reversible migration
