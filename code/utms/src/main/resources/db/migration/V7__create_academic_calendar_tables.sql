-- V7__create_academic_calendar_tables.sql
-- Academic Calendar: calendars, holidays, exam windows, orientation periods, working day patterns

-- Academic Calendar (one per campus+year+semester — KD-40)
CREATE TABLE utms.academic_calendars (
    id                    BIGSERIAL PRIMARY KEY,
    campus_id             BIGINT NOT NULL,
    academic_year         VARCHAR(20) NOT NULL,
    semester_identifier   VARCHAR(30) NOT NULL,
    semester_start_date   DATE NOT NULL,
    semester_end_date     DATE NOT NULL,
    is_active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100) NOT NULL,
    updated_by            VARCHAR(100) NOT NULL,
    deleted_at            TIMESTAMP NULL,
    CONSTRAINT fk_academic_calendars_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id),
    CONSTRAINT chk_calendar_date_order CHECK (semester_start_date <= semester_end_date)
);

-- Unique: one calendar per campus + academic year + semester (KD-40)
CREATE UNIQUE INDEX uq_academic_calendars_scope ON utms.academic_calendars(campus_id, academic_year, semester_identifier) WHERE deleted_at IS NULL;
CREATE INDEX idx_academic_calendars_campus_id ON utms.academic_calendars(campus_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_academic_calendars_year ON utms.academic_calendars(academic_year) WHERE deleted_at IS NULL;

-- Calendar Holidays (individual dates or date ranges with scope)
CREATE TABLE utms.calendar_holidays (
    id              BIGSERIAL PRIMARY KEY,
    calendar_id     BIGINT NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    description     VARCHAR(200) NOT NULL,
    scope           VARCHAR(20) NOT NULL CHECK (scope IN ('CAMPUS_SPECIFIC', 'INSTITUTION_WIDE')),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,
    CONSTRAINT fk_calendar_holidays_calendars FOREIGN KEY (calendar_id) REFERENCES utms.academic_calendars(id),
    CONSTRAINT chk_holiday_date_order CHECK (start_date <= end_date)
);

CREATE INDEX idx_calendar_holidays_calendar_id ON utms.calendar_holidays(calendar_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_calendar_holidays_dates ON utms.calendar_holidays(calendar_id, start_date, end_date) WHERE deleted_at IS NULL;

-- Calendar Exam Windows (mid-sem, end-sem, supplementary)
CREATE TABLE utms.calendar_exam_windows (
    id              BIGSERIAL PRIMARY KEY,
    calendar_id     BIGINT NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    exam_type       VARCHAR(30) NOT NULL CHECK (exam_type IN ('MID_SEMESTER', 'END_SEMESTER', 'SUPPLEMENTARY')),
    description     VARCHAR(200) NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,
    CONSTRAINT fk_calendar_exam_windows_calendars FOREIGN KEY (calendar_id) REFERENCES utms.academic_calendars(id),
    CONSTRAINT chk_exam_window_date_order CHECK (start_date <= end_date)
);

CREATE INDEX idx_calendar_exam_windows_calendar_id ON utms.calendar_exam_windows(calendar_id) WHERE deleted_at IS NULL;

-- Calendar Orientation/Induction Periods
CREATE TABLE utms.calendar_orientation_periods (
    id              BIGSERIAL PRIMARY KEY,
    calendar_id     BIGINT NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    description     VARCHAR(200) NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,
    CONSTRAINT fk_calendar_orientation_periods_calendars FOREIGN KEY (calendar_id) REFERENCES utms.academic_calendars(id),
    CONSTRAINT chk_orientation_date_order CHECK (start_date <= end_date)
);

CREATE INDEX idx_calendar_orientation_periods_calendar_id ON utms.calendar_orientation_periods(calendar_id) WHERE deleted_at IS NULL;

-- Working Day Patterns (one required per campus — KD-39)
CREATE TABLE utms.working_day_patterns (
    id                  BIGSERIAL PRIMARY KEY,
    campus_id           BIGINT NOT NULL,
    pattern_type        VARCHAR(30) NOT NULL CHECK (pattern_type IN ('FIVE_DAY', 'SIX_DAY', 'ALTERNATE_SATURDAY', 'CUSTOM')),
    working_saturdays   VARCHAR(100) NULL,
    custom_definition   TEXT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,
    deleted_at          TIMESTAMP NULL,
    CONSTRAINT fk_working_day_patterns_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id)
);

-- One active pattern per campus (KD-39: required)
CREATE UNIQUE INDEX uq_working_day_patterns_campus ON utms.working_day_patterns(campus_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_working_day_patterns_campus_id ON utms.working_day_patterns(campus_id) WHERE deleted_at IS NULL;
