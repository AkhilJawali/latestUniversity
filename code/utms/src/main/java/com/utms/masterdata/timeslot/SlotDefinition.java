package com.utms.masterdata.timeslot;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "slot_definitions", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class SlotDefinition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grid_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_slot_definitions_grids"))
    private TimeSlotGrid grid;

    @Column(name = "grid_id", insertable = false, updatable = false)
    private Long gridId;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type", nullable = false, length = 20)
    private SlotType slotType;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_day", length = 10)
    private DayOfWeekEnum applicableDay;

    @Column(name = "label", length = 100)
    private String label;

    /**
     * Computes the duration of this slot in minutes.
     */
    public int getDurationMinutes() {
        return (int) java.time.Duration.between(startTime, endTime).toMinutes();
    }
}
