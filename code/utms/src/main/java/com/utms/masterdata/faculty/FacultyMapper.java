package com.utms.masterdata.faculty;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface FacultyMapper {

    @Mapping(target = "homeDepartmentId", source = "homeDepartment.id")
    @Mapping(target = "homeDepartmentName", source = "homeDepartment.name")
    FacultyDto toDto(Faculty faculty);

    @Mapping(target = "homeDepartment", ignore = true)
    Faculty toEntity(CreateFacultyRequest request);

    @Mapping(target = "homeDepartment", ignore = true)
    void updateEntity(UpdateFacultyRequest request, @MappingTarget Faculty faculty);
}
