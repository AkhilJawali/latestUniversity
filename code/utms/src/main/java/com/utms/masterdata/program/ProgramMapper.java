package com.utms.masterdata.program;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface ProgramMapper {

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    ProgramDto toDto(Program program);

    @Mapping(target = "department", ignore = true)
    Program toEntity(CreateProgramRequest request);

    void updateEntity(UpdateProgramRequest request, @MappingTarget Program program);
}
