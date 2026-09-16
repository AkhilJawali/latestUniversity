# Task 20.6: Write Integration Tests for End-to-End Workflows

## Jira Reference
- **Issue Key**: SPTV2-227
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-227
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-174 — Integration and Wiring
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 20: Integration
- **Complexity**: L
- **Requirements**: 4.1, 6.8, 14.4, 17.1, 22.3

## Description
- Test: create master data → generate → review → approve → publish → notify
- Test: concurrent coordinator access to same department draft
- Test: add/drop → roster update → calendar feed refresh
- Test: resource block → session displacement → alternative → approval
- Test: cross-campus faculty scheduling with travel buffer

## Dependencies
- **Depends on**: Task 20.5 — SPTV2-226
- **Blocks**: None
- **Cross-role**: No

## Definition of Done
- [ ] All 5 integration test scenarios passing
- [ ] Concurrent access tested
- [ ] Full pipeline verified
