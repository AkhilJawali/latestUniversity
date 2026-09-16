-- V11: Timeout and Infeasibility Handling
-- Story: A4-12 (Timetable Generation — Timeout and Infeasibility Handling)
-- Design: KD-55 (new status values), KD-57 (bestSoFarCount), KD-58 (infeasibility tables)
-- PD-76 (configurable timeout), PD-77 (partial on infeasibility)

-- ============================================================
-- Extend generation_requests with timeout/progress columns
-- ============================================================
ALTER TABLE generation_requests ADD COLUMN best_so_far_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE generation_requests ADD COLUMN total_sessions INTEGER NULL;
ALTER TABLE generation_requests ADD COLUMN timeout_duration_seconds INTEGER NOT NULL DEFAULT 120;

-- ============================================================
-- Mark partial drafts explicitly (FR-1.4, PD-80)
-- ============================================================
ALTER TABLE timetable_drafts ADD COLUMN is_partial BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE timetable_drafts ADD COLUMN partial_acknowledged BOOLEAN NOT NULL DEFAULT FALSE;

-- ============================================================
-- Infeasibility Reports (KD-58: one per generation request)
-- ============================================================
CREATE TABLE infeasibility_reports (
    id                      BIGSERIAL PRIMARY KEY,
    generation_request_id   BIGINT NOT NULL,
    detected_at             TIMESTAMP NOT NULL,
    summary                 TEXT NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(100) NOT NULL,
    updated_by              VARCHAR(100) NOT NULL,
    deleted_at              TIMESTAMP NULL,
    CONSTRAINT fk_infeasibility_report_request FOREIGN KEY (generation_request_id) REFERENCES generation_requests(id)
);

-- One report per request (1:1)
CREATE UNIQUE INDEX uk_infeasibility_report_request
    ON infeasibility_reports(generation_request_id)
    WHERE deleted_at IS NULL;

-- ============================================================
-- Infeasibility Conflicts (one per unplaceable session)
-- ============================================================
CREATE TABLE infeasibility_conflicts (
    id                              BIGSERIAL PRIMARY KEY,
    report_id                       BIGINT NOT NULL,
    affected_session_description    VARCHAR(500) NOT NULL,
    conflicting_constraints         TEXT NOT NULL,
    explanation                     TEXT NOT NULL,
    is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by                      VARCHAR(100) NOT NULL,
    updated_by                      VARCHAR(100) NOT NULL,
    deleted_at                      TIMESTAMP NULL,
    CONSTRAINT fk_infeasibility_conflict_report FOREIGN KEY (report_id) REFERENCES infeasibility_reports(id)
);

CREATE INDEX idx_infeasibility_conflicts_report ON infeasibility_conflicts(report_id);

-- ============================================================
-- Unplaced Sessions (FR-2.5: persisted on timeout/infeasibility)
-- ============================================================
CREATE TABLE unplaced_sessions (
    id                          BIGSERIAL PRIMARY KEY,
    draft_id                    BIGINT NOT NULL,
    course_id                   BIGINT NOT NULL,
    course_code                 VARCHAR(50) NOT NULL,
    course_name                 VARCHAR(200) NOT NULL,
    faculty_id                  BIGINT NOT NULL,
    faculty_name                VARCHAR(200) NOT NULL,
    batch_id                    BIGINT NOT NULL,
    batch_name                  VARCHAR(100) NOT NULL,
    session_type                VARCHAR(20) NOT NULL,
    required_duration_minutes   INTEGER NOT NULL,
    reason                      VARCHAR(500) NULL,
    is_active                   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by                  VARCHAR(100) NOT NULL,
    updated_by                  VARCHAR(100) NOT NULL,
    deleted_at                  TIMESTAMP NULL,
    CONSTRAINT fk_unplaced_sessions_draft FOREIGN KEY (draft_id) REFERENCES timetable_drafts(id)
);

CREATE INDEX idx_unplaced_sessions_draft ON unplaced_sessions(draft_id);
