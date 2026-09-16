package com.utms.masterdata.block;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "soft_block_overrides", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class SoftBlockOverride extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private ResourceBlock block;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "justification", nullable = false, length = 500)
    private String justification;

    @Column(name = "overridden_by", nullable = false, length = 100)
    private String overriddenBy;

    @Column(name = "overridden_at", nullable = false)
    private LocalDateTime overriddenAt;
}
