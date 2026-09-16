package com.utms.masterdata.room;

import com.utms.common.converter.StringListConverter;
import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.campus.Campus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class Room extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "room_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private RoomType roomType;

    @Column(name = "equipment_tags", length = 2000)
    @Convert(converter = StringListConverter.class)
    private List<String> equipmentTags = new ArrayList<>();

    @Column(name = "building", length = 100)
    private String building;

    @Column(name = "floor", length = 20)
    private String floor;
}
