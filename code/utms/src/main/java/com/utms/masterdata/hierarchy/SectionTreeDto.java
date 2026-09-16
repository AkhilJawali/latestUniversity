package com.utms.masterdata.hierarchy;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SectionTreeDto {
    private Long id;
    private String sectionIdentifier;
    private Integer subStrength;
}
