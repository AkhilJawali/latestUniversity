package com.utms.masterdata.course;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_prerequisites", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class CoursePrerequisite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cp_course"))
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_course_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cp_prerequisite"))
    private Course prerequisiteCourse;
}
