package com.utms.masterdata.academiccalendar;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.campus.Campus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "working_day_patterns", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class WorkingDayPattern extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_working_day_patterns_campuses"))
    private Campus campus;

    @Column(name = "campus_id", insertable = false, updatable = false)
    private Long campusId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pattern_type", nullable = false, length = 30)
    private PatternType patternType;

    /**
     * For ALTERNATE_SATURDAY: comma-separated list of which Saturdays of the month are working.
     * E.g., "1,3" means 1st and 3rd Saturday are working days.
     */
    @Column(name = "working_saturdays", length = 100)
    private String workingSaturdays;

    /**
     * For CUSTOM pattern: flexible text/JSON definition for exotic patterns.
     */
    @Column(name = "custom_definition", columnDefinition = "TEXT")
    private String customDefinition;
}
