package com.utms.masterdata.academiccalendar;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.campus.Campus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "academic_calendars", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class AcademicCalendar extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_academic_calendars_campuses"))
    private Campus campus;

    @Column(name = "campus_id", insertable = false, updatable = false)
    private Long campusId;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "semester_identifier", nullable = false, length = 30)
    private String semesterIdentifier;

    @Column(name = "semester_start_date", nullable = false)
    private LocalDate semesterStartDate;

    @Column(name = "semester_end_date", nullable = false)
    private LocalDate semesterEndDate;

    @OneToMany(mappedBy = "calendar", fetch = FetchType.LAZY)
    private List<CalendarHoliday> holidays = new ArrayList<>();

    @OneToMany(mappedBy = "calendar", fetch = FetchType.LAZY)
    private List<CalendarExamWindow> examWindows = new ArrayList<>();

    @OneToMany(mappedBy = "calendar", fetch = FetchType.LAZY)
    private List<CalendarOrientationPeriod> orientationPeriods = new ArrayList<>();
}
