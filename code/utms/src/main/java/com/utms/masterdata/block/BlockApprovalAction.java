package com.utms.masterdata.block;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "block_approval_actions", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class BlockApprovalAction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private ResourceBlock block;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId;

    @Column(name = "comments", length = 1000)
    private String comments;
}
