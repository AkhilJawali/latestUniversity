# Task 9.3: Implement Publication Trigger and Notifications

## Jira Reference
- **Issue Key**: SPTV2-185
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-185
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-166 — Approval Workflow
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 9: Approval Workflow
- **Complexity**: M
- **Requirements**: 10.5
- **Design Reference**: Publication flow

## Description
- On final approval: transition status to "published"
- Emit publication event to Notification Service for all affected faculty/students
- Trigger calendar feed refresh for affected users

## Dependencies
- **Depends on**: Task 9.1 — SPTV2-183
- **Blocks**: Task 11.1 (notification delivery)
- **Cross-role**: No

## Definition of Done
- [ ] Publication transition on final approval
- [ ] Notification events emitted
- [ ] Calendar feed refresh triggered
