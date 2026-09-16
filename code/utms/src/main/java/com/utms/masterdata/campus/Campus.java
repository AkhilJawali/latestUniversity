package com.utms.masterdata.campus;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "campuses", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class Campus extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "location", nullable = false, length = 500)
    private String location;
}
