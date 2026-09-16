package com.utms.masterdata.faculty;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.campus.Campus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "faculty_campus_associations", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class FacultyCampusAssociation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_fca_faculty"))
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_fca_campus"))
    private Campus campus;
}
