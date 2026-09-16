package com.utms.scheduling.engine.repository;

import com.utms.scheduling.engine.entity.SoftConstraintViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoftConstraintViolationRepository extends JpaRepository<SoftConstraintViolation, Long> {

    List<SoftConstraintViolation> findByDraftIdAndDeletedAtIsNull(Long draftId);
}
