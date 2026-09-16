package com.utms.scheduling.config;

import com.utms.common.mapper.BaseMapperConfig;
import com.utms.scheduling.engine.entity.SoftConstraintWeight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface SoftConstraintWeightMapper {

    SoftConstraintWeightDto toDto(SoftConstraintWeight entity);

    SoftConstraintWeight toEntity(CreateSoftConstraintWeightRequest request);

    // campusId is immutable on update (PD-84).
    @Mapping(target = "campusId", ignore = true)
    void updateEntity(UpdateSoftConstraintWeightRequest request, @MappingTarget SoftConstraintWeight entity);
}
