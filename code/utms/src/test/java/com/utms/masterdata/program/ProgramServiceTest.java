package com.utms.masterdata.program;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.batch.BatchRepository;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
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
class ProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private ProgramMapper programMapper;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private ProgramService programService;

    // --- create ---

    @Test
    void create_validRequest_returnsProgramDto() {
        CreateProgramRequest request = CreateProgramRequest.builder()
                .name("B.Tech Computer Science")
                .code("BTCS")
                .departmentId(1L)
                .durationSemesters(8)
                .degreeType("B.Tech")
                .build();

        Department department = new Department();
        department.setId(1L);
        department.setName("Computer Science");

        Program program = new Program();
        program.setId(1L);
        program.setName("B.Tech Computer Science");
        program.setCode("BTCS");
        program.setDepartment(department);
        program.setDurationSemesters(8);
        program.setDegreeType("B.Tech");
        program.setIsActive(true);

        ProgramDto expectedDto = ProgramDto.builder()
                .id(1L)
                .name("B.Tech Computer Science")
                .code("BTCS")
                .departmentId(1L)
                .departmentName("Computer Science")
                .durationSemesters(8)
                .degreeType("B.Tech")
                .build();

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(programRepository.existsByCodeAndDepartmentIdAndDeletedAtIsNull("BTCS", 1L)).thenReturn(false);
        when(programMapper.toEntity(request)).thenReturn(program);
        when(programRepository.save(any(Program.class))).thenReturn(program);
        when(programMapper.toDto(program)).thenReturn(expectedDto);

        ProgramDto result = programService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("B.Tech Computer Science", result.getName());
        assertEquals("BTCS", result.getCode());
        assertEquals(1L, result.getDepartmentId());
        assertEquals(8, result.getDurationSemesters());
        verify(programRepository).save(any(Program.class));
    }

    @Test
    void create_invalidDepartmentId_throwsEntityNotFoundException() {
        CreateProgramRequest request = CreateProgramRequest.builder()
                .name("B.Tech Computer Science")
                .code("BTCS")
                .departmentId(99L)
                .durationSemesters(8)
                .degreeType("B.Tech")
                .build();

        when(departmentRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> programService.create(request));

        assertTrue(exception.getMessage().contains("Department"));
        verify(programRepository, never()).save(any());
    }

    @Test
    void create_duplicateCodeSameDepartment_throwsConflict() {
        CreateProgramRequest request = CreateProgramRequest.builder()
                .name("B.Tech Computer Science")
                .code("BTCS")
                .departmentId(1L)
                .durationSemesters(8)
                .degreeType("B.Tech")
                .build();

        Department department = new Department();
        department.setId(1L);

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(programRepository.existsByCodeAndDepartmentIdAndDeletedAtIsNull("BTCS", 1L)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> programService.create(request));

        assertTrue(exception.getMessage().contains("BTCS"));
        verify(programRepository, never()).save(any());
    }

    // --- delete ---

    @Test
    void delete_hasBatches_throwsBusinessRuleViolation() {
        Program program = new Program();
        program.setId(1L);
        program.setCode("BTCS");
        program.setIsActive(true);

        when(programRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(program));
        when(batchRepository.countByProgramIdAndDeletedAtIsNull(1L)).thenReturn(4L);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> programService.delete(1L));

        assertTrue(exception.getMessage().contains("Cannot delete program"));
        assertFalse(exception.getDetails().isEmpty());
        verify(programRepository, never()).save(any());
    }

    @Test
    void delete_noReferences_softDeletes() {
        Program program = new Program();
        program.setId(1L);
        program.setCode("BTCS");
        program.setIsActive(true);

        when(programRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(program));
        when(batchRepository.countByProgramIdAndDeletedAtIsNull(1L)).thenReturn(0L);

        programService.delete(1L);

        assertNotNull(program.getDeletedAt());
        assertFalse(program.getIsActive());
        verify(programRepository).save(program);
    }
}
