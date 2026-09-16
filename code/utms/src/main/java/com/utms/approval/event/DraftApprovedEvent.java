package com.utms.approval.event;

import java.time.Instant;

/**
 * Published (AFTER_COMMIT) when a draft receives final approval (A4-19 FR-2.2 / PD-104 /
 * KD-A19-3). This story sets {@code DraftStatus=APPROVED}; the publication story (A4-21)
 * listens for this event and performs the PUBLISHED transition + side-effects
 * (notifications, calendar feed). Kept as a plain record so A4-21 can consume it without a
 * dependency back on this package's services.
 */
public record DraftApprovedEvent(
        Long draftId,
        Long workflowInstanceId,
        String approvedByUserId,
        Instant approvedAt
) {
}
