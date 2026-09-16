# Task 14.1: Implement Elective Registration Service

## Jira Reference
- **Issue Key**: SPTV2-198
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-198
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-170 — Student Portal
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 14: Student Portal
- **Complexity**: L
- **Requirements**: 9.1, 9.2, 9.3, 9.4
- **Design Reference**: Elective registration

## Description
- Create StudentPortalService with register/drop elective operations
- Check for clash with core course slots before registration (reject with explanation)
- Enforce min/max enrolment thresholds per section
- Implement waitlist management (auto-place when at max capacity)

## Dependencies
- **Depends on**: Task 2.6 — SPTV2-134, Task 8.1 — SPTV2-175
- **Blocks**: Task 18.5 (student portal frontend)
- **Cross-role**: Yes — blocks SPTV2-219 (18.5 student portal)

## Definition of Done
- [ ] Register/drop operations working
- [ ] Clash detection with rejection
- [ ] Min/max enrolment enforced
- [ ] Waitlist management functional
