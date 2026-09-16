package com.utms.scheduling.engine.repository;

import com.utms.scheduling.engine.entity.InfeasibilityConflict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InfeasibilityConflictRepository extends JpaRepository<InfeasibilityConflict, Long> {

    List<InfeasibilityConflict> findByReportIdAndDeletedAtIsNull(Long reportId);
}
