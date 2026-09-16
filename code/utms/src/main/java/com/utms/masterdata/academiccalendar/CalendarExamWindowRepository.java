package com.utms.masterdata.academiccalendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarExamWindowRepository extends JpaRepository<CalendarExamWindow, Long> {

    Optional<CalendarExamWindow> findByIdAndDeletedAtIsNull(Long id);

    List<CalendarExamWindow> findByCalendarIdAndDeletedAtIsNull(Long calendarId);

    @Query("SELECT e FROM CalendarExamWindow e WHERE e.calendarId = :calendarId " +
           "AND e.deletedAt IS NULL AND e.startDate <= :date AND e.endDate >= :date")
    List<CalendarExamWindow> findByCalendarIdAndDateWithin(
            @Param("calendarId") Long calendarId,
            @Param("date") LocalDate date);
}
