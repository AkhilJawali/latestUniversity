package com.utms.masterdata.timeslot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SlotDefinitionRepository extends JpaRepository<SlotDefinition, Long> {

    Optional<SlotDefinition> findByIdAndDeletedAtIsNull(Long id);

    List<SlotDefinition> findByGridIdAndDeletedAtIsNull(Long gridId);

    List<SlotDefinition> findByGridIdAndApplicableDayAndDeletedAtIsNull(Long gridId, DayOfWeekEnum applicableDay);

    List<SlotDefinition> findByGridIdAndApplicableDayIsNullAndDeletedAtIsNull(Long gridId);

    long countByGridIdAndDeletedAtIsNullAndSlotType(Long gridId, SlotType slotType);
}
