package com.utms.masterdata.section;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SectionDto {
    private Long id;
    private String sectionIdentifier;
    private Integer subStrength;
    private Long batchId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
