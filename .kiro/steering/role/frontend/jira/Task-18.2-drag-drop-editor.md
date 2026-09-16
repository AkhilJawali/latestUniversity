# Task 18.2: Implement Drag-and-Drop Timetable Editor

## Jira Reference
- **Issue Key**: SPTV2-216
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-216
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-173 — Frontend
- **Status**: To Do

## Task Details
- **Role**: frontend
- **Phase**: Phase 18: Frontend
- **Complexity**: XL
- **Requirements**: 4.6, 4.7, 23.1, 23.2

## Description
- Build grid-based timetable view with drag-and-drop session placement
- Integrate WebSocket for real-time conflict feedback (< 2s)
- Display conflict indicators and alternative suggestions inline
- Implement optimistic concurrency handling (version mismatch prompts)
- Use context-aware output encoding (never dangerouslySetInnerHTML with user data)

## Dependencies
- **Depends on**: Task 6.6 — SPTV2-157, Task 8.4 — SPTV2-178 (backend APIs)
- **Blocks**: Task 18.3 (keyboard accessibility)
- **Cross-role**: Yes — blocked by SPTV2-157, SPTV2-178

## Definition of Done
- [ ] Drag-and-drop placement working
- [ ] WebSocket conflict feedback < 2s
- [ ] Conflict indicators displayed
- [ ] Optimistic concurrency handled
- [ ] No XSS vectors (no dangerouslySetInnerHTML)
