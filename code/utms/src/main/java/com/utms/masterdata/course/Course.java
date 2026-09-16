package com.utms.masterdata.course;

import com.utms.common.converter.StringListConverter;
import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.department.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class Course extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_courses_departments"))
    private Department department;

    @Column(name = "lecture_hours", nullable = false)
    private Integer lectureHours;

    @Column(name = "tutorial_hours", nullable = false)
    private Integer tutorialHours;

    @Column(name = "practical_hours", nullable = false)
    private Integer practicalHours;

    @Column(name = "credits", nullable = false, precision = 3, scale = 1)
    private BigDecimal credits;

    @Column(name = "course_type", nullable = false, length = 20)
    private String courseType;

    @Convert(converter = StringListConverter.class)
    @Column(name = "equipment_tags", length = 2000)
    private List<String> equipmentTags = new ArrayList<>();

    @Column(name = "is_cross_listed", nullable = false)
    private Boolean isCrossListed = false;
}
