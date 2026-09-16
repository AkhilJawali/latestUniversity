package com.utms.masterdata.faculty.availability;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.faculty.Faculty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "faculty_availability_windows", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class FacultyAvailabilityWindow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_faculty_availability_windows_faculty"))
    private Faculty faculty;

    @Column(name = "day_of_week", nullable = false, length = 10)
    private String dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "reason_code", nullable = false, length = 50)
    private String reasonCode;

    @Column(name = "reason_note", length = 500)
    private String reasonNote;
}
