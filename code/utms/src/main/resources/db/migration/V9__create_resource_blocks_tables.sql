-- V9__create_resource_blocks_tables.sql
-- Resource blocking and availability workflow tables

CREATE TABLE utms.resource_blocks (
    id              BIGSERIAL PRIMARY KEY,
    resource_type   VARCHAR(10) NOT NULL CHECK (resource_type IN ('ROOM', 'ASSET')),
    resource_id     BIGINT NOT NULL,
    block_type      VARCHAR(10) NOT NULL CHECK (block_type IN ('HARD', 'SOFT')),
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    reason_code     VARCHAR(50) NOT NULL,
    reason_text     VARCHAR(500),
    recurrence_pattern VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL'
                    CHECK (status IN ('PENDING_APPROVAL', 'ACTIVE', 'RELEASED', 'REJECTED', 'EXPIRED', 'WITHDRAWN')),
    raised_by       VARCHAR(100) NOT NULL,
    raised_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    activated_at    TIMESTAMP,
    released_at     TIMESTAMP,
    expires_at      TIMESTAMP,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP,
    CONSTRAINT chk_resource_blocks_dates CHECK (start_date <= end_date),
    CONSTRAINT chk_resource_blocks_times CHECK (start_time < end_time)
);

CREATE INDEX idx_resource_blocks_resource ON utms.resource_blocks(resource_type, resource_id, status)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_resource_blocks_active_dates ON utms.resource_blocks(start_date, end_date)
    WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX idx_resource_blocks_status ON utms.resource_blocks(status)
    WHERE deleted_at IS NULL;

CREATE TABLE utms.block_approval_actions (
    id              BIGSERIAL PRIMARY KEY,
    block_id        BIGINT NOT NULL,
    action          VARCHAR(20) NOT NULL CHECK (action IN ('APPROVE', 'REJECT', 'WITHDRAW')),
    actor_id        VARCHAR(100) NOT NULL,
    comments        VARCHAR(1000),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP,
    CONSTRAINT fk_block_approval_actions_block FOREIGN KEY (block_id) REFERENCES utms.resource_blocks(id)
);

CREATE INDEX idx_block_approval_actions_block_id ON utms.block_approval_actions(block_id)
    WHERE deleted_at IS NULL;

CREATE TABLE utms.soft_block_overrides (
    id              BIGSERIAL PRIMARY KEY,
    block_id        BIGINT NOT NULL,
    session_id      BIGINT,
    justification   VARCHAR(500) NOT NULL,
    overridden_by   VARCHAR(100) NOT NULL,
    overridden_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP,
    CONSTRAINT fk_soft_block_overrides_block FOREIGN KEY (block_id) REFERENCES utms.resource_blocks(id)
);

CREATE INDEX idx_soft_block_overrides_block_id ON utms.soft_block_overrides(block_id)
    WHERE deleted_at IS NULL;
