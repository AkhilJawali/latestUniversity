# Task 6.10: Implement Lab and Practical Session Scheduling

## Jira Reference
- **Issue Key**: SPTV2-161
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-161
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-151 — Scheduling Engine
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 6: Scheduling Engine
- **Complexity**: L
- **Requirements**: 8.1, 8.2, 8.3, 8.4, 8.5
- **Design Reference**: Lab scheduling

## Description
- Implement batch-splitting logic (divide batch into N sub-groups)
- Allocate separate lab slots per sub-group with matching equipment tags
- Support block scheduling (2-3 contiguous periods)
- Enforce compressed window constraints (e.g., 2 PM-5 PM)
- Treat lab technician availability as hard constraint

## Dependencies
- **Depends on**: Task 6.2 — SPTV2-153
- **Blocks**: Task 6.11 (lab tests)
- **Cross-role**: No

## Definition of Done
- [ ] Batch-splitting working
- [ ] Equipment tag matching
- [ ] Block scheduling (contiguous periods)
- [ ] Compressed window enforced
- [ ] Lab tech availability as hard constraint
