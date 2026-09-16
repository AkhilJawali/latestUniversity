package com.utms.masterdata.block;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class ResourceBlockDto {
    private Long id;
    private String resourceType;
    private Long resourceId;
    private String blockType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reasonCode;
    private String reasonText;
    private String recurrencePattern;
    private String status;
    private String raisedBy;
    private LocalDateTime raisedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime releasedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
