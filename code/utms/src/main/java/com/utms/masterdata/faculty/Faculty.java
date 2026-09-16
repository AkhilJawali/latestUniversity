package com.utms.masterdata.faculty;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.department.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "faculty", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class Faculty extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "identifier", nullable = false, length = 50, unique = true)
    private String identifier;

    @Column(name = "designation", nullable = false, length = 50)
    private String designation;

    @Column(name = "qualification", nullable = false, length = 500)
    private String qualification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_department_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_faculty_departments"))
    private Department homeDepartment;

    @Column(name = "min_weekly_load", precision = 4, scale = 1)
    private BigDecimal minWeeklyLoad;

    @Column(name = "max_weekly_load", precision = 4, scale = 1)
    private BigDecimal maxWeeklyLoad;
}
