package com.utms.approval.entity;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A configurable approval pipeline (A4-19 FR-6). One active global pipeline for this story
 * (PD-102); {@code scope} is reserved for per-department/campus scoping later.
 */
@Entity
@Table(name = "approval_pipelines")
@Getter
@Setter
public class ApprovalPipeline extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "scope", length = 100)
    private String scope;
}
