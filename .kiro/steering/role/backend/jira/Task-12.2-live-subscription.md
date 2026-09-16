# Task 12.2: Implement Live Feed Subscription and Refresh

## Jira Reference
- **Issue Key**: SPTV2-196
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-196
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-169 — Calendar Export Service
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 12: Calendar Export Service
- **Complexity**: M
- **Requirements**: 16.2, 16.3, 16.4
- **Design Reference**: Calendar export

## Description
- Auto-update feed when sessions change (reschedule, substitution, cancellation)
- Implement HTTP endpoint for calendar app subscription (CalDAV-style)
- Support bulk export of department-wide subscription links
- Log export and subscription status per user

## Dependencies
- **Depends on**: Task 12.1 — SPTV2-195
- **Blocks**: None
- **Cross-role**: No

## Definition of Done
- [ ] Auto-update on session changes
- [ ] CalDAV-style subscription endpoint
- [ ] Bulk export links
- [ ] Export status logging
