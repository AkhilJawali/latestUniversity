package com.utms.scheduling.config;

import com.utms.common.mapper.BaseMapperConfig;
import com.utms.scheduling.engine.entity.InstitutionCommonSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface CommonSlotMapper {

    CommonSlotDto toDto(InstitutionCommonSlot entity);

    InstitutionCommonSlot toEntity(CreateCommonSlotRequest request);

    // campusId is immutable on update (PD-84).
    @Mapping(target = "campusId", ignore = true)
    void updateEntity(UpdateCommonSlotRequest request, @MappingTarget InstitutionCommonSlot entity);
}
