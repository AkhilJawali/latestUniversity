package com.utms.masterdata.block;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoftBlockOverrideRepository extends JpaRepository<SoftBlockOverride, Long> {

    List<SoftBlockOverride> findByBlockIdAndDeletedAtIsNull(Long blockId);
}
