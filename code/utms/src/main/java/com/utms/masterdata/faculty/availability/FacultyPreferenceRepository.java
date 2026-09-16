package com.utms.masterdata.faculty.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyPreferenceRepository extends JpaRepository<FacultyPreference, Long> {

    Optional<FacultyPreference> findByFacultyId(Long facultyId);
}
