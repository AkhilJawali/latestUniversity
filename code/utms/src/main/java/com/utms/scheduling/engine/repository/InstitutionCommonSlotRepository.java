package com.utms.scheduling.engine.repository;

import com.utms.scheduling.engine.entity.InstitutionCommonSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstitutionCommonSlotRepository
        extends JpaRepository<InstitutionCommonSlot, Long>, JpaSpecificationExecutor<InstitutionCommonSlot> {

    List<InstitutionCommonSlot> findByCampusIdAndIsActiveTrue(Long campusId);

    Optional<InstitutionCommonSlot> findByIdAndDeletedAtIsNull(Long id);
}
