package com.utms.scheduling.engine.repository;

import com.utms.scheduling.engine.entity.ScheduledSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledSessionRepository extends JpaRepository<ScheduledSession, Long> {

    List<ScheduledSession> findByDraftIdAndDeletedAtIsNull(Long draftId);

    Page<ScheduledSession> findByDraftIdAndDeletedAtIsNull(Long draftId, Pageable pageable);

    Optional<ScheduledSession> findByIdAndDeletedAtIsNull(Long id);
}
