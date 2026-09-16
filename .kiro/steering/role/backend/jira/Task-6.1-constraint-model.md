# Task 6.1: Implement Constraint Model and Data Structures

## Jira Reference
- **Issue Key**: SPTV2-152
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-152
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-151 — Scheduling Engine
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 6: Scheduling Engine
- **Complexity**: L
- **Requirements**: 12.1, 12.2, 4.1
- **Design Reference**: Scheduling engine core

## Description
- Define HardConstraint and SoftConstraint types with discrimination unions
- Implement ConstraintSet builder that loads constraints from database
- Define Variable (session-to-assign) and Domain (possible slot+room assignments) structures
- Implement constraint weight configuration (soft constraint priority ordering)

## Dependencies
- **Depends on**: Task 4.3 — SPTV2-142, Task 4.4 — SPTV2-143
- **Blocks**: Task 6.2 (constraint propagation)
- **Cross-role**: No

## Definition of Done
- [ ] Hard/soft constraint types defined
- [ ] ConstraintSet builder loads from DB
- [ ] Variable/Domain structures implemented
- [ ] Weight configuration working
