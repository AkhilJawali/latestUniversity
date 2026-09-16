# Task 4.3: Implement Course Management Service

## Jira Reference
- **Issue Key**: SPTV2-142
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-142
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-139 — Master Data Module
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 4: Master Data Module
- **Complexity**: M
- **Requirements**: 1.3
- **Design Reference**: Master data services

## Description
- Create CourseService with CRUD supporting L-T-P split, credits, type, prerequisites, equipment tags
- Validate prerequisite course references exist
- Enforce equipment tag format (string array)

## Dependencies
- **Depends on**: Task 2.2 — SPTV2-130
- **Blocks**: Task 6.1 (constraint model)
- **Cross-role**: No

## Definition of Done
- [ ] Course CRUD with L-T-P support
- [ ] Prerequisite validation
- [ ] Equipment tag handling
