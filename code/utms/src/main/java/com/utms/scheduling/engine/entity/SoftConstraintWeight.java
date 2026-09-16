package com.utms.scheduling.engine.entity;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "soft_constraint_weights")
@Getter
@Setter
public class SoftConstraintWeight extends BaseEntity {

    @Column(name = "campus_id", nullable = false)
    private Long campusId;

    @Column(name = "constraint_type", nullable = false, length = 40)
    private String constraintType;

    @Column(name = "weight", nullable = false, precision = 4, scale = 2)
    private BigDecimal weight = new BigDecimal("1.00");
}
