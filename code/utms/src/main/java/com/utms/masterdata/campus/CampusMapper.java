package com.utms.masterdata.campus;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface CampusMapper {

    CampusDto toDto(Campus campus);

    Campus toEntity(CreateCampusRequest request);

    void updateEntity(UpdateCampusRequest request, @MappingTarget Campus campus);
}
