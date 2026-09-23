package com.utms.masterdata.section;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.batch.Batch;
import com.utms.masterdata.batch.BatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SectionMapper sectionMapper;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private SectionService sectionService;

    // --- create ---

    @Test
    void create_validRequest_returnsSectionDto() {
        Long batchId = 1L;
        CreateSectionRequest request = CreateSectionRequest.builder()
                .sectionIdentifier("A")
                .subStrength(30)
                .build();

        Batch batch = new Batch();
        batch.setId(batchId);
        batch.setStrength(60);

        Section section = new Section();
        section.setId(1L);
        section.setSectionIdentifier("A");
        section.setSubStrength(30);
        section.setBatch(batch);
        section.setIsActive(true);

        SectionDto expectedDto = SectionDto.builder()
                .id(1L)
                .sectionIdentifier("A")
                .subStrength(30)
                .batchId(batchId)
                .build();

        when(batchRepository.findByIdAndDeletedAtIsNull(batchId)).thenReturn(Optional.of(batch));
        when(sectionRepository.existsBySectionIdentifierAndBatchIdAndDeletedAtIsNull("A", batchId)).thenReturn(false);
        when(sectionMapper.toEntity(request)).thenReturn(section);
        when(sectionRepository.save(any(Section.class))).thenReturn(section);
        when(sectionMapper.toDto(section)).thenReturn(expectedDto);

        SectionDto result = sectionService.create(batchId, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("A", result.getSectionIdentifier());
        assertEquals(30, result.getSubStrength());
        assertEquals(batchId, result.getBatchId());
        verify(sectionRepository).save(any(Section.class));
    }

    @Test
    void create_invalidBatchId_throwsEntityNotFoundException() {
        Long batchId = 99L;
        CreateSectionRequest request = CreateSectionRequest.builder()
                .sectionIdentifier("A")
                .subStrength(30)
                .build();

        when(batchRepository.findByIdAndDeletedAtIsNull(batchId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> sectionService.create(batchId, request));

        assertTrue(exception.getMessage().contains("Batch"));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void create_duplicateIdentifierSameBatch_throwsConflict() {
        Long batchId = 1L;
        CreateSectionRequest request = CreateSectionRequest.builder()
                .sectionIdentifier("A")
                .subStrength(30)
                .build();

        Batch batch = new Batch();
        batch.setId(batchId);
        batch.setStrength(60);

        when(batchRepository.findByIdAndDeletedAtIsNull(batchId)).thenReturn(Optional.of(batch));
        when(sectionRepository.existsBySectionIdentifierAndBatchIdAndDeletedAtIsNull("A", batchId)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> sectionService.create(batchId, request));

        assertTrue(exception.getMessage().contains("A"));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void create_subStrengthExceedsBatchStrength_throwsBusinessRuleViolation() {
        Long batchId = 1L;
        CreateSectionRequest request = CreateSectionRequest.builder()
                .sectionIdentifier("A")
                .subStrength(100)
                .build();

        Batch batch = new Batch();
        batch.setId(batchId);
        batch.setStrength(60);

        when(batchRepository.findByIdAndDeletedAtIsNull(batchId)).thenReturn(Optional.of(batch));
        when(sectionRepository.existsBySectionIdentifierAndBatchIdAndDeletedAtIsNull("A", batchId)).thenReturn(false);
        when(sectionRepository.findAllByBatchIdAndDeletedAtIsNull(batchId)).thenReturn(List.of());

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> sectionService.create(batchId, request));

        assertTrue(exception.getMessage().contains("Total section sub-strength"));
        assertTrue(exception.getMessage().contains("100"));
        assertTrue(exception.getMessage().contains("60"));
        verify(sectionRepository, never()).save(any());
    }

    // --- update ---

    @Test
    void update_subStrengthExceedsBatchStrength_throwsBusinessRuleViolation() {
        Long sectionId = 1L;
        UpdateSectionRequest request = UpdateSectionRequest.builder()
                .subStrength(100)
                .build();

        Batch batch = new Batch();
        batch.setId(1L);
        batch.setStrength(60);

        Section section = new Section();
        section.setId(sectionId);
        section.setSectionIdentifier("A");
        section.setSubStrength(30);
        section.setBatch(batch);

        when(sectionRepository.findByIdAndDeletedAtIsNull(sectionId)).thenReturn(Optional.of(section));
        when(sectionRepository.findAllByBatchIdAndDeletedAtIsNull(1L)).thenReturn(List.of(section));

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> sectionService.update(sectionId, request));

        assertTrue(exception.getMessage().contains("Total section sub-strength"));
        assertTrue(exception.getMessage().contains("100"));
        assertTrue(exception.getMessage().contains("60"));
        verify(sectionRepository, never()).save(any());
    }

    // --- delete ---

    @Test
    void delete_softDeletes() {
        Section section = new Section();
        section.setId(1L);
        section.setSectionIdentifier("A");
        section.setIsActive(true);

        when(sectionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(section));

        sectionService.delete(1L);

        assertNotNull(section.getDeletedAt());
        assertFalse(section.getIsActive());
        verify(sectionRepository).save(section);
    }
}
