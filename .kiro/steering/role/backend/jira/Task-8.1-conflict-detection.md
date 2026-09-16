# Task 8.1: Implement Real-Time Conflict Detection Engine

## Jira Reference
- **Issue Key**: SPTV2-175
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-175
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-165 — Conflict Detector
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 8: Conflict Detector
- **Complexity**: XL
- **Requirements**: 11.1, 11.2, 4.7, 22.2
- **Design Reference**: Conflict detection

## Description
- Create ConflictDetectorService with single-placement check (< 2s target)
- Detect: faculty double-booking, room double-booking, batch clashes (core-core, core-elective, elective-elective)
- Detect: max daily/weekly hours exceeded, room capacity exceeded, equipment mismatch
- Detect: hard block violations, travel-time violations, common slot violations
- Use Redis caching for hot-path lookups (current slot occupancy)

## Dependencies
- **Depends on**: Task 6.6 — SPTV2-157, Task 1.3 — SPTV2-124
- **Blocks**: Task 8.2, 8.3, 18.4 (coordinator dashboard)
- **Cross-role**: Yes — blocks SPTV2-218 (18.4 coordinator dashboard)

## Definition of Done
- [ ] Single-placement check < 2 seconds
- [ ] All conflict types detected
- [ ] Redis caching for hot-path
- [ ] No false negatives
