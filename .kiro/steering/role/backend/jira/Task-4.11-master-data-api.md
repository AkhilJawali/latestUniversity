# Task 4.11: Implement Master Data REST API Routes

## Jira Reference
- **Issue Key**: SPTV2-150
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-150
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-139 — Master Data Module
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 4: Master Data Module
- **Complexity**: L
- **Requirements**: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7
- **Design Reference**: API routes

## Description
- Create route handlers for all master data CRUD endpoints
- Apply authentication and RBAC middleware
- Return 400 with field-level errors for validation failures
- Never expose internal details in error responses

## Dependencies
- **Depends on**: Task 4.1 — SPTV2-140, Task 4.3 — SPTV2-142
- **Blocks**: Task 18.1 (frontend setup)
- **Cross-role**: Yes — blocks SPTV2-215 (18.1 frontend setup)

## Definition of Done
- [ ] All CRUD endpoints exposed
- [ ] Auth + RBAC middleware applied
- [ ] Validation errors returned correctly
- [ ] No internal details leaked
