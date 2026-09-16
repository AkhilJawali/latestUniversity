# Task 10.1: Implement Accreditation Norm Configuration

## Jira Reference
- **Issue Key**: SPTV2-188
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-188
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-167 — Compliance Module
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 10: Compliance Module
- **Complexity**: M
- **Requirements**: 13.1, 13.2
- **Design Reference**: Compliance

## Description
- Create ComplianceModuleService with configurable NBA/NAAC/UGC norms
- Support min/max weekly hours per faculty cadre
- Support school-specific credit-to-contact-hour ratios
- Store student-faculty contact hours per course requirement

## Dependencies
- **Depends on**: Task 8.1 — SPTV2-175
- **Blocks**: Task 10.2 (compliance validation)
- **Cross-role**: No

## Definition of Done
- [ ] Norm configuration CRUD
- [ ] Per-cadre min/max hours
- [ ] Credit-to-contact ratios configurable
