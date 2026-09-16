# Task 2.9: Notification Schema

## Jira Reference
- **Issue Key**: SPTV2-137
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-137
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-128 — Database Schema and Migrations
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 2: Database Schema and Migrations
- **Complexity**: S
- **Requirements**: 15.3, 15.4
- **Design Reference**: Database schema

## Description
- Create tables: notifications, notification_preferences, notification_digests
- Define channel enum (email, sms, in_app) and priority levels

## Dependencies
- **Depends on**: Task 1.2 — SPTV2-123
- **Blocks**: Task 11.1 (notification service)
- **Cross-role**: No

## Definition of Done
- [ ] Migration file created and runs successfully
- [ ] Indexes created
- [ ] Foreign keys properly named
- [ ] Reversible migration
