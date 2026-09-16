package com.utms.masterdata.department;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.campus.Campus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "departments", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class Department extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_departments_campuses"))
    private Campus campus;
}
