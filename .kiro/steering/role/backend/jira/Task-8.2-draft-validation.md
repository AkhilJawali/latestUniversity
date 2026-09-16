# Task 8.2: Implement Full Draft Validation

## Jira Reference
- **Issue Key**: SPTV2-176
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-176
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-165 — Conflict Detector
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 8: Conflict Detector
- **Complexity**: M
- **Requirements**: 11.1, 2.4
- **Design Reference**: Conflict detection

## Description
- Create validateDraft method that checks all sessions in a draft for conflicts
- Return structured violation list with severity (hard/soft), affected entities, descriptions
- Implement calendar change impact assessment

## Dependencies
- **Depends on**: Task 8.1 — SPTV2-175
- **Blocks**: Task 9.1 (approval workflow needs validated drafts)
- **Cross-role**: No

## Definition of Done
- [ ] Full draft validation working
- [ ] Structured violation list returned
- [ ] Calendar impact assessment functional
