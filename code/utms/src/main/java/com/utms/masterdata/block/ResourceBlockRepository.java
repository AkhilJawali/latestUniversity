package com.utms.masterdata.block;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceBlockRepository extends JpaRepository<ResourceBlock, Long>,
        JpaSpecificationExecutor<ResourceBlock> {

    Optional<ResourceBlock> findByIdAndDeletedAtIsNull(Long id);

    List<ResourceBlock> findByResourceTypeAndResourceIdAndStatusAndDeletedAtIsNull(
            String resourceType, Long resourceId, String status);

    List<ResourceBlock> findByStatusAndExpiresAtBeforeAndDeletedAtIsNull(
            String status, LocalDateTime expiryThreshold);
}
