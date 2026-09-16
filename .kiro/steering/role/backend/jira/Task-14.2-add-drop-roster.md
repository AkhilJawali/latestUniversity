# Task 14.2: Implement Add/Drop Roster Management

## Jira Reference
- **Issue Key**: SPTV2-199
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-199
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-170 — Student Portal
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 14: Student Portal
- **Complexity**: M
- **Requirements**: 17.1, 17.2, 17.3, 17.5

## Description
- On add/drop: immediately update student timetable, class roster, headcount for capacity
- Maintain authoritative real-time roster per session
- Alert coordinator when enrolment approaches room capacity threshold
- Reject add if room capacity would be exceeded, show alternative sections

## Dependencies
- **Depends on**: Task 14.1 — SPTV2-198
- **Blocks**: None
- **Cross-role**: No

## Definition of Done
- [ ] Roster updated on add/drop
- [ ] Capacity threshold alerts
- [ ] Over-capacity rejection with alternatives
