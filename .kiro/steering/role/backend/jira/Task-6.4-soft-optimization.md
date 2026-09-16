# Task 6.4: Implement Soft-Constraint Optimization (Local Search)

## Jira Reference
- **Issue Key**: SPTV2-155
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-155
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-151 — Scheduling Engine
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 6: Scheduling Engine
- **Complexity**: L
- **Requirements**: 12.2, 12.5, 6.3, 7.1
- **Design Reference**: Scheduling engine optimization

## Description
- Implement hill-climbing with random restarts for soft-constraint improvement
- Optimize: faculty time preferences, room proximity, gap minimization, day-pattern balance
- Record each soft-constraint relaxation with justification reason
- Implement quality score computation based on weighted soft-constraint satisfaction

## Dependencies
- **Depends on**: Task 6.3 — SPTV2-154
- **Blocks**: Task 6.5 (worker isolation)
- **Cross-role**: No

## Definition of Done
- [ ] Hill-climbing optimization working
- [ ] Quality score computed
- [ ] Relaxation reasons recorded
