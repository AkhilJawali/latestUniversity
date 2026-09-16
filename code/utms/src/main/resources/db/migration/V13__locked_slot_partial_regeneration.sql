-- V13: Locked Slot Preservation and Partial Re-Generation
-- Story: A4-14
-- Design: KD-60 (is_approved column), KD-61 (regeneration provenance), PD-83..85
-- Note: The design doc named this migration V12, but V12 was taken by A4-13
--       (fortnightly patterns) in parallel work; this migration is therefore V13.
-- Rollback:
--   DROP INDEX idx_session_draft_fixed;
--   ALTER TABLE generation_requests DROP COLUMN regeneration_scope;
--   ALTER TABLE generation_requests DROP CONSTRAINT fk_generation_requests_source_draft;
--   ALTER TABLE generation_requests DROP COLUMN source_draft_id;
--   ALTER TABLE scheduled_sessions DROP COLUMN is_approved;

-- ============================================================
-- Approved flag on each session (PD-83, KD-60).
-- Written by the approval workflow (A4-18) later; read by the scheduling
-- engine now, which treats approved sessions as immovable/fixed (HC-LOCK-2).
-- Existing rows default to false. is_locked already exists from V10 (no re-add).
-- ============================================================
ALTER TABLE scheduled_sessions
    ADD COLUMN is_approved BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN scheduled_sessions.is_approved IS
    'Set by approval workflow (A4-18). Treated as immovable/fixed by the scheduling engine (A4-14).';

-- ============================================================
-- Regeneration provenance on the generation request (KD-61).
-- source_draft_id: which draft a partial re-generation was based on.
-- regeneration_scope: JSON selector {batchIds, sectionIds, courseIds}.
-- Both NULL for a full generation; populated for a partial re-generation.
-- ============================================================
ALTER TABLE generation_requests
    ADD COLUMN source_draft_id BIGINT NULL;

ALTER TABLE generation_requests
    ADD CONSTRAINT fk_generation_requests_source_draft
    FOREIGN KEY (source_draft_id) REFERENCES timetable_drafts(id);

ALTER TABLE generation_requests
    ADD COLUMN regeneration_scope TEXT NULL;

COMMENT ON COLUMN generation_requests.regeneration_scope IS
    'JSON selector {batchIds,sectionIds,courseIds} for partial re-generation (A4-14). NULL for full generation.';

-- ============================================================
-- Fast lookup of fixed (locked/approved) sessions for a draft during
-- partial re-generation pre-placement (HC-LOCK-4).
-- ============================================================
CREATE INDEX idx_session_draft_fixed
    ON scheduled_sessions(draft_id)
    WHERE deleted_at IS NULL AND (is_locked = TRUE OR is_approved = TRUE);
