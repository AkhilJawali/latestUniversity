-- V10: Scheduling Engine Tables
-- Story: A4-11 (Timetable Generation Engine)
-- Design: KD-46 (thread pool), KD-47 (session variable), KD-52 (common slots), PD-68 (concurrency), PD-70 (weights), PD-74 (derivation rules)

-- ============================================================
-- Generation Requests
-- ============================================================
CREATE TABLE generation_requests (
    id              BIGSERIAL PRIMARY KEY,
    department_id   BIGINT NOT NULL,
    semester        VARCHAR(20) NOT NULL,
    academic_year   VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    progress        INTEGER NOT NULL DEFAULT 0,
    phase           VARCHAR(30) NULL,
    seed            BIGINT NULL,
    triggered_by    VARCHAR(100) NOT NULL,
    triggered_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP NULL,
    error_message   TEXT NULL,
    draft_id        BIGINT NULL,
    elapsed_ms      BIGINT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,
    CONSTRAINT fk_genreq_department FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- PD-68: Only one IN_PROGRESS generation per department+semester
CREATE UNIQUE INDEX uq_genreq_dept_semester_inprogress
    ON generation_requests(department_id, semester)
    WHERE status = 'IN_PROGRESS' AND deleted_at IS NULL;

CREATE INDEX idx_genreq_department_id ON generation_requests(department_id);
CREATE INDEX idx_genreq_status ON generation_requests(status) WHERE deleted_at IS NULL;

-- ============================================================
-- Timetable Drafts
-- ============================================================
CREATE TABLE timetable_drafts (
    id                      BIGSERIAL PRIMARY KEY,
    department_id           BIGINT NOT NULL,
    semester                VARCHAR(20) NOT NULL,
    academic_year           VARCHAR(20) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version                 INTEGER NOT NULL DEFAULT 1,
    feasibility_score       DECIMAL(4,3) NOT NULL DEFAULT 0.000,
    quality_score           DECIMAL(4,3) NOT NULL DEFAULT 0.000,
    total_sessions_required INTEGER NOT NULL DEFAULT 0,
    total_sessions_placed   INTEGER NOT NULL DEFAULT 0,
    generation_request_id   BIGINT NOT NULL,
    generated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(100) NOT NULL,
    updated_by              VARCHAR(100) NOT NULL,
    deleted_at              TIMESTAMP NULL,
    CONSTRAINT fk_draft_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_draft_genreq FOREIGN KEY (generation_request_id) REFERENCES generation_requests(id)
);

CREATE INDEX idx_draft_department_semester ON timetable_drafts(department_id, semester) WHERE deleted_at IS NULL;
CREATE INDEX idx_draft_status ON timetable_drafts(status) WHERE deleted_at IS NULL;

-- ============================================================
-- Scheduled Sessions
-- ============================================================
CREATE TABLE scheduled_sessions (
    id                  BIGSERIAL PRIMARY KEY,
    draft_id            BIGINT NOT NULL,
    course_id           BIGINT NOT NULL,
    faculty_id          BIGINT NOT NULL,
    batch_id            BIGINT NOT NULL,
    section_id          BIGINT NULL,
    room_id             BIGINT NOT NULL,
    day_of_week         VARCHAR(10) NOT NULL,
    slot_definition_id  BIGINT NOT NULL,
    session_type        VARCHAR(20) NOT NULL,
    is_locked           BOOLEAN NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,
    deleted_at          TIMESTAMP NULL,
    CONSTRAINT fk_session_draft FOREIGN KEY (draft_id) REFERENCES timetable_drafts(id),
    CONSTRAINT fk_session_course FOREIGN KEY (course_id) REFERENCES courses(id),
    CONSTRAINT fk_session_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(id),
    CONSTRAINT fk_session_batch FOREIGN KEY (batch_id) REFERENCES batches(id),
    CONSTRAINT fk_session_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_session_slot FOREIGN KEY (slot_definition_id) REFERENCES slot_definitions(id)
);

CREATE INDEX idx_session_draft ON scheduled_sessions(draft_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_session_faculty_day ON scheduled_sessions(faculty_id, day_of_week) WHERE deleted_at IS NULL;
CREATE INDEX idx_session_room_day ON scheduled_sessions(room_id, day_of_week) WHERE deleted_at IS NULL;
CREATE INDEX idx_session_batch_day ON scheduled_sessions(batch_id, day_of_week) WHERE deleted_at IS NULL;

-- ============================================================
-- Soft Constraint Violations
-- ============================================================
CREATE TABLE soft_constraint_violations (
    id                      BIGSERIAL PRIMARY KEY,
    draft_id                BIGINT NOT NULL,
    constraint_type         VARCHAR(40) NOT NULL,
    affected_entity_type    VARCHAR(20) NOT NULL,
    affected_entity_id      BIGINT NOT NULL,
    description             TEXT NOT NULL,
    relaxation_reason       TEXT NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(100) NOT NULL,
    updated_by              VARCHAR(100) NOT NULL,
    deleted_at              TIMESTAMP NULL,
    CONSTRAINT fk_violation_draft FOREIGN KEY (draft_id) REFERENCES timetable_drafts(id)
);

CREATE INDEX idx_violation_draft ON soft_constraint_violations(draft_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_violation_type ON soft_constraint_violations(constraint_type) WHERE deleted_at IS NULL;

-- ============================================================
-- Session Derivation Rules (PD-74: configurable L-T-P to session mapping)
-- ============================================================
CREATE TABLE session_derivation_rules (
    id                    BIGSERIAL PRIMARY KEY,
    campus_id             BIGINT NOT NULL,
    component_type        VARCHAR(10) NOT NULL,
    slot_duration_minutes INTEGER NOT NULL,
    hours_per_session     DECIMAL(3,1) NOT NULL,
    description           VARCHAR(200) NULL,
    is_active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100) NOT NULL,
    updated_by            VARCHAR(100) NOT NULL,
    deleted_at            TIMESTAMP NULL,
    CONSTRAINT fk_derivation_campus FOREIGN KEY (campus_id) REFERENCES campuses(id),
    CONSTRAINT uq_derivation_campus_type UNIQUE (campus_id, component_type)
);

CREATE INDEX idx_derivation_campus ON session_derivation_rules(campus_id) WHERE deleted_at IS NULL;

-- ============================================================
-- Soft Constraint Weights (PD-70: configurable quality score weights)
-- ============================================================
CREATE TABLE soft_constraint_weights (
    id              BIGSERIAL PRIMARY KEY,
    campus_id       BIGINT NOT NULL,
    constraint_type VARCHAR(40) NOT NULL,
    weight          DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,
    CONSTRAINT fk_weight_campus FOREIGN KEY (campus_id) REFERENCES campuses(id),
    CONSTRAINT uq_weight_campus_type UNIQUE (campus_id, constraint_type)
);

CREATE INDEX idx_weight_campus ON soft_constraint_weights(campus_id) WHERE deleted_at IS NULL;

-- ============================================================
-- Institution Common Slots (KD-52: CCC/UWE pre-placed blockers)
-- ============================================================
CREATE TABLE institution_common_slots (
    id                     BIGSERIAL PRIMARY KEY,
    campus_id              BIGINT NOT NULL,
    name                   VARCHAR(100) NOT NULL,
    day_of_week            VARCHAR(10) NOT NULL,
    slot_definition_id     BIGINT NOT NULL,
    applies_to_all_batches BOOLEAN NOT NULL DEFAULT TRUE,
    is_active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by             VARCHAR(100) NOT NULL,
    updated_by             VARCHAR(100) NOT NULL,
    deleted_at             TIMESTAMP NULL,
    CONSTRAINT fk_commonslot_campus FOREIGN KEY (campus_id) REFERENCES campuses(id),
    CONSTRAINT fk_commonslot_slot FOREIGN KEY (slot_definition_id) REFERENCES slot_definitions(id)
);

CREATE INDEX idx_commonslot_campus ON institution_common_slots(campus_id) WHERE deleted_at IS NULL;
