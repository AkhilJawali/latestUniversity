package com.utms.masterdata.faculty;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacultyCampusAssociationService {

    private final FacultyRepository facultyRepository;
    private final CampusRepository campusRepository;
    private final FacultyCampusAssociationRepository facultyCampusAssociationRepository;

    @Transactional
    public void addAssociation(Long facultyId, Long campusId) {
        Faculty faculty = facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new EntityNotFoundException("Campus", campusId));

        // Check if already active
        if (facultyCampusAssociationRepository.existsByFacultyIdAndCampusIdAndDeletedAtIsNull(facultyId, campusId)) {
            log.debug("Campus association already exists: facultyId={}, campusId={} — skipping", facultyId, campusId);
            return;
        }

        // Check if soft-deleted record exists (unique constraint is plain)
        Optional<FacultyCampusAssociation> existing = facultyCampusAssociationRepository
                .findByFacultyIdAndCampusId(facultyId, campusId);

        if (existing.isPresent()) {
            // Reactivate soft-deleted record
            FacultyCampusAssociation fca = existing.get();
            fca.setDeletedAt(null);
            fca.setIsActive(true);
            facultyCampusAssociationRepository.save(fca);
            log.info("Campus association reactivated: facultyId={}, campusId={}", facultyId, campusId);
        } else {
            FacultyCampusAssociation fca = new FacultyCampusAssociation();
            fca.setFaculty(faculty);
            fca.setCampus(campus);
            fca.setIsActive(true);
            facultyCampusAssociationRepository.save(fca);
            log.info("Campus association added: facultyId={}, campusId={}", facultyId, campusId);
        }
    }

    /**
     * Remove a campus association (soft-delete).
     * Blocks removal if this is the last active association (HC-FAC-7).
     * TODO: Warn if faculty has active sessions at this campus.
     */
    @Transactional
    public void removeAssociation(Long facultyId, Long campusId) {
        FacultyCampusAssociation fca = facultyCampusAssociationRepository
                .findByFacultyIdAndCampusId(facultyId, campusId)
                .filter(a -> a.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("FacultyCampusAssociation",
                        "facultyId=" + facultyId + ",campusId=" + campusId, "not found"));

        // HC-FAC-7: Block if last active association
        long activeCount = facultyCampusAssociationRepository.countByFacultyIdAndDeletedAtIsNull(facultyId);
        if (activeCount <= 1) {
            throw new BusinessRuleViolationException(
                    "Cannot remove the last campus association for faculty " + facultyId
                            + ". A faculty member must have at least one campus association.");
        }

        fca.setDeletedAt(LocalDateTime.now());
        fca.setIsActive(false);
        facultyCampusAssociationRepository.save(fca);

        log.info("Campus association removed: facultyId={}, campusId={}", facultyId, campusId);
    }

    @Transactional(readOnly = true)
    public List<FacultyCampusAssociation> getAssociations(Long facultyId) {
        return facultyCampusAssociationRepository.findByFacultyIdAndDeletedAtIsNull(facultyId);
    }

    /**
     * Read back a faculty's active campus associations as DTOs (id + name + code).
     * Added for OQ-1 (A4-420): lets the faculty-management UI display the current set.
     * Validates the faculty exists (404 if not) before returning.
     */
    @Transactional(readOnly = true)
    public List<FacultyCampusDto> getAssociationDtos(Long facultyId) {
        if (!facultyRepository.findByIdAndDeletedAtIsNull(facultyId).isPresent()) {
            throw new EntityNotFoundException("Faculty", facultyId);
        }
        return facultyCampusAssociationRepository.findByFacultyIdAndDeletedAtIsNull(facultyId)
                .stream()
                .map(fca -> {
                    Campus campus = fca.getCampus();
                    return FacultyCampusDto.builder()
                            .campusId(campus.getId())
                            .name(campus.getName())
                            .code(campus.getCode())
                            .build();
                })
                .toList();
    }
}
