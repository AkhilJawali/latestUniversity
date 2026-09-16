package com.utms.masterdata.room;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class RoomDto {
    private Long id;
    private String name;
    private String code;
    private Long campusId;
    private String campusName;
    private Integer capacity;
    private RoomType roomType;
    private List<String> equipmentTags;
    private String building;
    private String floor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
