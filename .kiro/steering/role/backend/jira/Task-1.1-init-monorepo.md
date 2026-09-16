# Task 1.1: Initialize Spring Boot Project Structure

## Jira Reference
- **Issue Key**: SPTV2-122
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-122
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-121 — Project Setup and Infrastructure
- **Status**: Done

## Task Details
- **Role**: backend
- **Phase**: Phase 1: Project Setup and Infrastructure
- **Complexity**: M
- **Requirements**: 22.1, 22.3
- **Design Reference**: Infrastructure setup

## Description
- Create single-module Maven project with package namespace `com.utms`
- Configure Java 17+, Spring Boot 3.x with Spring Web, Spring Data JPA, Spring Security
- Set up Checkstyle, Spotless, and conventional commit hooks
- Add pom.xml with all dependencies pinned to exact versions

## Dependencies
- **Depends on**: None (first task)
- **Blocks**: Task 1.2, 1.3, 1.5
- **Cross-role**: No

## Definition of Done
- [ ] Maven project structure created
- [ ] Spring Boot application starts
- [ ] Checkstyle + Spotless configured and passing
- [ ] Conventional commit hooks working
