package com.utms.masterdata.section;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.batch.Batch;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sections", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class Section extends BaseEntity {

    @Column(name = "section_identifier", nullable = false, length = 10)
    private String sectionIdentifier;

    @Column(name = "sub_strength")
    private Integer subStrength;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sections_batches"))
    private Batch batch;
}
