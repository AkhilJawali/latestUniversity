package com.utms.scheduling.engine.entity;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "soft_constraint_violations")
@Getter
@Setter
public class SoftConstraintViolation extends BaseEntity {

    @Column(name = "draft_id", nullable = false)
    private Long draftId;

    @Column(name = "constraint_type", nullable = false, length = 40)
    private String constraintType;

    @Column(name = "affected_entity_type", nullable = false, length = 20)
    private String affectedEntityType;

    @Column(name = "affected_entity_id", nullable = false)
    private Long affectedEntityId;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "relaxation_reason", nullable = false, columnDefinition = "TEXT")
    private String relaxationReason;
}
