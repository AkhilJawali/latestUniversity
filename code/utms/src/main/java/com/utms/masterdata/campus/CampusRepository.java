package com.utms.masterdata.campus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Long>, JpaSpecificationExecutor<Campus> {

    boolean existsByCodeAndDeletedAtIsNull(String code);

    Optional<Campus> findByIdAndDeletedAtIsNull(Long id);

    Optional<Campus> findByCodeAndDeletedAtIsNull(String code);
}
