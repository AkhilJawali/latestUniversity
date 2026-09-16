package com.utms.masterdata.block;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "resource_blocks", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class ResourceBlock extends BaseEntity {

    @Column(name = "resource_type", nullable = false, length = 10)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "block_type", nullable = false, length = 10)
    private String blockType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "reason_code", nullable = false, length = 50)
    private String reasonCode;

    @Column(name = "reason_text", length = 500)
    private String reasonText;

    @Column(name = "recurrence_pattern", length = 100)
    private String recurrencePattern;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "raised_by", nullable = false, length = 100)
    private String raisedBy;

    @Column(name = "raised_at", nullable = false)
    private LocalDateTime raisedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
