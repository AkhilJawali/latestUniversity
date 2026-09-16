# Task 11.1: Implement Notification Delivery Engine

## Jira Reference
- **Issue Key**: SPTV2-191
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-191
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-168 — Notification Service
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 11: Notification Service
- **Complexity**: L
- **Requirements**: 15.1, 15.2
- **Design Reference**: Notifications

## Description
- Create NotificationService using Bull queue for reliable async delivery
- Support channels: email (nodemailer/SendGrid), SMS (Twilio), in-app (database + WebSocket)
- Implement retry logic (3 retries with exponential backoff)
- Mark failed notifications and surface in coordinator dashboard

## Dependencies
- **Depends on**: Task 9.3 — SPTV2-185, Task 2.9 — SPTV2-137
- **Blocks**: Task 11.2 (digest)
- **Cross-role**: No

## Definition of Done
- [ ] Multi-channel delivery working
- [ ] Retry logic with backoff
- [ ] Failed notifications surfaced
