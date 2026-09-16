package com.utms.masterdata.course;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface CourseMapper {

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    CourseDto toDto(Course course);

    @Mapping(target = "department", ignore = true)
    Course toEntity(CreateCourseRequest request);

    @Mapping(target = "department", ignore = true)
    void updateEntity(UpdateCourseRequest request, @MappingTarget Course course);
}
