package com.utms.masterdata.campus;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CampusDto {
    private Long id;
    private String name;
    private String code;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
