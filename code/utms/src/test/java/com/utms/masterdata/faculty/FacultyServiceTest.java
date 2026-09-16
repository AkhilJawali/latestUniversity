package com.utms.masterdata.faculty;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultyServiceTest {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private FacultyMapper facultyMapper;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private FacultyCompetencyService facultyCompetencyService;

    @Mock
    private FacultyCampusAssociationService facultyCampusAssociationService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private FacultyService facultyService;

    // --- create ---

    @Test
    void create_validRequest_returnsFacultyDto() {
        CreateFacultyRequest request = CreateFacultyRequest.builder()
                .name("Dr. John Smith")
                .identifier("FAC-001")
                .designation("Professor")
                .qualification("PhD Computer Science")
                .homeDepartmentId(1L)
                .campusIds(List.of(1L))
                .build();

        Department department = new Department();
        department.setId(1L);
        department.setName("Computer Science");

        Campus campus = new Campus();
        campus.setId(1L);

        Faculty faculty = new Faculty();
        faculty.setId(10L);
        faculty.setName("Dr. John Smith");
        faculty.setIdentifier("FAC-001");
        faculty.setDesignation("Professor");
        faculty.setHomeDepartment(department);

        FacultyDto expectedDto = FacultyDto.builder()
                .id(10L)
                .name("Dr. John Smith")
                .identifier("FAC-001")
                .designation("Professor")
                .homeDepartmentId(1L)
                .homeDepartmentName("Computer Science")
                .build();

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(facultyRepository.existsByIdentifier("FAC-001")).thenReturn(false);
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(facultyMapper.toEntity(request)).thenReturn(faculty);
        when(facultyRepository.save(any(Faculty.class))).thenReturn(faculty);
        when(facultyMapper.toDto(faculty)).thenReturn(expectedDto);

        FacultyDto result = facultyService.create(request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Dr. John Smith", result.getName());
        assertEquals("FAC-001", result.getIdentifier());
        verify(facultyRepository).save(any(Faculty.class));
        verify(facultyCampusAssociationService).addAssociation(10L, 1L);
    }

    @Test
    void create_duplicateIdentifier_throwsConflict() {
        CreateFacultyRequest request = CreateFacultyRequest.builder()
                .name("Dr. John Smith")
                .identifier("FAC-001")
                .designation("Professor")
                .qualification("PhD")
                .homeDepartmentId(1L)
                .campusIds(List.of(1L))
                .build();

        Department department = new Department();
        department.setId(1L);

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(facultyRepository.existsByIdentifier("FAC-001")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> facultyService.create(request));

        assertTrue(exception.getMessage().contains("FAC-001"));
        verify(facultyRepository, never()).save(any());
    }

    @Test
    void create_invalidDepartment_throwsNotFound() {
        CreateFacultyRequest request = CreateFacultyRequest.builder()
                .name("Dr. John Smith")
                .identifier("FAC-001")
                .designation("Professor")
                .qualification("PhD")
                .homeDepartmentId(99L)
                .campusIds(List.of(1L))
                .build();

        when(departmentRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> facultyService.create(request));

        assertTrue(exception.getMessage().contains("Department"));
        verify(facultyRepository, never()).save(any());
    }

    @Test
    void create_loadMinGreaterThanMax_throwsBusinessRuleViolation() {
        CreateFacultyRequest request = CreateFacultyRequest.builder()
                .name("Dr. John Smith")
                .identifier("FAC-002")
                .designation("Professor")
                .qualification("PhD")
                .homeDepartmentId(1L)
                .minWeeklyLoad(new BigDecimal("20.0"))
                .maxWeeklyLoad(new BigDecimal("10.0"))
                .campusIds(List.of(1L))
                .build();

        Department department = new Department();
        department.setId(1L);

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(facultyRepository.existsByIdentifier("FAC-002")).thenReturn(false);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> facultyService.create(request));

        assertTrue(exception.getMessage().contains("Minimum weekly load"));
        verify(facultyRepository, never()).save(any());
    }

    @Test
    void create_invalidDesignation_throwsBusinessRuleViolation() {
        CreateFacultyRequest request = CreateFacultyRequest.builder()
                .name("Dr. John Smith")
                .identifier("FAC-003")
                .designation("Invalid Designation")
                .qualification("PhD")
                .homeDepartmentId(1L)
                .campusIds(List.of(1L))
                .build();

        Department department = new Department();
        department.setId(1L);

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(facultyRepository.existsByIdentifier("FAC-003")).thenReturn(false);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> facultyService.create(request));

        assertTrue(exception.getMessage().contains("Invalid designation"));
        verify(facultyRepository, never()).save(any());
    }

    // --- update ---

    @Test
    void update_departmentTransfer_setsNewDepartment() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);
        faculty.setDesignation("Professor");
        Department oldDept = new Department();
        oldDept.setId(1L);
        oldDept.setName("CS");
        faculty.setHomeDepartment(oldDept);

        Department newDept = new Department();
        newDept.setId(2L);
        newDept.setName("IT");

        UpdateFacultyRequest request = UpdateFacultyRequest.builder()
                .name("Dr. John Smith")
                .designation("Professor")
                .qualification("PhD")
                .homeDepartmentId(2L)
                .build();

        FacultyDto expectedDto = FacultyDto.builder().id(10L).homeDepartmentId(2L).build();

        when(facultyRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(faculty));
        when(departmentRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(newDept));
        when(facultyRepository.save(any(Faculty.class))).thenReturn(faculty);
        when(facultyMapper.toDto(faculty)).thenReturn(expectedDto);

        FacultyDto result = facultyService.update(10L, request);

        assertEquals(2L, result.getHomeDepartmentId());
        verify(departmentRepository).findByIdAndDeletedAtIsNull(2L);
    }

    @Test
    void update_designationChange_emitsFacultyDesignationChangedEvent() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);
        faculty.setDesignation("Assistant Professor");
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("CS");
        faculty.setHomeDepartment(dept);

        UpdateFacultyRequest request = UpdateFacultyRequest.builder()
                .name("Dr. John Smith")
                .designation("Associate Professor")
                .qualification("PhD")
                .homeDepartmentId(1L)
                .build();

        FacultyDto expectedDto = FacultyDto.builder().id(10L).build();

        when(facultyRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(faculty));
        when(facultyRepository.save(any(Faculty.class))).thenReturn(faculty);
        when(facultyMapper.toDto(faculty)).thenReturn(expectedDto);

        facultyService.update(10L, request);

        ArgumentCaptor<FacultyDesignationChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(FacultyDesignationChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        FacultyDesignationChangedEvent event = eventCaptor.getValue();
        assertEquals(10L, event.facultyId());
        assertEquals("Assistant Professor", event.previousDesignation());
        assertEquals("Associate Professor", event.newDesignation());
    }

    // --- delete ---

    @Test
    void delete_existingFaculty_softDeletes() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);
        faculty.setIdentifier("FAC-001");
        faculty.setIsActive(true);

        when(facultyRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(faculty));

        facultyService.delete(10L);

        assertNotNull(faculty.getDeletedAt());
        assertFalse(faculty.getIsActive());
        verify(facultyRepository).save(faculty);
    }

    @Test
    void delete_nonExistentFaculty_throwsNotFound() {
        when(facultyRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> facultyService.delete(99L));

        verify(facultyRepository, never()).save(any());
    }
}
