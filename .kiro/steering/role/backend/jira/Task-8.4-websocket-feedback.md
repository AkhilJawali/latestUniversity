# Task 8.4: Implement WebSocket Real-Time Conflict Feedback

## Jira Reference
- **Issue Key**: SPTV2-178
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-178
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-165 — Conflict Detector
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 8: Conflict Detector
- **Complexity**: L
- **Requirements**: 4.7, 11.2, 23.2
- **Design Reference**: Real-time feedback

## Description
- Create WebSocket handler for drag-and-drop conflict checks
- Publish conflict results via WebSocket within 2-second SLA
- Handle stale data (reject placement if draft modified since drag start)
- Implement optimistic concurrency with version vectors

## Dependencies
- **Depends on**: Task 8.3 — SPTV2-177, Task 1.4 — SPTV2-125
- **Blocks**: Task 18.2 (drag-drop editor)
- **Cross-role**: Yes — blocks SPTV2-216 (18.2 drag-drop editor)

## Definition of Done
- [ ] WebSocket handler operational
- [ ] < 2s response time
- [ ] Stale data rejection working
- [ ] Optimistic concurrency with version vectors
