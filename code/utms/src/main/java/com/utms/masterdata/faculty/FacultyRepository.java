package com.utms.masterdata.faculty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long>, JpaSpecificationExecutor<Faculty> {

    boolean existsByIdentifierAndDeletedAtIsNull(String identifier);

    boolean existsByIdentifier(String identifier);

    Optional<Faculty> findByIdAndDeletedAtIsNull(Long id);

    Optional<Faculty> findByIdentifierAndDeletedAtIsNull(String identifier);

    long countByHomeDepartmentIdAndDeletedAtIsNull(Long departmentId);
}
