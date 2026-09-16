package com.utms.masterdata.asset;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "asset_availability_windows", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class AssetAvailabilityWindow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_asset_availability_windows_assets"))
    private SchedulableAsset asset;

    @Column(name = "day_of_week", nullable = false, length = 10)
    private String dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
