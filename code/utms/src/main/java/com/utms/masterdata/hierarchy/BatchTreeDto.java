package com.utms.masterdata.hierarchy;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class BatchTreeDto {
    private Long id;
    private String yearIdentifier;
    private Integer strength;
    private List<SectionTreeDto> sections;
}
