package com.utms.scheduling.engine.enums;

/**
 * Status lifecycle for a timetable draft.
 * SUPERSEDED is distinct from soft-delete — superseded drafts remain queryable for version comparison (A4-19).
 */
public enum DraftStatus {
    DRAFT,
    UNDER_REVIEW,
    APPROVED,
    PUBLISHED,
    SUPERSEDED
}
