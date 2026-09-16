package com.utms.masterdata.faculty;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.course.Course;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "faculty_competencies", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class FacultyCompetency extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_fc_faculty"))
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_fc_course"))
    private Course course;
}
