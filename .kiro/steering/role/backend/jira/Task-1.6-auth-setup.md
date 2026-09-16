# Task 1.6: Set Up Authentication and Session Management

## Jira Reference
- **Issue Key**: SPTV2-127
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-127
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-121 — Project Setup and Infrastructure
- **Status**: Done

## Task Details
- **Role**: backend
- **Phase**: Phase 1: Project Setup and Infrastructure
- **Complexity**: L
- **Requirements**: 20.1, 20.3
- **Design Reference**: Authentication and session management

## Description
- Implement JWT-based authentication with refresh token rotation
- Use bcrypt/argon2 for password hashing (never plain text)
- Implement session invalidation on logout/password change/inactivity
- Generate tokens with cryptographically secure randomness
- Set HttpOnly, Secure, SameSite flags on all cookies

## Dependencies
- **Depends on**: Task 1.4 — SPTV2-125
- **Blocks**: Task 15.1 (RBAC middleware)
- **Cross-role**: No

## Definition of Done
- [ ] JWT auth flow working (login, refresh, logout)
- [ ] Password hashing with argon2/bcrypt
- [ ] Session invalidation on logout/password change
- [ ] Secure cookie flags set
- [ ] Token generation uses crypto-secure randomness
