package com.utms.approval.enums;

/**
 * The action recorded on a {@code WorkflowStep} of the approval audit trail (A4-19, FR-4).
 * Append-only: one row per action.
 */
public enum WorkflowAction {
    SUBMITTED,
    APPROVED,
    REJECTED
}
