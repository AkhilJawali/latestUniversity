package com.utms.masterdata.batch;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.program.Program;
import com.utms.masterdata.program.ProgramRepository;
import com.utms.masterdata.section.SectionRepository;
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
class BatchServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private BatchMapper batchMapper;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private BatchService batchService;

    // --- create ---

    @Test
    void create_validRequest_returnsBatchDto() {
        CreateBatchRequest request = CreateBatchRequest.builder()
                .yearIdentifier("2024-25")
                .strength(60)
                .programId(1L)
                .electiveBasket("AI/ML")
                .build();

        Program program = new Program();
        program.setId(1L);
        program.setName("B.Tech CS");

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setYearIdentifier("2024-25");
        batch.setStrength(60);
        batch.setElectiveBasket("AI/ML");
        batch.setProgram(program);
        batch.setIsActive(true);

        BatchDto expectedDto = BatchDto.builder()
                .id(1L)
                .yearIdentifier("2024-25")
                .strength(60)
                .electiveBasket("AI/ML")
                .programId(1L)
                .programName("B.Tech CS")
                .build();

        when(programRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(program));
        when(batchMapper.toEntity(request)).thenReturn(batch);
        when(batchRepository.save(any(Batch.class))).thenReturn(batch);
        when(batchMapper.toDto(batch)).thenReturn(expectedDto);

        BatchDto result = batchService.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("2024-25", result.getYearIdentifier());
        assertEquals(60, result.getStrength());
        assertEquals(1L, result.getProgramId());
        verify(batchRepository).save(any(Batch.class));
    }

    @Test
    void create_invalidProgramId_throwsEntityNotFoundException() {
        CreateBatchRequest request = CreateBatchRequest.builder()
                .yearIdentifier("2024-25")
                .strength(60)
                .programId(99L)
                .build();

        when(programRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> batchService.create(request));

        assertTrue(exception.getMessage().contains("Program"));
        verify(batchRepository, never()).save(any());
    }

    // --- delete ---

    @Test
    void delete_hasSections_throwsBusinessRuleViolation() {
        Batch batch = new Batch();
        batch.setId(1L);
        batch.setYearIdentifier("2024-25");
        batch.setIsActive(true);

        when(batchRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(batch));
        when(sectionRepository.countByBatchIdAndDeletedAtIsNull(1L)).thenReturn(3L);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> batchService.delete(1L));

        assertTrue(exception.getMessage().contains("Cannot delete batch"));
        assertFalse(exception.getDetails().isEmpty());
        verify(batchRepository, never()).save(any());
    }

    @Test
    void delete_noReferences_softDeletes() {
        Batch batch = new Batch();
        batch.setId(1L);
        batch.setYearIdentifier("2024-25");
        batch.setIsActive(true);

        when(batchRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(batch));
        when(sectionRepository.countByBatchIdAndDeletedAtIsNull(1L)).thenReturn(0L);

        batchService.delete(1L);

        assertNotNull(batch.getDeletedAt());
        assertFalse(batch.getIsActive());
        verify(batchRepository).save(batch);
    }
}
