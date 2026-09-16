package com.utms.scheduling.config;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.masterdata.timeslot.SlotDefinition;
import com.utms.masterdata.timeslot.SlotDefinitionRepository;
import com.utms.scheduling.engine.entity.InstitutionCommonSlot;
import com.utms.scheduling.engine.repository.InstitutionCommonSlotRepository;
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
class CommonSlotServiceTest {

    @Mock
    private InstitutionCommonSlotRepository repository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private SlotDefinitionRepository slotDefinitionRepository;

    @Mock
    private CommonSlotMapper mapper;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private CommonSlotService service;

    private CreateCommonSlotRequest validCreate() {
        return CreateCommonSlotRequest.builder()
                .campusId(1L)
                .name("Common Connect Class")
                .dayOfWeek("MONDAY")
                .slotDefinitionId(5L)
                .appliesToAllBatches(true)
                .build();
    }

    private InstitutionCommonSlot entity(Long id, Long campusId, Long slotDefId) {
        InstitutionCommonSlot e = new InstitutionCommonSlot();
        e.setId(id);
        e.setCampusId(campusId);
        e.setName("Common Connect Class");
        e.setDayOfWeek("MONDAY");
        e.setSlotDefinitionId(slotDefId);
        e.setAppliesToAllBatches(true);
        e.setIsActive(true);
        return e;
    }

    @Test
    void create_validRequest_returnsDto() {
        CreateCommonSlotRequest request = validCreate();
        InstitutionCommonSlot saved = entity(1L, 1L, 5L);
        CommonSlotDto dto = CommonSlotDto.builder().id(1L).campusId(1L).name("Common Connect Class").build();

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(new Campus()));
        when(slotDefinitionRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(new SlotDefinition()));
        when(mapper.toEntity(request)).thenReturn(entity(null, 1L, 5L));
        when(repository.save(any(InstitutionCommonSlot.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(dto);

        CommonSlotDto result = service.create(request);

        assertNotNull(result);
        assertEquals("Common Connect Class", result.getName());
        verify(repository).save(any(InstitutionCommonSlot.class));
        verify(auditEventPublisher).publish(any());
    }

    @Test
    void create_missingCampus_throwsEntityNotFound() {
        CreateCommonSlotRequest request = validCreate();
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("Campus"));
        verify(repository, never()).save(any());
    }

    @Test
    void create_missingSlotDefinition_throwsEntityNotFound() {
        CreateCommonSlotRequest request = validCreate();
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(new Campus()));
        when(slotDefinitionRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("SlotDefinition"));
        verify(repository, never()).save(any());
    }

    @Test
    void update_changedSlotDefinition_revalidatesFk() {
        InstitutionCommonSlot existing = entity(1L, 1L, 5L);
        UpdateCommonSlotRequest request = UpdateCommonSlotRequest.builder()
                .name("Updated")
                .dayOfWeek("TUESDAY")
                .slotDefinitionId(9L)
                .appliesToAllBatches(false)
                .build();

        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));
        when(slotDefinitionRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(new SlotDefinition()));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toDto(existing)).thenReturn(CommonSlotDto.builder().id(1L).build());

        service.update(1L, request);

        verify(slotDefinitionRepository).findByIdAndDeletedAtIsNull(9L);
        verify(mapper).updateEntity(request, existing);
        verify(auditEventPublisher).publish(any());
    }

    @Test
    void update_notFound_throwsEntityNotFound() {
        UpdateCommonSlotRequest request = UpdateCommonSlotRequest.builder()
                .name("X").dayOfWeek("MONDAY").slotDefinitionId(5L).build();
        when(repository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.update(99L, request));
        verify(repository, never()).save(any());
    }

    @Test
    void delete_existing_softDeletes() {
        InstitutionCommonSlot existing = entity(1L, 1L, 5L);
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
