package com.utms.masterdata.faculty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyCampusAssociationRepository extends JpaRepository<FacultyCampusAssociation, Long> {

    List<FacultyCampusAssociation> findByFacultyIdAndDeletedAtIsNull(Long facultyId);

    Optional<FacultyCampusAssociation> findByFacultyIdAndCampusId(Long facultyId, Long campusId);

    boolean existsByFacultyIdAndCampusIdAndDeletedAtIsNull(Long facultyId, Long campusId);

    long countByFacultyIdAndDeletedAtIsNull(Long facultyId);

    long countByCampusIdAndDeletedAtIsNull(Long campusId);
}
