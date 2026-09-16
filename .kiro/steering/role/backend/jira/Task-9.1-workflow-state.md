# Task 9.1: Implement Workflow State Machine

## Jira Reference
- **Issue Key**: SPTV2-183
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-183
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-166 — Approval Workflow
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 9: Approval Workflow
- **Complexity**: L
- **Requirements**: 10.1, 10.2, 10.4
- **Design Reference**: Approval workflow

## Description
- Create ApprovalWorkflowService with configurable multi-level pipeline
- Support levels: Coordinator → HOD → Dean/Registrar (configurable)
- Implement state transitions: draft → under_review → approved → published
- Handle rejection: revert to previous level with comments and reason
- Record all transitions in audit trail

## Dependencies
- **Depends on**: Task 8.2 — SPTV2-176
- **Blocks**: Task 9.3 (publication), Task 18.4 (dashboard)
- **Cross-role**: Yes — blocks SPTV2-218 (18.4 coordinator dashboard)

## Definition of Done
- [ ] Multi-level pipeline configurable
- [ ] State transitions working
- [ ] Rejection with comments
- [ ] Audit trail integration
