# Design: Academic Calendar Management

**Jira Reference:** A4-9
**Source Requirements:** docs/requirements/A4-9-academic-calendar-management-requirements.md
**Application:** Existing (UTMS monolith)
**Stack:** Java 21 (LTS) · Spring Boot 3.x (latest stable GA) · Maven
**Generated:** 2026-08-25 (v2 — fixes: removePattern impact detection, no unlabeled defaults, calendar uniqueness)

## Key Decisions

KD-38: Pattern change/removal triggers impact detection (same as holiday add). KD-39: Pattern REQUIRED per campus (no silent 5-day default). KD-40: Calendar unique per (campus+year+semester).

## Provisional Decisions

PD-52 through PD-60 resolving all 9 requirement OQs with explicit rationale.

## Migration: V8 — academic_calendars, calendar_holidays, calendar_exam_windows, calendar_orientation_periods, working_day_patterns tables. Unique index on calendar scope. One pattern per campus (required).

## Service Logic

addHoliday: persist + impact detection event. updatePattern: persist + impact detection for dates that became non-working (KD-38). removePattern: BLOCKED (422 — required per KD-39). CalendarQueryService.isWorkingDay: checks holidays, orientation, exam windows, pattern — fails loudly if no pattern exists.

## Traceability

All FRs (1-8), all HCs (1-6) mapped. FR-7 impact detection covers BOTH holiday-add AND pattern-change (KD-38 fix). FR-5 pattern is required (KD-39 fix).
