# Task 6.2: Implement Constraint Propagation (AC-3 Variant)

## Jira Reference
- **Issue Key**: SPTV2-153
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-153
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-151 — Scheduling Engine
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 6: Scheduling Engine
- **Complexity**: XL
- **Requirements**: 12.1, 6.1, 6.2, 7.2, 7.6, 9.7
- **Design Reference**: Scheduling engine core

## Description
- Implement arc consistency algorithm for domain pruning
- Handle all hard constraint types: faculty double-booking, room double-booking, batch clashes, capacity, hard blocks, common slots, travel-time buffers
- Detect early infeasibility when any domain becomes empty

## Dependencies
- **Depends on**: Task 6.1 — SPTV2-152
- **Blocks**: Task 6.3 (backtracking search)
- **Cross-role**: No

## Definition of Done
- [ ] AC-3 algorithm implemented
- [ ] All hard constraints enforced
- [ ] Early infeasibility detection working
