# Task 8.5: Implement Faculty Workload Violation Detection

## Jira Reference
- **Issue Key**: SPTV2-179
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-179
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-165 — Conflict Detector
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 8: Conflict Detector
- **Complexity**: M
- **Requirements**: 7.3, 7.4, 7.5, 13.2, 13.3
- **Design Reference**: Workload management

## Description
- Compute weekly/semester load per faculty based on assigned sessions and credit-to-contact ratio
- Flag violations when load exceeds max or falls below min for cadre
- Support combined multi-department workload view

## Dependencies
- **Depends on**: Task 8.1 — SPTV2-175, Task 4.4 — SPTV2-143
- **Blocks**: Task 18.6 (faculty views)
- **Cross-role**: Yes — blocks SPTV2-220 (18.6 faculty/admin views)

## Definition of Done
- [ ] Workload computation accurate
- [ ] Min/max violations flagged
- [ ] Multi-department view working
