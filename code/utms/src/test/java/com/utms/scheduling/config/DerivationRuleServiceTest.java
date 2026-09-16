package com.utms.scheduling.config;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.scheduling.engine.entity.SessionDerivationRule;
import com.utms.scheduling.engine.repository.SessionDerivationRuleRepository;
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
class DerivationRuleServiceTest {

    @Mock
    private SessionDerivationRuleRepository repository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private DerivationRuleMapper mapper;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private DerivationRuleService service;

    private CreateDerivationRuleRequest validCreate() {
        return CreateDerivationRuleRequest.builder()
                .campusId(1L)
                .componentType("LECTURE")
                .slotDurationMinutes(60)
                .hoursPerSession(new BigDecimal("1.0"))
                .description("Lecture")
                .build();
    }

    private SessionDerivationRule entity(Long id, Long campusId, String componentType) {
        SessionDerivationRule e = new SessionDerivationRule();
        e.setId(id);
        e.setCampusId(campusId);
        e.setComponentType(componentType);
        e.setSlotDurationMinutes(60);
        e.setHoursPerSession(new BigDecimal("1.0"));
        e.setIsActive(true);
        return e;
    }

    // --- create ---

    @Test
    void create_validRequest_returnsDto() {
        CreateDerivationRuleRequest request = validCreate();
        SessionDerivationRule saved = entity(1L, 1L, "LECTURE");
        DerivationRuleDto dto = DerivationRuleDto.builder().id(1L).campusId(1L).componentType("LECTURE").build();

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(new Campus()));
        when(repository.existsByCampusIdAndComponentTypeAndDeletedAtIsNull(1L, "LECTURE")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity(null, 1L, "LECTURE"));
        when(repository.save(any(SessionDerivationRule.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(dto);

        DerivationRuleDto result = service.create(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("LECTURE", result.getComponentType());
        verify(repository).save(any(SessionDerivationRule.class));
        verify(auditEventPublisher).publish(any());
    }

    @Test
    void create_missingCampus_throwsEntityNotFound() {
        CreateDerivationRuleRequest request = validCreate();
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("Campus"));
        verify(repository, never()).save(any());
    }

    @Test
    void create_duplicateComponentType_throwsConflict() {
        CreateDerivationRuleRequest request = validCreate();
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(new Campus()));
        when(repository.existsByCampusIdAndComponentTypeAndDeletedAtIsNull(1L, "LECTURE")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("LECTURE"));
        verify(repository, never()).save(any());
    }

    // --- update ---

    @Test
    void update_changedComponentType_rechecksUniqueness() {
        SessionDerivationRule existing = entity(1L, 1L, "LECTURE");
        UpdateDerivationRuleRequest request = UpdateDerivationRuleRequest.builder()
                .componentType("TUTORIAL")
                .slotDurationMinutes(90)
                .hoursPerSession(new BigDecimal("1.5"))
                .build();
        DerivationRuleDto dto = DerivationRuleDto.builder().id(1L).componentType("TUTORIAL").build();

        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByCampusIdAndComponentTypeAndDeletedAtIsNull(1L, "TUTORIAL")).thenReturn(false);
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toDto(existing)).thenReturn(dto);

        DerivationRuleDto result = service.update(1L, request);

        assertEquals("TUTORIAL", result.getComponentType());
        verify(repository).existsByCampusIdAndComponentTypeAndDeletedAtIsNull(1L, "TUTORIAL");
        verify(mapper).updateEntity(request, existing);
        verify(auditEventPublisher).publish(any());
    }

    @Test
    void update_sameComponentType_skipsUniquenessCheck() {
        SessionDerivationRule existing = entity(1L, 1L, "LECTURE");
        UpdateDerivationRuleRequest request = UpdateDerivationRuleRequest.builder()
                .componentType("LECTURE")
                .slotDurationMinutes(90)
                .hoursPerSession(new BigDecimal("1.5"))
                .build();

        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toDto(existing)).thenReturn(DerivationRuleDto.builder().id(1L).componentType("LECTURE").build());

        service.update(1L, request);

        verify(repository, never()).existsByCampusIdAndComponentTypeAndDeletedAtIsNull(anyLong(), anyString());
    }

    @Test
    void update_notFound_throwsEntityNotFound() {
        UpdateDerivationRuleRequest request = UpdateDerivationRuleRequest.builder()
                .componentType("LECTURE").slotDurationMinutes(60).hoursPerSession(new BigDecimal("1.0")).build();
        when(repository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.update(99L, request));
        verify(repository, never()).save(any());
    }

    // --- delete ---

    @Test
    void delete_existing_softDeletes() {
        SessionDerivationRule existing = entity(1L, 1L, "LECTURE");
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
