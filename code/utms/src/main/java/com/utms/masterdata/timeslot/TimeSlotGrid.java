package com.utms.masterdata.timeslot;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.campus.Campus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "time_slot_grids", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class TimeSlotGrid extends BaseEntity {

    @Column(name = "grid_name", nullable = false, length = 200)
    private String gridName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_time_slot_grids_campuses"))
    private Campus campus;

    @Column(name = "campus_id", insertable = false, updatable = false)
    private Long campusId;

    @OneToMany(mappedBy = "grid", fetch = FetchType.LAZY)
    private List<SlotDefinition> slotDefinitions = new ArrayList<>();
}
