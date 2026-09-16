# Task 4.6: Implement Academic Calendar Service

## Jira Reference
- **Issue Key**: SPTV2-145
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-145
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-139 — Master Data Module
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 4: Master Data Module
- **Complexity**: M
- **Requirements**: 2.1, 2.2, 2.3, 2.4
- **Design Reference**: Master data services

## Description
- Create AcademicCalendarService with semester dates, holidays, exam windows, orientation periods
- Support per-campus calendar variations
- Implement calendar change impact detection (flag sessions on removed working days)

## Dependencies
- **Depends on**: Task 2.4 — SPTV2-132
- **Blocks**: Task 6.6 (generation orchestrator)
- **Cross-role**: No

## Definition of Done
- [ ] Calendar CRUD with all date types
- [ ] Per-campus variations supported
- [ ] Change impact detection working
