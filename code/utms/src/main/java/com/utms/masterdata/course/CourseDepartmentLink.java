package com.utms.masterdata.course;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.department.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_department_links", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class CourseDepartmentLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cdl_course"))
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cdl_department"))
    private Department department;
}
