package com.utms.scheduling.conflict;

import java.util.List;

import org.springframework.stereotype.Service;

import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.repository.ScheduledSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads draft sessions and builds the occupancy index.
 * 
 * KD-61: Hydrates the DraftOccupancyIndex from persisted ScheduledSession rows.
 * 
 * Design: A4-16 §5.3 DraftOccupancyLoader
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DraftOccupancyLoader {

    private final ScheduledSessionRepository sessionRepository;

    /**
     * Loads all sessions for a draft and builds the occupancy index.
     * 
     * @param draftId the draft ID
     * @return populated occupancy index
     */
    public DraftOccupancyIndex loadForDraft(Long draftId) {
        log.debug("Loading occupancy index for draft {}", draftId);
        
        DraftOccupancyIndex index = new DraftOccupancyIndex();
        
        List<ScheduledSession> sessions = sessionRepository.findByDraftIdAndDeletedAtIsNull(draftId);
        
        for (ScheduledSession session : sessions) {
            index.addSession(session);
        }
        
        log.debug("Loaded {} sessions for draft {}", sessions.size(), draftId);
        return index;
    }

    /**
     * Loads sessions for multiple drafts and builds a combined occupancy index.
     * Used for cross-draft conflict detection.
     * 
     * @param draftIds the draft IDs
     * @return populated occupancy index
     */
    public DraftOccupancyIndex loadForDrafts(List<Long> draftIds) {
        log.debug("Loading occupancy index for {} drafts", draftIds.size());
        
        DraftOccupancyIndex index = new DraftOccupancyIndex();
        
        for (Long draftId : draftIds) {
            List<ScheduledSession> sessions = sessionRepository.findByDraftIdAndDeletedAtIsNull(draftId);
            for (ScheduledSession session : sessions) {
                index.addSession(session);
            }
        }
        
        log.debug("Loaded {} total sessions", index.getAllSessions().size());
        return index;
    }
}
