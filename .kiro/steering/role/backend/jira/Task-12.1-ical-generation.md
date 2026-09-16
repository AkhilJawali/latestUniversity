# Task 12.1: Implement iCal Feed Generation

## Jira Reference
- **Issue Key**: SPTV2-195
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-195
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-169 — Calendar Export Service
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 12: Calendar Export Service
- **Complexity**: M
- **Requirements**: 16.1, 16.2
- **Design Reference**: Calendar export

## Description
- Create CalendarExportService generating RFC 5545 compliant .ics feeds
- Generate one calendar event per published session with correct day, time, room, course
- Support personal feeds per faculty member and student
- Implement feed URL generation with secure, non-guessable tokens

## Dependencies
- **Depends on**: Task 9.3 — SPTV2-185
- **Blocks**: Task 12.2 (live subscription)
- **Cross-role**: No

## Definition of Done
- [ ] RFC 5545 compliant .ics output
- [ ] Personal feeds per user
- [ ] Secure token-based URLs
