package com.utms.masterdata.timeslot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TimeSlotGridRepository extends JpaRepository<TimeSlotGrid, Long>, JpaSpecificationExecutor<TimeSlotGrid> {

    Optional<TimeSlotGrid> findByIdAndDeletedAtIsNull(Long id);

    Optional<TimeSlotGrid> findByCampusIdAndDeletedAtIsNull(Long campusId);

    boolean existsByCampusIdAndDeletedAtIsNull(Long campusId);
}
