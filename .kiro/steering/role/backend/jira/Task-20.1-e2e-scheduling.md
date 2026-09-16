# Task 20.1: Wire End-to-End Scheduling Workflow

## Jira Reference
- **Issue Key**: SPTV2-222
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-222
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-174 — Integration and Wiring
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 20: Integration
- **Complexity**: L
- **Requirements**: 4.1, 10.5, 15.1, 16.2

## Description
- Connect: master data → engine generation → conflict validation → approval → publication → notification → calendar refresh
- Verify event propagation through all domain modules
- Ensure all multi-entity operations use database transactions
- Audit events written within same transaction as mutations

## Dependencies
- **Depends on**: Task 16.5 — SPTV2-214
- **Blocks**: Task 20.6 (integration tests)
- **Cross-role**: No

## Definition of Done
- [ ] Full pipeline connected
- [ ] Event propagation verified
- [ ] Transactions for multi-entity ops
- [ ] Audit in same transaction
