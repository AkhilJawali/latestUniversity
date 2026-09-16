package com.utms.approval.enums;

/**
 * Overall state of an approval {@code WorkflowInstance} (A4-19).
 *
 * <ul>
 *   <li>{@link #IN_REVIEW} — the draft is progressing through review levels.</li>
 *   <li>{@link #APPROVED} — the final level approved; the draft is APPROVED and a
 *       {@code DraftApprovedEvent} has been emitted for publication (A4-21).</li>
 *   <li>{@link #REJECTED_RETURNED} — rejected at the first review level; the draft was
 *       returned to the submitter (draft status back to DRAFT).</li>
 *   <li>{@link #WITHDRAWN} — reserved for a future "withdraw submission" action (PD-106);
 *       not produced by any endpoint in this story.</li>
 * </ul>
 */
public enum WorkflowState {
    IN_REVIEW,
    APPROVED,
    REJECTED_RETURNED,
    WITHDRAWN
}
