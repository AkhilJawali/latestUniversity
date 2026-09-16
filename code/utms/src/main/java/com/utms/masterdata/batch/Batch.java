package com.utms.masterdata.batch;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.program.Program;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "batches", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class Batch extends BaseEntity {

    @Column(name = "year_identifier", nullable = false, length = 20)
    private String yearIdentifier;

    @Column(name = "strength", nullable = false)
    private Integer strength;

    @Column(name = "elective_basket", length = 200)
    private String electiveBasket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_batches_programs"))
    private Program program;
}
