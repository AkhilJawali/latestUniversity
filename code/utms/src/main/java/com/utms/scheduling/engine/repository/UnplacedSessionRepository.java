package com.utms.scheduling.engine.repository;

import com.utms.scheduling.engine.entity.UnplacedSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnplacedSessionRepository extends JpaRepository<UnplacedSession, Long> {

    List<UnplacedSession> findByDraftIdAndDeletedAtIsNull(Long draftId);
}
