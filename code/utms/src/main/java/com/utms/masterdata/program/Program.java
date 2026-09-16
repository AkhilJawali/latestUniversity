package com.utms.masterdata.program;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.department.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "programs", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class Program extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "duration_semesters", nullable = false)
    private Integer durationSemesters;

    @Column(name = "degree_type", nullable = false, length = 50)
    private String degreeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_programs_departments"))
    private Department department;
}
