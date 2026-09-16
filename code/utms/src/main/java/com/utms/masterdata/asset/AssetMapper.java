package com.utms.masterdata.asset;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapperConfig.class)
public interface AssetMapper {

    @Mapping(target = "owningDepartmentId", source = "owningDepartment.id")
    @Mapping(target = "owningDepartmentName", source = "owningDepartment.name")
    @Mapping(target = "campusId", source = "campus.id")
    @Mapping(target = "campusName", source = "campus.name")
    AssetDto toDto(SchedulableAsset asset);

    AssetAvailabilityWindowDto toWindowDto(AssetAvailabilityWindow window);

    @Mapping(target = "owningDepartment", ignore = true)
    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "availabilityWindows", ignore = true)
    SchedulableAsset toEntity(CreateAssetRequest request);

    @Mapping(target = "owningDepartment", ignore = true)
    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "availabilityWindows", ignore = true)
    void updateEntity(UpdateAssetRequest request, @MappingTarget SchedulableAsset asset);
}
