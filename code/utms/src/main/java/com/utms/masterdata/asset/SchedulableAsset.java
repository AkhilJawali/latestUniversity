package com.utms.masterdata.asset;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.department.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "schedulable_assets", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class SchedulableAsset extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "identifier", nullable = false, length = 50)
    private String identifier;

    @Column(name = "asset_type", nullable = false, length = 50)
    private String assetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owning_department_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_schedulable_assets_departments"))
    private Department owningDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_schedulable_assets_campuses"))
    private Campus campus;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AssetAvailabilityWindow> availabilityWindows = new ArrayList<>();
}
