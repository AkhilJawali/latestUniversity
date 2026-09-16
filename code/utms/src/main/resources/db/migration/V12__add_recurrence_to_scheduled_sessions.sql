-- V12: Fortnightly and Alternate-Week Scheduling Patterns
-- Story: A4-13
-- Design: KD-59 (columns on session), KD-60 (enums as STRING), PD-81..84
-- Rollback:
--   ALTER TABLE scheduled_sessions DROP CONSTRAINT ck_session_recurrence_weekgroup;
--   DROP INDEX idx_session_recurrence;
--   ALTER TABLE scheduled_sessions DROP COLUMN week_group;
--   ALTER TABLE scheduled_sessions DROP COLUMN recurrence_type;

-- Recurrence pattern on each session. Existing rows default to WEEKLY,
-- preserving the canonical weekly behavior assumed by A4-11 (backward compatibility).
ALTER TABLE scheduled_sessions
    ADD COLUMN recurrence_type VARCHAR(20) NOT NULL DEFAULT 'WEEKLY';

ALTER TABLE scheduled_sessions
    ADD COLUMN week_group VARCHAR(10) NULL;

-- HC-FN-4: a WEEKLY session has no week group; a FORTNIGHTLY session must
-- occupy exactly one of the two disjoint groups. Enforced at the DB as a
-- safety net behind service-layer validation.
ALTER TABLE scheduled_sessions
    ADD CONSTRAINT ck_session_recurrence_weekgroup
    CHECK (
        (recurrence_type = 'WEEKLY'      AND week_group IS NULL) OR
        (recurrence_type = 'FORTNIGHTLY' AND week_group IN ('WEEK_A', 'WEEK_B'))
    );

-- Fetch fortnightly sessions quickly for feed/display and alternate-week checks.
CREATE INDEX idx_session_recurrence
    ON scheduled_sessions(recurrence_type)
    WHERE deleted_at IS NULL AND recurrence_type <> 'WEEKLY';
