# Task 6.3: Implement Backtracking Search with MRV and LCV

## Jira Reference
- **Issue Key**: SPTV2-154
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-154
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-151 — Scheduling Engine
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 6: Scheduling Engine
- **Complexity**: XL
- **Requirements**: 4.1, 4.4, 12.1
- **Design Reference**: Scheduling engine core

## Description
- Implement variable ordering using Minimum Remaining Values (MRV)
- Implement value ordering using Least Constraining Value (LCV)
- Implement backtracking with chronological and conflict-directed strategies
- Handle locked slots as pre-assigned immovable variables

## Dependencies
- **Depends on**: Task 6.2 — SPTV2-153
- **Blocks**: Task 6.4 (soft-constraint optimization)
- **Cross-role**: No

## Definition of Done
- [ ] MRV variable ordering working
- [ ] LCV value ordering working
- [ ] Backtracking finds valid solutions
- [ ] Locked slots preserved
