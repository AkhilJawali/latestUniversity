-- V4__create_faculty_availability_tables.sql
-- Faculty availability windows and preferences for scheduling constraints

CREATE TABLE utms.faculty_availability_windows (
    id              BIGSERIAL PRIMARY KEY,
    faculty_id      BIGINT NOT NULL,
    day_of_week     VARCHAR(10) NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    reason_code     VARCHAR(50) NOT NULL,
    reason_note     VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,
    CONSTRAINT chk_faculty_avail_time_order CHECK (start_time < end_time),
    CONSTRAINT fk_faculty_availability_windows_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id)
);

CREATE INDEX idx_faculty_availability_windows_faculty_day
    ON utms.faculty_availability_windows(faculty_id, day_of_week)
    WHERE deleted_at IS NULL;

CREATE TABLE utms.faculty_preferences (
    id                      BIGSERIAL PRIMARY KEY,
    faculty_id              BIGINT NOT NULL,
    preferred_time_of_day   VARCHAR(20) NOT NULL CHECK (preferred_time_of_day IN ('MORNING', 'AFTERNOON', 'NO_PREFERENCE')),
    session_distribution    VARCHAR(20) NOT NULL CHECK (session_distribution IN ('CONSECUTIVE', 'SPREAD', 'NO_PREFERENCE')),
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(100) NOT NULL,
    updated_by              VARCHAR(100) NOT NULL,
    CONSTRAINT uq_faculty_preferences_faculty UNIQUE (faculty_id),
    CONSTRAINT fk_faculty_preferences_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id)
);

CREATE INDEX idx_faculty_preferences_faculty_id ON utms.faculty_preferences(faculty_id);
