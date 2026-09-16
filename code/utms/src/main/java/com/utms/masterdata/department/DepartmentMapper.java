package com.utms.masterdata.department;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface DepartmentMapper {

    @Mapping(source = "campus.id", target = "campusId")
    @Mapping(source = "campus.name", target = "campusName")
    DepartmentDto toDto(Department department);

    @Mapping(target = "campus", ignore = true)
    Department toEntity(CreateDepartmentRequest request);

    void updateEntity(UpdateDepartmentRequest request, @MappingTarget Department department);
}
