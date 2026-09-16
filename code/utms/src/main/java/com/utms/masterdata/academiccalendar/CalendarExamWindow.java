package com.utms.masterdata.academiccalendar;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "calendar_exam_windows", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class CalendarExamWindow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_calendar_exam_windows_calendars"))
    private AcademicCalendar calendar;

    @Column(name = "calendar_id", insertable = false, updatable = false)
    private Long calendarId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false, length = 30)
    private ExamType examType;

    @Column(name = "description", length = 200)
    private String description;
}
