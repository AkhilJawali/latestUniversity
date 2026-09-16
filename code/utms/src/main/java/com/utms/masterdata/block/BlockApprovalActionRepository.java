package com.utms.masterdata.block;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockApprovalActionRepository extends JpaRepository<BlockApprovalAction, Long> {
}
