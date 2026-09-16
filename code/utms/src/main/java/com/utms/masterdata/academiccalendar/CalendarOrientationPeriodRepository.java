package com.utms.masterdata.academiccalendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarOrientationPeriodRepository extends JpaRepository<CalendarOrientationPeriod, Long> {

    Optional<CalendarOrientationPeriod> findByIdAndDeletedAtIsNull(Long id);

    List<CalendarOrientationPeriod> findByCalendarIdAndDeletedAtIsNull(Long calendarId);

    @Query("SELECT o FROM CalendarOrientationPeriod o WHERE o.calendarId = :calendarId " +
           "AND o.deletedAt IS NULL AND o.startDate <= :date AND o.endDate >= :date")
    List<CalendarOrientationPeriod> findByCalendarIdAndDateWithin(
            @Param("calendarId") Long calendarId,
            @Param("date") LocalDate date);
}
