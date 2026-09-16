# Task 15.1: Implement RBAC Middleware and Enforcement

## Jira Reference
- **Issue Key**: SPTV2-206
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-206
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-171 — RBAC and Access Control
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 15: RBAC
- **Complexity**: L
- **Requirements**: 20.1, 20.2, 20.3

## Description
- Create RBAC middleware that checks user role permissions for every action
- Deny unauthorized actions and log access-denial audit entry
- Implement RLS context injection per request
- Ensure data segregation: Coordinators see only their department/campus data

## Dependencies
- **Depends on**: Task 1.6 — SPTV2-127, Task 2.10 — SPTV2-138
- **Blocks**: Task 4.11 (API routes need RBAC)
- **Cross-role**: No

## Definition of Done
- [ ] RBAC middleware enforcing permissions
- [ ] Access-denial audit logging
- [ ] RLS context injection per request
- [ ] Data segregation verified
