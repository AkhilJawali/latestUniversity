package com.utms.masterdata.department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {

    boolean existsByCodeAndCampusIdAndDeletedAtIsNull(String code, Long campusId);

    Optional<Department> findByIdAndDeletedAtIsNull(Long id);

    long countByCampusIdAndDeletedAtIsNull(Long campusId);
}
