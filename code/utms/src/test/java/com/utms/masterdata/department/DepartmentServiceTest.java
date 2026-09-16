package com.utms.masterdata.department;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.masterdata.program.ProgramRepository;
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
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private DepartmentService departmentService;

    // --- create ---

    @Test
    void create_validRequest_returnsDepartmentDto() {
        CreateDepartmentRequest request = CreateDepartmentRequest.builder()
                .name("Computer Science")
                .code("CS")
                .campusId(1L)
                .build();

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");

        Department department = new Department();
        department.setId(1L);
        department.setName("Computer Science");
        department.setCode("CS");
        department.setCampus(campus);
        department.setIsActive(true);

        DepartmentDto expectedDto = DepartmentDto.builder()
                .id(1L)
                .name("Computer Science")
                .code("CS")
                .campusId(1L)
                .campusName("Main Campus")
                .build();

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(departmentRepository.existsByCodeAndCampusIdAndDeletedAtIsNull("CS", 1L)).thenReturn(false);
        when(departmentMapper.toEntity(request)).thenReturn(department);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        when(departmentMapper.toDto(department)).thenReturn(expectedDto);

        DepartmentDto result = departmentService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Computer Science", result.getName());
        assertEquals("CS", result.getCode());
        assertEquals(1L, result.getCampusId());
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void create_invalidCampusId_throwsEntityNotFoundException() {
        CreateDepartmentRequest request = CreateDepartmentRequest.builder()
                .name("Computer Science")
                .code("CS")
                .campusId(99L)
                .build();

        when(campusRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> departmentService.create(request));

        assertTrue(exception.getMessage().contains("Campus"));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void create_duplicateCodeSameCampus_throwsConflict() {
        CreateDepartmentRequest request = CreateDepartmentRequest.builder()
                .name("Computer Science")
                .code("CS")
                .campusId(1L)
                .build();

        Campus campus = new Campus();
        campus.setId(1L);

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(departmentRepository.existsByCodeAndCampusIdAndDeletedAtIsNull("CS", 1L)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> departmentService.create(request));

        assertTrue(exception.getMessage().contains("CS"));
        verify(departmentRepository, never()).save(any());
    }

    // --- delete ---

    @Test
    void delete_hasPrograms_throwsBusinessRuleViolation() {
        Department department = new Department();
        department.setId(1L);
        department.setCode("CS");
        department.setIsActive(true);

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(programRepository.countByDepartmentIdAndDeletedAtIsNull(1L)).thenReturn(5L);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> departmentService.delete(1L));

        assertTrue(exception.getMessage().contains("Cannot delete department"));
        assertFalse(exception.getDetails().isEmpty());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void delete_noReferences_softDeletes() {
        Department department = new Department();
        department.setId(1L);
        department.setCode("CS");
        department.setIsActive(true);

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(programRepository.countByDepartmentIdAndDeletedAtIsNull(1L)).thenReturn(0L);

        departmentService.delete(1L);

        assertNotNull(department.getDeletedAt());
        assertFalse(department.getIsActive());
        verify(departmentRepository).save(department);
    }
}
