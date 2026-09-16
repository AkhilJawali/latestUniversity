package com.utms.scheduling.engine.entity;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "institution_common_slots")
@Getter
@Setter
public class InstitutionCommonSlot extends BaseEntity {

    @Column(name = "campus_id", nullable = false)
    private Long campusId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "day_of_week", nullable = false, length = 10)
    private String dayOfWeek;

    @Column(name = "slot_definition_id", nullable = false)
    private Long slotDefinitionId;

    @Column(name = "applies_to_all_batches", nullable = false)
    private Boolean appliesToAllBatches = true;
}
