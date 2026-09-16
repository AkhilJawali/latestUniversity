package com.utms.masterdata.academiccalendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarHolidayRepository extends JpaRepository<CalendarHoliday, Long> {

    Optional<CalendarHoliday> findByIdAndDeletedAtIsNull(Long id);

    List<CalendarHoliday> findByCalendarIdAndDeletedAtIsNull(Long calendarId);

    @Query("SELECT h FROM CalendarHoliday h WHERE h.calendarId = :calendarId " +
           "AND h.deletedAt IS NULL AND h.startDate <= :endDate AND h.endDate >= :startDate")
    List<CalendarHoliday> findByCalendarIdAndDateRange(
            @Param("calendarId") Long calendarId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT h FROM CalendarHoliday h WHERE h.calendarId = :calendarId " +
           "AND h.deletedAt IS NULL AND h.startDate <= :date AND h.endDate >= :date")
    List<CalendarHoliday> findByCalendarIdAndDateWithin(
            @Param("calendarId") Long calendarId,
            @Param("date") LocalDate date);
}
