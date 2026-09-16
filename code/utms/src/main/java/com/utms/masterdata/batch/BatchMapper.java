package com.utms.masterdata.batch;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface BatchMapper {

    @Mapping(source = "program.id", target = "programId")
    @Mapping(source = "program.name", target = "programName")
    BatchDto toDto(Batch batch);

    @Mapping(target = "program", ignore = true)
    Batch toEntity(CreateBatchRequest request);

    void updateEntity(UpdateBatchRequest request, @MappingTarget Batch batch);
}
