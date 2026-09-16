package com.utms.masterdata.faculty.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyAvailabilityWindowRepository extends JpaRepository<FacultyAvailabilityWindow, Long> {

    List<FacultyAvailabilityWindow> findByFacultyIdAndDeletedAtIsNull(Long facultyId);

    List<FacultyAvailabilityWindow> findByFacultyIdAndDayOfWeekAndDeletedAtIsNull(Long facultyId, String dayOfWeek);

    Optional<FacultyAvailabilityWindow> findByIdAndDeletedAtIsNull(Long id);
}
