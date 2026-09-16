package com.utms.masterdata.hierarchy;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class DepartmentTreeDto {
    private Long id;
    private String name;
    private String code;
    private List<ProgramTreeDto> programs;
}
