package com.utms.scheduling.engine.entity;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "session_derivation_rules")
@Getter
@Setter
public class SessionDerivationRule extends BaseEntity {

    @Column(name = "campus_id", nullable = false)
    private Long campusId;

    @Column(name = "component_type", nullable = false, length = 10)
    private String componentType;

    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes;

    @Column(name = "hours_per_session", nullable = false, precision = 3, scale = 1)
    private BigDecimal hoursPerSession;

    @Column(name = "description", length = 200)
    private String description;
}
