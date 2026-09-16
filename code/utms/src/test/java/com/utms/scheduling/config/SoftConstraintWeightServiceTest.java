package com.utms.scheduling.config;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.scheduling.engine.entity.SoftConstraintWeight;
import com.utms.scheduling.engine.repository.SoftConstraintWeightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoftConstraintWeightServiceTest {

    @Mock
    private SoftConstraintWeightRepository repository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private SoftConstraintWeightMapper mapper;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private SoftConstraintWeightService service;

    private CreateSoftConstraintWeightRequest validCreate() {
        return CreateSoftConstraintWeightRequest.builder()
                .campusId(1L)
                .constraintType("ROOM_PROXIMITY")
                .weight(new BigDecimal("2.50"))
                .build();
    }

    private SoftConstraintWeight entity(Long id, Long campusId, String constraintType) {
        SoftConstraintWeight e = new SoftConstraintWeight();
        e.setId(id);
        e.setCampusId(campusId);
        e.setConstraintType(constraintType);
        e.setWeight(new BigDecimal("2.50"));
        e.setIsActive(true);
        return e;
    }

    @Test
    void create_validRequest_returnsDto() {
        CreateSoftConstraintWeightRequest request = validCreate();
        SoftConstraintWeight saved = entity(1L, 1L, "ROOM_PROXIMITY");
        SoftConstraintWeightDto dto = SoftConstraintWeightDto.builder()
                .id(1L).campusId(1L).constraintType("ROOM_PROXIMITY").weight(new BigDecimal("2.50")).build();

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(new Campus()));
        when(repository.existsByCampusIdAndConstraintTypeAndDeletedAtIsNull(1L, "ROOM_PROXIMITY")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity(null, 1L, "ROOM_PROXIMITY"));
        when(repository.save(any(SoftConstraintWeight.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(dto);

        SoftConstraintWeightDto result = service.create(request);

        assertNotNull(result);
        assertEquals("ROOM_PROXIMITY", result.getConstraintType());
        verify(repository).save(any(SoftConstraintWeight.class));
        verify(auditEventPublisher).publish(any());
    }

    @Test
    void create_missingCampus_throwsEntityNotFound() {
        CreateSoftConstraintWeightRequest request = validCreate();
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("Campus"));
        verify(repository, never()).save(any());
    }

    @Test
    void create_duplicateConstraintType_throwsConflict() {
        CreateSoftConstraintWeightRequest request = validCreate();
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(new Campus()));
        when(repository.existsByCampusIdAndConstraintTypeAndDeletedAtIsNull(1L, "ROOM_PROXIMITY")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("ROOM_PROXIMITY"));
        verify(repository, never()).save(any());
    }

    @Test
    void update_changedConstraintType_rechecksUniqueness() {
        SoftConstraintWeight existing = entity(1L, 1L, "ROOM_PROXIMITY");
        UpdateSoftConstraintWeightRequest request = UpdateSoftConstraintWeightRequest.builder()
                .constraintType("GAP_MINIMIZATION")
                .weight(new BigDecimal("3.00"))
                .build();

        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByCampusIdAndConstraintTypeAndDeletedAtIsNull(1L, "GAP_MINIMIZATION")).thenReturn(false);
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toDto(existing)).thenReturn(SoftConstraintWeightDto.builder().id(1L).constraintType("GAP_MINIMIZATION").build());

        service.update(1L, request);

        verify(repository).existsByCampusIdAndConstraintTypeAndDeletedAtIsNull(1L, "GAP_MINIMIZATION");
        verify(mapper).updateEntity(request, existing);
        verify(auditEventPublisher).publish(any());
    }

    @Test
    void update_notFound_throwsEntityNotFound() {
        UpdateSoftConstraintWeightRequest request = UpdateSoftConstraintWeightRequest.builder()
                .constraintType("ROOM_PROXIMITY").weight(new BigDecimal("1.00")).build();
        when(repository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.update(99L, request));
        verify(repository, never()).save(any());
    }

    @Test
    void delete_existing_softDeletes() {
        SoftConstraintWeight existing = entity(1L, 1L, "ROOM_PROXIMITY");
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        assertNotNull(existing.getDeletedAt());
        assertFalse(existing.getIsActive());
        verify(repository).save(existing);
        verify(auditEventPublisher).publish(any());
    }

    @Test
    void delete_notFound_throwsEntityNotFound() {
        when(repository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.delete(99L));
        verify(repository, never()).save(any());
    }
}
