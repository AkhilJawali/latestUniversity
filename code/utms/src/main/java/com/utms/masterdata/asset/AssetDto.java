package com.utms.masterdata.asset;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class AssetDto {
    private Long id;
    private String name;
    private String identifier;
    private String assetType;
    private Long owningDepartmentId;
    private String owningDepartmentName;
    private Long campusId;
    private String campusName;
    private List<AssetAvailabilityWindowDto> availabilityWindows;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
