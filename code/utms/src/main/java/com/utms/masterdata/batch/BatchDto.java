package com.utms.masterdata.batch;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BatchDto {
    private Long id;
    private String yearIdentifier;
    private Integer strength;
    private String electiveBasket;
    private Long programId;
    private String programName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
