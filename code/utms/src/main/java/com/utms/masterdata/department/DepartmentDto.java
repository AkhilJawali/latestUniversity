package com.utms.masterdata.department;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class DepartmentDto {
    private Long id;
    private String name;
    private String code;
    private Long campusId;
    private String campusName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
