-- V14__create_approval_workflow_tables.sql
-- A4-19 Multi-Level Approval Workflow: configurable pipelines, per-draft workflow
-- instances, and an append-only approval audit trail (workflow_steps).
-- Reversible (drop order): workflow_steps, workflow_instances, approval_levels, approval_pipelines.

-- Configurable approval pipeline (A4-19 FR-6 / PD-101, PD-102).
CREATE TABLE utms.approval_pipelines (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    scope           VARCHAR(100),                    -- nullable: global for now (PD-102)
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP
);

CREATE INDEX idx_approval_pipelines_active ON utms.approval_pipelines(is_active)
    WHERE deleted_at IS NULL;

-- Ordered levels within a pipeline (A4-19 FR-6; KD-A19-2 level 0 = drafting/Coordinator).
CREATE TABLE utms.approval_levels (
    id              BIGSERIAL PRIMARY KEY,
    pipeline_id     BIGINT NOT NULL,
    level_index     INTEGER NOT NULL,
    level_name      VARCHAR(100) NOT NULL,
    required_role   VARCHAR(50),                     -- advisory until RBAC (PD-103)
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP,
    CONSTRAINT fk_approval_levels_pipeline FOREIGN KEY (pipeline_id)
        REFERENCES utms.approval_pipelines(id),
    CONSTRAINT uq_approval_levels_pipeline_index UNIQUE (pipeline_id, level_index)
);

CREATE INDEX idx_approval_levels_pipeline_id ON utms.approval_levels(pipeline_id)
    WHERE deleted_at IS NULL;

-- One workflow instance per draft submission (A4-19 FR-1; @Version for concurrency PD-105).
CREATE TABLE utms.workflow_instances (
    id                  BIGSERIAL PRIMARY KEY,
    draft_id            BIGINT NOT NULL,
    pipeline_id         BIGINT NOT NULL,
    current_level_index INTEGER NOT NULL,
    state               VARCHAR(20) NOT NULL
                        CHECK (state IN ('IN_REVIEW', 'APPROVED', 'REJECTED_RETURNED', 'WITHDRAWN')),
    version             BIGINT NOT NULL DEFAULT 0,   -- JPA @Version (PD-105)
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,
    deleted_at          TIMESTAMP,
    CONSTRAINT fk_workflow_instances_draft FOREIGN KEY (draft_id)
        REFERENCES utms.timetable_drafts(id),
    CONSTRAINT fk_workflow_instances_pipeline FOREIGN KEY (pipeline_id)
        REFERENCES utms.approval_pipelines(id)
);

-- HC-AW-1: at most one active (IN_REVIEW) workflow per draft.
CREATE UNIQUE INDEX uq_workflow_instances_active_draft ON utms.workflow_instances(draft_id)
    WHERE state = 'IN_REVIEW' AND deleted_at IS NULL;

-- Reviewer-queue lookups (FR-5.3 / NFR-5).
CREATE INDEX idx_workflow_instances_state_level ON utms.workflow_instances(state, current_level_index)
    WHERE deleted_at IS NULL;

-- Append-only approval audit trail (A4-19 FR-4 / HC-AW-3). No UPDATE/DELETE at the app layer.
CREATE TABLE utms.workflow_steps (
    id                      BIGSERIAL PRIMARY KEY,
    workflow_instance_id    BIGINT NOT NULL,
    level_index             INTEGER NOT NULL,
    level_name              VARCHAR(100) NOT NULL,
    action                  VARCHAR(20) NOT NULL
                            CHECK (action IN ('SUBMITTED', 'APPROVED', 'REJECTED')),
    actor_user_id           VARCHAR(100) NOT NULL,
    comments                VARCHAR(2000),
    rejection_reason        VARCHAR(500),
    acted_at                TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(100) NOT NULL,
    updated_by              VARCHAR(100) NOT NULL,
    deleted_at              TIMESTAMP,
    CONSTRAINT fk_workflow_steps_instance FOREIGN KEY (workflow_instance_id)
        REFERENCES utms.workflow_instances(id)
);

CREATE INDEX idx_workflow_steps_instance ON utms.workflow_steps(workflow_instance_id)
    WHERE deleted_at IS NULL;

-- Seed the default institution pipeline (A4-19 FR-6.2 / PD-101).
INSERT INTO utms.approval_pipelines (name, scope, is_active, created_by, updated_by)
VALUES ('Default Institution Pipeline', NULL, TRUE, 'system', 'system');

INSERT INTO utms.approval_levels (pipeline_id, level_index, level_name, required_role, created_by, updated_by)
SELECT p.id, v.level_index, v.level_name, v.required_role, 'system', 'system'
FROM utms.approval_pipelines p
CROSS JOIN (VALUES
    (0, 'Coordinator', 'COORDINATOR'),
    (1, 'HOD', 'HOD'),
    (2, 'Dean/Registrar', 'DEAN_OR_REGISTRAR')
) AS v(level_index, level_name, required_role)
WHERE p.name = 'Default Institution Pipeline';
