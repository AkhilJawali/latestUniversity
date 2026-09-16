# Task 4.1: Implement Campus Hierarchy CRUD Service

## Jira Reference
- **Issue Key**: SPTV2-140
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-140
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-139 — Master Data Module
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 4: Master Data Module
- **Complexity**: M
- **Requirements**: 1.1, 1.2, 1.7
- **Design Reference**: Master data services

## Description
- Create CampusService, DepartmentService, ProgramService, BatchService with full CRUD
- Implement referential integrity validation (reject orphan references)
- Use parameterized queries for all database access
- Validate all inputs with zod schemas (type, length, format)

## Dependencies
- **Depends on**: Task 2.1 — SPTV2-129
- **Blocks**: Task 4.11 (API routes)
- **Cross-role**: No

## Definition of Done
- [ ] CRUD operations for all hierarchy entities
- [ ] Referential integrity enforced
- [ ] Input validation with zod
- [ ] Parameterized queries only
