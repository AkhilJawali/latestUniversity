package com.utms.scheduling.config;

import com.utms.common.mapper.BaseMapperConfig;
import com.utms.scheduling.engine.entity.SessionDerivationRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface DerivationRuleMapper {

    DerivationRuleDto toDto(SessionDerivationRule entity);

    SessionDerivationRule toEntity(CreateDerivationRuleRequest request);

    // campusId is immutable on update (PD-84) — never remap it from the request.
    @Mapping(target = "campusId", ignore = true)
    void updateEntity(UpdateDerivationRuleRequest request, @MappingTarget SessionDerivationRule entity);
}
