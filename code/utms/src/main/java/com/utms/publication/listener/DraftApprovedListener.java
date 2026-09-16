package com.utms.publication.listener;

import com.utms.approval.event.DraftApprovedEvent;
import com.utms.publication.service.PublicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * A4-21 — auto-publish bridge from the approval workflow (A4-19).
 *
 * <p>Listens for {@link DraftApprovedEvent} (emitted by A4-19 on final approval) and publishes
 * the draft. It runs AFTER_COMMIT of the approval transaction (KD-A21-3): the approval is
 * already durably committed, so this listener opens a fresh transaction via
 * {@link PublicationService#publish}. Publication is best-effort (KD-A21-4): a failure here is
 * logged and swallowed so it can never roll back or invalidate the completed approval. The
 * draft can still be published manually via the publish endpoint if this auto-publish fails.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DraftApprovedListener {

    private final PublicationService publicationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDraftApproved(DraftApprovedEvent event) {
        try {
            publicationService.publish(event.draftId(), event.approvedByUserId());
        } catch (RuntimeException ex) {
            // Best-effort (KD-A21-4): approval already committed; never propagate.
            log.error("Auto-publish failed for approved draft {} (approved by {}); "
                            + "the draft can be published manually. Reason: {}",
                    event.draftId(), event.approvedByUserId(), ex.getMessage(), ex);
        }
    }
}
