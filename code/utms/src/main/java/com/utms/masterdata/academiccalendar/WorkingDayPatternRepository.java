package com.utms.masterdata.academiccalendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkingDayPatternRepository extends JpaRepository<WorkingDayPattern, Long> {

    Optional<WorkingDayPattern> findByIdAndDeletedAtIsNull(Long id);

    Optional<WorkingDayPattern> findByCampusIdAndDeletedAtIsNull(Long campusId);

    boolean existsByCampusIdAndDeletedAtIsNull(Long campusId);
}
