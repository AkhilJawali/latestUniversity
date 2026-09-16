package com.utms.masterdata.section;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface SectionMapper {

    @Mapping(source = "batch.id", target = "batchId")
    SectionDto toDto(Section section);

    @Mapping(target = "batch", ignore = true)
    Section toEntity(CreateSectionRequest request);

    void updateEntity(UpdateSectionRequest request, @MappingTarget Section section);
}
