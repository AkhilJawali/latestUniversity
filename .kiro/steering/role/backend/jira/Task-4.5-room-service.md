# Task 4.5: Implement Room and Resource Management Service

## Jira Reference
- **Issue Key**: SPTV2-144
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-144
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-139 — Master Data Module
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 4: Master Data Module
- **Complexity**: L
- **Requirements**: 1.5, 1.6, 6.6, 6.7, 6.9
- **Design Reference**: Master data services

## Description
- Create RoomService with CRUD including capacity, type, equipment tags, building/floor
- Implement ResourceBlockService for hard/soft block lifecycle (raise, approve, release)
- Enforce block approval workflow when block impacts published sessions

## Dependencies
- **Depends on**: Task 2.3 — SPTV2-131
- **Blocks**: Task 6.2 (constraint propagation)
- **Cross-role**: No

## Definition of Done
- [ ] Room CRUD with equipment tags
- [ ] Resource block lifecycle working
- [ ] Block approval gating enforced
