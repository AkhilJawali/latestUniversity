package com.utms.masterdata.timeslot;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotGridServiceTest {

    @Mock
    private TimeSlotGridRepository gridRepository;

    @Mock
    private SlotDefinitionRepository slotRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private TimeSlotGridMapper mapper;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private TimeSlotGridService timeSlotGridService;

    // --- createGrid ---

    @Test
    void createGrid_validRequest_returnsDto() {
        // Arrange
        CreateSlotDefinitionRequest slotReq = CreateSlotDefinitionRequest.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .slotType(SlotType.TEACHING)
                .applicableDay(null)
                .build();

        CreateTimeSlotGridRequest request = CreateTimeSlotGridRequest.builder()
                .campusId(1L)
                .gridName("Main Grid")
                .slots(List.of(slotReq))
                .build();

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");

        TimeSlotGrid savedGrid = new TimeSlotGrid();
        savedGrid.setId(10L);
        savedGrid.setGridName("Main Grid");
        savedGrid.setCampus(campus);
        savedGrid.setIsActive(true);

        SlotDefinition savedSlot = new SlotDefinition();
        savedSlot.setId(100L);
        savedSlot.setGrid(savedGrid);
        savedSlot.setStartTime(LocalTime.of(9, 0));
        savedSlot.setEndTime(LocalTime.of(10, 0));
        savedSlot.setSlotType(SlotType.TEACHING);
        savedSlot.setIsActive(true);

        TimeSlotGridDto expectedDto = TimeSlotGridDto.builder()
                .id(10L)
                .campusId(1L)
                .gridName("Main Grid")
                .slots(List.of(SlotDefinitionDto.builder()
                        .id(100L)
                        .gridId(10L)
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(10, 0))
                        .durationMinutes(60)
                        .slotType(SlotType.TEACHING)
                        .build()))
                .build();

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(gridRepository.existsByCampusIdAndDeletedAtIsNull(1L)).thenReturn(false);
        when(campusRepository.getReferenceById(1L)).thenReturn(campus);
        when(gridRepository.save(any(TimeSlotGrid.class))).thenReturn(savedGrid);
        when(mapper.toSlotEntity(any(CreateSlotDefinitionRequest.class))).thenReturn(savedSlot);
        when(slotRepository.save(any(SlotDefinition.class))).thenReturn(savedSlot);
        when(mapper.toDto(any(TimeSlotGrid.class))).thenReturn(expectedDto);

        // Act
        TimeSlotGridDto result = timeSlotGridService.create(request);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Main Grid", result.getGridName());
        assertEquals(1L, result.getCampusId());
        assertEquals(1, result.getSlots().size());
        verify(gridRepository).save(any(TimeSlotGrid.class));
        verify(slotRepository).save(any(SlotDefinition.class));
        verify(auditEventPublisher).publish(any());
    }

    @Test
    void createGrid_duplicateCampus_throwsConflict() {
        // Arrange: PD-61 — one grid per campus
        CreateSlotDefinitionRequest slotReq = CreateSlotDefinitionRequest.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .slotType(SlotType.TEACHING)
                .applicableDay(null)
                .build();

        CreateTimeSlotGridRequest request = CreateTimeSlotGridRequest.builder()
                .campusId(1L)
                .gridName("Duplicate Grid")
                .slots(List.of(slotReq))
                .build();

        Campus campus = new Campus();
        campus.setId(1L);

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(gridRepository.existsByCampusIdAndDeletedAtIsNull(1L)).thenReturn(true);

        // Act & Assert
        ConflictException ex = assertThrows(ConflictException.class, () ->
                timeSlotGridService.create(request));
        assertTrue(ex.getMessage().contains("active time-slot grid already exists"));
        verify(gridRepository, never()).save(any());
    }

    // --- addSlot ---

    @Test
    void addSlot_validNoOverlap_persists() {
        // Arrange: existing grid with one slot 09:00-10:00, adding 10:00-11:00 (no overlap)
        Long gridId = 10L;
        TimeSlotGrid grid = new TimeSlotGrid();
        grid.setId(gridId);
        grid.setIsActive(true);

        SlotDefinition existingSlot = new SlotDefinition();
        existingSlot.setId(100L);
        existingSlot.setGridId(gridId);
        existingSlot.setStartTime(LocalTime.of(9, 0));
        existingSlot.setEndTime(LocalTime.of(10, 0));
        existingSlot.setSlotType(SlotType.TEACHING);
        existingSlot.setApplicableDay(null); // ALL days

        CreateSlotDefinitionRequest request = CreateSlotDefinitionRequest.builder()
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .slotType(SlotType.TEACHING)
                .applicableDay(null)
                .build();

        SlotDefinition newSlot = new SlotDefinition();
        newSlot.setId(101L);
        newSlot.setGridId(gridId);
        newSlot.setStartTime(LocalTime.of(10, 0));
        newSlot.setEndTime(LocalTime.of(11, 0));
        newSlot.setSlotType(SlotType.TEACHING);
        newSlot.setIsActive(true);

        SlotDefinitionDto expectedDto = SlotDefinitionDto.builder()
                .id(101L)
                .gridId(gridId)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .durationMinutes(60)
                .slotType(SlotType.TEACHING)
                .build();

        when(gridRepository.findByIdAndDeletedAtIsNull(gridId)).thenReturn(Optional.of(grid));
        when(slotRepository.findByGridIdAndDeletedAtIsNull(gridId)).thenReturn(List.of(existingSlot));
        when(mapper.toSlotEntity(any(CreateSlotDefinitionRequest.class))).thenReturn(newSlot);
        when(slotRepository.save(any(SlotDefinition.class))).thenReturn(newSlot);
        when(mapper.toSlotDto(any(SlotDefinition.class))).thenReturn(expectedDto);

        // Act
        SlotDefinitionDto result = timeSlotGridService.addSlot(gridId, request);

        // Assert
        assertNotNull(result);
        assertEquals(101L, result.getId());
        verify(slotRepository).save(any(SlotDefinition.class));
        verify(auditEventPublisher).publish(any());
    }

    @Test
    void addSlot_overlapsExistingAllDaysSameTime_throwsBusinessRule() {
        // Arrange: existing ALL-days slot 09:00-10:00, adding another ALL-days 09:30-10:30 (overlap)
        // KD-41: null vs null = overlap check applies
        Long gridId = 10L;
        TimeSlotGrid grid = new TimeSlotGrid();
        grid.setId(gridId);
        grid.setIsActive(true);

        SlotDefinition existingSlot = new SlotDefinition();
        existingSlot.setId(100L);
        existingSlot.setGridId(gridId);
        existingSlot.setStartTime(LocalTime.of(9, 0));
        existingSlot.setEndTime(LocalTime.of(10, 0));
        existingSlot.setSlotType(SlotType.TEACHING);
        existingSlot.setApplicableDay(null); // ALL days

        CreateSlotDefinitionRequest request = CreateSlotDefinitionRequest.builder()
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(10, 30))
                .slotType(SlotType.TEACHING)
                .applicableDay(null) // ALL days — should conflict
                .build();

        when(gridRepository.findByIdAndDeletedAtIsNull(gridId)).thenReturn(Optional.of(grid));
        when(slotRepository.findByGridIdAndDeletedAtIsNull(gridId)).thenReturn(List.of(existingSlot));

        // Act & Assert
        ConflictException ex = assertThrows(ConflictException.class, () ->
                timeSlotGridService.addSlot(gridId, request));
        assertTrue(ex.getMessage().contains("overlaps"));
        verify(slotRepository, never()).save(any());
    }

    @Test
    void addSlot_allDaysAndFridayOverrideSameTime_succeeds() {
        // KD-41: ALL-days (null) + FRIDAY (specific-day) at same time = override, NOT overlap
        // slotsCanOverlap(null, FRIDAY) returns false → no conflict
        Long gridId = 10L;
        TimeSlotGrid grid = new TimeSlotGrid();
        grid.setId(gridId);
        grid.setIsActive(true);

        SlotDefinition existingAllDaysSlot = new SlotDefinition();
        existingAllDaysSlot.setId(100L);
        existingAllDaysSlot.setGridId(gridId);
        existingAllDaysSlot.setStartTime(LocalTime.of(9, 0));
        existingAllDaysSlot.setEndTime(LocalTime.of(10, 0));
        existingAllDaysSlot.setSlotType(SlotType.TEACHING);
        existingAllDaysSlot.setApplicableDay(null); // ALL days

        // Adding FRIDAY-specific slot at same time — should succeed (override)
        CreateSlotDefinitionRequest request = CreateSlotDefinitionRequest.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .slotType(SlotType.TEACHING)
                .applicableDay(DayOfWeekEnum.FRIDAY)
                .build();

        SlotDefinition newSlot = new SlotDefinition();
        newSlot.setId(101L);
        newSlot.setGridId(gridId);
        newSlot.setStartTime(LocalTime.of(9, 0));
        newSlot.setEndTime(LocalTime.of(10, 0));
        newSlot.setSlotType(SlotType.TEACHING);
        newSlot.setApplicableDay(DayOfWeekEnum.FRIDAY);
        newSlot.setIsActive(true);

        SlotDefinitionDto expectedDto = SlotDefinitionDto.builder()
                .id(101L)
                .gridId(gridId)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .durationMinutes(60)
                .slotType(SlotType.TEACHING)
                .applicableDay(DayOfWeekEnum.FRIDAY)
                .build();

        when(gridRepository.findByIdAndDeletedAtIsNull(gridId)).thenReturn(Optional.of(grid));
        when(slotRepository.findByGridIdAndDeletedAtIsNull(gridId)).thenReturn(List.of(existingAllDaysSlot));
        when(mapper.toSlotEntity(any(CreateSlotDefinitionRequest.class))).thenReturn(newSlot);
        when(slotRepository.save(any(SlotDefinition.class))).thenReturn(newSlot);
        when(mapper.toSlotDto(any(SlotDefinition.class))).thenReturn(expectedDto);

        // Act
        SlotDefinitionDto result = timeSlotGridService.addSlot(gridId, request);

        // Assert — no exception, slot persisted
        assertNotNull(result);
        assertEquals(DayOfWeekEnum.FRIDAY, result.getApplicableDay());
        verify(slotRepository).save(any(SlotDefinition.class));
    }

    @Test
    void addSlot_twoFridaySlotsOverlap_throwsBusinessRule() {
        // KD-41: FRIDAY + FRIDAY at same time = overlap (same day scope)
        // slotsCanOverlap(FRIDAY, FRIDAY) returns true → conflict detected
        Long gridId = 10L;
        TimeSlotGrid grid = new TimeSlotGrid();
        grid.setId(gridId);
        grid.setIsActive(true);

        SlotDefinition existingFridaySlot = new SlotDefinition();
        existingFridaySlot.setId(100L);
        existingFridaySlot.setGridId(gridId);
        existingFridaySlot.setStartTime(LocalTime.of(9, 0));
        existingFridaySlot.setEndTime(LocalTime.of(10, 0));
        existingFridaySlot.setSlotType(SlotType.TEACHING);
        existingFridaySlot.setApplicableDay(DayOfWeekEnum.FRIDAY);

        // Adding another FRIDAY slot overlapping same time — should fail
        CreateSlotDefinitionRequest request = CreateSlotDefinitionRequest.builder()
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(10, 30))
                .slotType(SlotType.TEACHING)
                .applicableDay(DayOfWeekEnum.FRIDAY)
                .build();

        when(gridRepository.findByIdAndDeletedAtIsNull(gridId)).thenReturn(Optional.of(grid));
        when(slotRepository.findByGridIdAndDeletedAtIsNull(gridId)).thenReturn(List.of(existingFridaySlot));

        // Act & Assert
        ConflictException ex = assertThrows(ConflictException.class, () ->
                timeSlotGridService.addSlot(gridId, request));
        assertTrue(ex.getMessage().contains("overlaps"));
        verify(slotRepository, never()).save(any());
    }

    // --- removeSlot ---

    @Test
    void removeSlot_noSessionsRef_softDeletes() {
        // KD-42: soft-delete when no sessions reference the slot
        Long gridId = 10L;
        Long slotId = 100L;

        TimeSlotGrid grid = new TimeSlotGrid();
        grid.setId(gridId);
        grid.setIsActive(true);

        SlotDefinition slot = new SlotDefinition();
        slot.setId(slotId);
        slot.setGridId(gridId);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setSlotType(SlotType.TEACHING);
        slot.setIsActive(true);

        when(gridRepository.findByIdAndDeletedAtIsNull(gridId)).thenReturn(Optional.of(grid));
        when(slotRepository.findByIdAndDeletedAtIsNull(slotId)).thenReturn(Optional.of(slot));
        // More than 1 teaching slot remains so removal is allowed
        when(slotRepository.countByGridIdAndDeletedAtIsNullAndSlotType(gridId, SlotType.TEACHING)).thenReturn(2L);

        // Act
        timeSlotGridService.removeSlot(gridId, slotId);

        // Assert — slot is soft-deleted (deletedAt set, isActive = false)
        verify(slotRepository).save(slot);
        assertNotNull(slot.getDeletedAt());
        assertFalse(slot.getIsActive());
        verify(auditEventPublisher).publish(any());
    }

    @Test
    void removeSlot_hasSessionsRef_throws422() {
        // KD-43: removal guard — cannot remove the last TEACHING slot (HC-GRID-4)
        // The full session-reference guard is a TODO in the service (awaiting scheduling module).
        // This test validates the existing guard which blocks removal when it would leave
        // zero teaching slots, yielding a 422 BusinessRuleViolationException.
        Long gridId = 10L;
        Long slotId = 100L;

        TimeSlotGrid grid = new TimeSlotGrid();
        grid.setId(gridId);
        grid.setIsActive(true);

        SlotDefinition slot = new SlotDefinition();
        slot.setId(slotId);
        slot.setGridId(gridId);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setSlotType(SlotType.TEACHING);
        slot.setIsActive(true);

        when(gridRepository.findByIdAndDeletedAtIsNull(gridId)).thenReturn(Optional.of(grid));
        when(slotRepository.findByIdAndDeletedAtIsNull(slotId)).thenReturn(Optional.of(slot));
        // Only 1 teaching slot — removal blocked (HC-GRID-4)
        when(slotRepository.countByGridIdAndDeletedAtIsNullAndSlotType(gridId, SlotType.TEACHING)).thenReturn(1L);

        // Act & Assert
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () ->
                timeSlotGridService.removeSlot(gridId, slotId));
        assertTrue(ex.getMessage().contains("Cannot remove the last teaching slot"));
        verify(slotRepository, never()).save(any());
    }

    // --- getEffectiveSlotsForDay ---

    @Test
    void getEffectiveSlotsForDay_fridayWithOverride_returnsDaySpecific() {
        // KD-41/PD-65: FRIDAY-specific slot overrides ALL-days slot at overlapping time
        Long gridId = 10L;

        TimeSlotGrid grid = new TimeSlotGrid();
        grid.setId(gridId);
        grid.setIsActive(true);

        // ALL-days slot at 09:00-10:00
        SlotDefinition allDaysSlot = new SlotDefinition();
        allDaysSlot.setId(100L);
        allDaysSlot.setGridId(gridId);
        allDaysSlot.setStartTime(LocalTime.of(9, 0));
        allDaysSlot.setEndTime(LocalTime.of(10, 0));
        allDaysSlot.setSlotType(SlotType.TEACHING);
        allDaysSlot.setApplicableDay(null);

        // FRIDAY-specific override at 09:00-10:00 (BREAK instead of TEACHING)
        SlotDefinition fridaySlot = new SlotDefinition();
        fridaySlot.setId(101L);
        fridaySlot.setGridId(gridId);
        fridaySlot.setStartTime(LocalTime.of(9, 0));
        fridaySlot.setEndTime(LocalTime.of(10, 0));
        fridaySlot.setSlotType(SlotType.BREAK);
        fridaySlot.setApplicableDay(DayOfWeekEnum.FRIDAY);

        SlotDefinitionDto fridayDto = SlotDefinitionDto.builder()
                .id(101L)
                .gridId(gridId)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .durationMinutes(60)
                .slotType(SlotType.BREAK)
                .applicableDay(DayOfWeekEnum.FRIDAY)
                .build();

        when(gridRepository.findByIdAndDeletedAtIsNull(gridId)).thenReturn(Optional.of(grid));
        when(slotRepository.findByGridIdAndApplicableDayAndDeletedAtIsNull(gridId, DayOfWeekEnum.FRIDAY))
                .thenReturn(List.of(fridaySlot));
        when(slotRepository.findByGridIdAndApplicableDayIsNullAndDeletedAtIsNull(gridId))
                .thenReturn(List.of(allDaysSlot));
        // The friday slot overrides the all-days slot, so only friday slot in effective list
        when(mapper.toSlotDtoList(any())).thenReturn(List.of(fridayDto));

        // Act
        List<SlotDefinitionDto> result = timeSlotGridService.getEffectiveSlotsForDay(gridId, DayOfWeekEnum.FRIDAY);

        // Assert — only friday-specific slot returned (all-days overridden)
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(DayOfWeekEnum.FRIDAY, result.get(0).getApplicableDay());
        assertEquals(SlotType.BREAK, result.get(0).getSlotType());
    }

    @Test
    void getEffectiveSlotsForDay_mondayNoOverride_returnsAllDays() {
        // When no day-specific override exists for MONDAY, all-day slots are returned intact
        Long gridId = 10L;

        TimeSlotGrid grid = new TimeSlotGrid();
        grid.setId(gridId);
        grid.setIsActive(true);

        // ALL-days slot
        SlotDefinition allDaysSlot = new SlotDefinition();
        allDaysSlot.setId(100L);
        allDaysSlot.setGridId(gridId);
        allDaysSlot.setStartTime(LocalTime.of(9, 0));
        allDaysSlot.setEndTime(LocalTime.of(10, 0));
        allDaysSlot.setSlotType(SlotType.TEACHING);
        allDaysSlot.setApplicableDay(null);

        SlotDefinitionDto allDaysDto = SlotDefinitionDto.builder()
                .id(100L)
                .gridId(gridId)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .durationMinutes(60)
                .slotType(SlotType.TEACHING)
                .applicableDay(null)
                .build();

        when(gridRepository.findByIdAndDeletedAtIsNull(gridId)).thenReturn(Optional.of(grid));
        when(slotRepository.findByGridIdAndApplicableDayAndDeletedAtIsNull(gridId, DayOfWeekEnum.MONDAY))
                .thenReturn(List.of()); // No monday-specific slots
        when(slotRepository.findByGridIdAndApplicableDayIsNullAndDeletedAtIsNull(gridId))
                .thenReturn(List.of(allDaysSlot));
        when(mapper.toSlotDtoList(any())).thenReturn(List.of(allDaysDto));

        // Act
        List<SlotDefinitionDto> result = timeSlotGridService.getEffectiveSlotsForDay(gridId, DayOfWeekEnum.MONDAY);

        // Assert — all-days slot returned since no monday override exists
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getApplicableDay());
        assertEquals(SlotType.TEACHING, result.get(0).getSlotType());
    }
}
