package com.utms.masterdata.academiccalendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicCalendarRepository extends JpaRepository<AcademicCalendar, Long>, JpaSpecificationExecutor<AcademicCalendar> {

    Optional<AcademicCalendar> findByIdAndDeletedAtIsNull(Long id);

    Optional<AcademicCalendar> findByCampusIdAndAcademicYearAndSemesterIdentifierAndDeletedAtIsNull(
            Long campusId, String academicYear, String semesterIdentifier);

    boolean existsByCampusIdAndAcademicYearAndSemesterIdentifierAndDeletedAtIsNull(
            Long campusId, String academicYear, String semesterIdentifier);

    List<AcademicCalendar> findByCampusIdAndDeletedAtIsNull(Long campusId);

    List<AcademicCalendar> findByAcademicYearAndDeletedAtIsNull(String academicYear);
}
