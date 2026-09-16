package com.utms.masterdata.section;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long>, JpaSpecificationExecutor<Section> {

    boolean existsBySectionIdentifierAndBatchIdAndDeletedAtIsNull(String sectionIdentifier, Long batchId);

    Optional<Section> findByIdAndDeletedAtIsNull(Long id);

    List<Section> findAllByBatchIdAndDeletedAtIsNull(Long batchId);

    long countByBatchIdAndDeletedAtIsNull(Long batchId);

    @Query("SELECT MAX(s.subStrength) FROM Section s WHERE s.batch.id = :batchId AND s.deletedAt IS NULL")
    Integer findMaxSubStrengthByBatchId(@Param("batchId") Long batchId);
}
