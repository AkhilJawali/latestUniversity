package com.utms.scheduling.engine.repository;

import com.utms.scheduling.engine.entity.SoftConstraintWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoftConstraintWeightRepository
        extends JpaRepository<SoftConstraintWeight, Long>, JpaSpecificationExecutor<SoftConstraintWeight> {

    List<SoftConstraintWeight> findByCampusIdAndIsActiveTrue(Long campusId);

    Optional<SoftConstraintWeight> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCampusIdAndConstraintTypeAndDeletedAtIsNull(Long campusId, String constraintType);
}
