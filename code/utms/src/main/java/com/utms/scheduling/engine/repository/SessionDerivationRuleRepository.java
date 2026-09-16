package com.utms.scheduling.engine.repository;

import com.utms.scheduling.engine.entity.SessionDerivationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionDerivationRuleRepository
        extends JpaRepository<SessionDerivationRule, Long>, JpaSpecificationExecutor<SessionDerivationRule> {

    List<SessionDerivationRule> findByCampusIdAndIsActiveTrue(Long campusId);

    boolean existsByCampusIdAndDeletedAtIsNull(Long campusId);

    Optional<SessionDerivationRule> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCampusIdAndComponentTypeAndDeletedAtIsNull(Long campusId, String componentType);
}
