package com.utms.masterdata.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchedulableAssetRepository extends JpaRepository<SchedulableAsset, Long>,
        JpaSpecificationExecutor<SchedulableAsset> {

    boolean existsByIdentifierAndDeletedAtIsNull(String identifier);

    Optional<SchedulableAsset> findByIdAndDeletedAtIsNull(Long id);
}
