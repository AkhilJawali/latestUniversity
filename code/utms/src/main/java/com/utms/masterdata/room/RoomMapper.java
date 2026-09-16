package com.utms.masterdata.room;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface RoomMapper {

    @Mapping(target = "campusId", source = "campus.id")
    @Mapping(target = "campusName", source = "campus.name")
    RoomDto toDto(Room room);

    @Mapping(target = "campus", ignore = true)
    Room toEntity(CreateRoomRequest request);

    @Mapping(target = "campus", ignore = true)
    void updateEntity(UpdateRoomRequest request, @MappingTarget Room room);
}
