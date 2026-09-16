# Task 8.3: Implement Alternative Slot Suggestion Engine

## Jira Reference
- **Issue Key**: SPTV2-177
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-177
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-165 — Conflict Detector
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 8: Conflict Detector
- **Complexity**: M
- **Requirements**: 11.3, 11.5
- **Design Reference**: Conflict resolution

## Description
- Generate ranked alternative slots when conflicts are detected
- Order alternatives by feasibility score (non-increasing)
- Limit suggestion count (configurable, default 5)

## Dependencies
- **Depends on**: Task 8.1 — SPTV2-175
- **Blocks**: Task 8.4 (WebSocket feedback)
- **Cross-role**: No

## Definition of Done
- [ ] Alternatives generated on conflict
- [ ] Ranked by feasibility score
- [ ] Configurable limit
