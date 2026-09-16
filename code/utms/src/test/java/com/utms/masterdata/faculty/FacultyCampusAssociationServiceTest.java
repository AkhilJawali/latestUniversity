package com.utms.masterdata.faculty;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultyCampusAssociationServiceTest {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private FacultyCampusAssociationRepository facultyCampusAssociationRepository;

    @InjectMocks
    private FacultyCampusAssociationService facultyCampusAssociationService;

    // --- addAssociation ---

    @Test
    void addAssociation_newAssociation_createsLink() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        Campus campus = new Campus();
        campus.setId(1L);

        when(facultyRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(faculty));
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(facultyCampusAssociationRepository.existsByFacultyIdAndCampusIdAndDeletedAtIsNull(10L, 1L)).thenReturn(false);
        when(facultyCampusAssociationRepository.findByFacultyIdAndCampusId(10L, 1L)).thenReturn(Optional.empty());
        when(facultyCampusAssociationRepository.save(any(FacultyCampusAssociation.class))).thenAnswer(inv -> inv.getArgument(0));

        facultyCampusAssociationService.addAssociation(10L, 1L);

        verify(facultyCampusAssociationRepository).save(any(FacultyCampusAssociation.class));
    }

    @Test
    void addAssociation_alreadyActive_skips() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        Campus campus = new Campus();
        campus.setId(1L);

        when(facultyRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(faculty));
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(facultyCampusAssociationRepository.existsByFacultyIdAndCampusIdAndDeletedAtIsNull(10L, 1L)).thenReturn(true);

        facultyCampusAssociationService.addAssociation(10L, 1L);

        verify(facultyCampusAssociationRepository, never()).save(any());
    }

    // --- removeAssociation ---

    @Test
    void removeAssociation_multipleExist_softDeletes() {
        FacultyCampusAssociation fca = new FacultyCampusAssociation();
        fca.setId(99L);
        fca.setIsActive(true);
        fca.setDeletedAt(null);

        when(facultyCampusAssociationRepository.findByFacultyIdAndCampusId(10L, 1L)).thenReturn(Optional.of(fca));
        when(facultyCampusAssociationRepository.countByFacultyIdAndDeletedAtIsNull(10L)).thenReturn(2L);
        when(facultyCampusAssociationRepository.save(any(FacultyCampusAssociation.class))).thenAnswer(inv -> inv.getArgument(0));

        facultyCampusAssociationService.removeAssociation(10L, 1L);

        assertNotNull(fca.getDeletedAt());
        assertFalse(fca.getIsActive());
        verify(facultyCampusAssociationRepository).save(fca);
    }

    @Test
    void removeAssociation_lastAssociation_throwsBusinessRuleViolation() {
        FacultyCampusAssociation fca = new FacultyCampusAssociation();
        fca.setId(99L);
        fca.setIsActive(true);
        fca.setDeletedAt(null);

        when(facultyCampusAssociationRepository.findByFacultyIdAndCampusId(10L, 1L)).thenReturn(Optional.of(fca));
        when(facultyCampusAssociationRepository.countByFacultyIdAndDeletedAtIsNull(10L)).thenReturn(1L);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> facultyCampusAssociationService.removeAssociation(10L, 1L));

        assertTrue(exception.getMessage().contains("last campus association"));
        verify(facultyCampusAssociationRepository, never()).save(any());
    }

    @Test
    void removeAssociation_notFound_throwsNotFound() {
        when(facultyCampusAssociationRepository.findByFacultyIdAndCampusId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> facultyCampusAssociationService.removeAssociation(10L, 1L));

        verify(facultyCampusAssociationRepository, never()).save(any());
    }
}
