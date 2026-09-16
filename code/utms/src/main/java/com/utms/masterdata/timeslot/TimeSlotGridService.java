package com.utms.masterdata.timeslot;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.campus.CampusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeSlotGridService {

    private final TimeSlotGridRepository gridRepository;
    private final SlotDefinitionRepository slotRepository;
    private final CampusRepository campusRepository;
    private final TimeSlotGridMapper mapper;
    private final AuditEventPublisher auditEventPublisher;

    @Transactional
    public TimeSlotGridDto create(CreateTimeSlotGridRequest request) {
        // Validate campus exists
        if (!campusRepository.findByIdAndDeletedAtIsNull(request.getCampusId()).isPresent()) {
            throw new EntityNotFoundException("Campus", request.getCampusId());
        }

        // One grid per campus (PD-61)
        if (gridRepository.existsByCampusIdAndDeletedAtIsNull(request.getCampusId())) {
            throw new ConflictException("An active time-slot grid already exists for campus ID " + request.getCampusId());
        }

        // Validate at least one TEACHING slot (HC-GRID-4)
        boolean hasTeachingSlot = request.getSlots().stream()
                .anyMatch(s -> s.getSlotType() == SlotType.TEACHING);
        if (!hasTeachingSlot) {
            throw new BusinessRuleViolationException(
                    "Grid must contain at least one TEACHING slot",
                    List.of(Map.of("rule", "HC-GRID-4", "message", "At least one teaching slot required")));
        }

        // Validate start < end for each slot (HC-GRID-3)
        for (CreateSlotDefinitionRequest slot : request.getSlots()) {
            if (!slot.getStartTime().isBefore(slot.getEndTime())) {
                throw new BusinessRuleViolationException(
                        "Slot start_time must be before end_time",
                        List.of(Map.of("field", "startTime", "rejectedValue", slot.getStartTime().toString(),
                                "endTime", slot.getEndTime().toString())));
            }
        }

        // Validate no overlapping slots (HC-GRID-1) — day-aware (KD-41)
        validateNoOverlaps(request.getSlots());

        // Persist grid
        TimeSlotGrid grid = new TimeSlotGrid();
        grid.setGridName(request.getGridName());
        grid.setCampus(campusRepository.getReferenceById(request.getCampusId()));
        grid.setIsActive(true);
        grid = gridRepository.save(grid);

        // Persist slots
        List<SlotDefinition> savedSlots = new ArrayList<>();
        for (CreateSlotDefinitionRequest slotReq : request.getSlots()) {
            SlotDefinition slot = mapper.toSlotEntity(slotReq);
            slot.setGrid(grid);
            slot.setIsActive(true);
            savedSlots.add(slotRepository.save(slot));
        }
        grid.setSlotDefinitions(savedSlots);

        auditEventPublisher.publish(new AuditEvent("TimeSlotGrid", grid.getId(),
                AuditEvent.Action.CREATED, null, grid, "system", Instant.now()));

        log.info("Time-slot grid created: id={}, campusId={}, slots={}", grid.getId(), request.getCampusId(), savedSlots.size());
        return mapper.toDto(grid);
    }

    @Transactional(readOnly = true)
    public TimeSlotGridDto findById(Long id) {
        TimeSlotGrid grid = findActiveGridOrThrow(id);
        // Eagerly load active slots
        List<SlotDefinition> activeSlots = slotRepository.findByGridIdAndDeletedAtIsNull(id);
        grid.setSlotDefinitions(activeSlots);
        return mapper.toDto(grid);
    }

    @Transactional(readOnly = true)
    public TimeSlotGridDto findByCampusId(Long campusId) {
        TimeSlotGrid grid = gridRepository.findByCampusIdAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid", "campusId", campusId.toString()));
        List<SlotDefinition> activeSlots = slotRepository.findByGridIdAndDeletedAtIsNull(grid.getId());
        grid.setSlotDefinitions(activeSlots);
        return mapper.toDto(grid);
    }

    @Transactional
    public TimeSlotGridDto updateGridName(Long id, UpdateTimeSlotGridRequest request) {
        TimeSlotGrid grid = findActiveGridOrThrow(id);
        String previousName = grid.getGridName();
        grid.setGridName(request.getGridName());
        grid = gridRepository.save(grid);

        auditEventPublisher.publish(new AuditEvent("TimeSlotGrid", grid.getId(),
                AuditEvent.Action.UPDATED, previousName, grid.getGridName(), "system", Instant.now()));

        log.info("Time-slot grid name updated: id={}", grid.getId());
        List<SlotDefinition> activeSlots = slotRepository.findByGridIdAndDeletedAtIsNull(grid.getId());
        grid.setSlotDefinitions(activeSlots);
        return mapper.toDto(grid);
    }

    @Transactional
    public SlotDefinitionDto addSlot(Long gridId, CreateSlotDefinitionRequest request) {
        TimeSlotGrid grid = findActiveGridOrThrow(gridId);

        // Validate start < end (HC-GRID-3)
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessRuleViolationException(
                    "Slot start_time must be before end_time",
                    List.of(Map.of("field", "startTime", "rejectedValue", request.getStartTime().toString(),
                            "endTime", request.getEndTime().toString())));
        }

        // Check overlap against existing active slots — day-aware (KD-41)
        List<SlotDefinition> existingSlots = slotRepository.findByGridIdAndDeletedAtIsNull(gridId);
        validateNewSlotNoOverlap(request, existingSlots);

        SlotDefinition slot = mapper.toSlotEntity(request);
        slot.setGrid(grid);
        slot.setIsActive(true);
        slot = slotRepository.save(slot);

        auditEventPublisher.publish(new AuditEvent("SlotDefinition", slot.getId(),
                AuditEvent.Action.CREATED, null, slot, "system", Instant.now()));

        log.info("Slot added to grid: gridId={}, slotId={}, type={}", gridId, slot.getId(), slot.getSlotType());
        return mapper.toSlotDto(slot);
    }

    @Transactional
    public void removeSlot(Long gridId, Long slotId) {
        TimeSlotGrid grid = findActiveGridOrThrow(gridId);
        SlotDefinition slot = slotRepository.findByIdAndDeletedAtIsNull(slotId)
                .orElseThrow(() -> new EntityNotFoundException("SlotDefinition", slotId));

        if (!slot.getGridId().equals(gridId)) {
            throw new EntityNotFoundException("SlotDefinition", slotId);
        }

        // Guard: ensure at least one teaching slot remains after removal (HC-GRID-4)
        if (slot.getSlotType() == SlotType.TEACHING) {
            long teachingCount = slotRepository.countByGridIdAndDeletedAtIsNullAndSlotType(gridId, SlotType.TEACHING);
            if (teachingCount <= 1) {
                throw new BusinessRuleViolationException(
                        "Cannot remove the last teaching slot from the grid",
                        List.of(Map.of("rule", "HC-GRID-4", "message", "Grid must retain at least one teaching slot")));
            }
        }

        // TODO: KD-43 — check session references once scheduling module exists
        // If sessions reference this slot, block removal with BusinessRuleViolationException

        // Soft-delete (KD-42)
        slot.setDeletedAt(LocalDateTime.now());
        slot.setIsActive(false);
        slotRepository.save(slot);

        auditEventPublisher.publish(new AuditEvent("SlotDefinition", slot.getId(),
                AuditEvent.Action.DELETED, slot, null, "system", Instant.now()));

        log.info("Slot soft-deleted: gridId={}, slotId={}", gridId, slotId);
    }

    @Transactional
    public void deleteGrid(Long id) {
        TimeSlotGrid grid = findActiveGridOrThrow(id);

        // TODO: KD-43 — check if active timetables reference this grid; block if so

        // Soft-delete all active slots
        List<SlotDefinition> activeSlots = slotRepository.findByGridIdAndDeletedAtIsNull(id);
        for (SlotDefinition slot : activeSlots) {
            slot.setDeletedAt(LocalDateTime.now());
            slot.setIsActive(false);
            slotRepository.save(slot);
        }

        // Soft-delete grid (PD-64)
        grid.setDeletedAt(LocalDateTime.now());
        grid.setIsActive(false);
        gridRepository.save(grid);

        auditEventPublisher.publish(new AuditEvent("TimeSlotGrid", grid.getId(),
                AuditEvent.Action.DELETED, grid, null, "system", Instant.now()));

        log.info("Time-slot grid soft-deleted: id={}, campusId={}", grid.getId(), grid.getCampusId());
    }

    /**
     * Returns the effective slots for a specific day by merging day-specific overrides
     * with all-days slots. Day-specific slots override all-days slots for overlapping time ranges.
     * (KD-41, PD-65)
     */
    @Transactional(readOnly = true)
    public List<SlotDefinitionDto> getEffectiveSlotsForDay(Long gridId, DayOfWeekEnum day) {
        findActiveGridOrThrow(gridId);

        // Get day-specific slots
        List<SlotDefinition> daySlots = slotRepository.findByGridIdAndApplicableDayAndDeletedAtIsNull(gridId, day);
        // Get all-days slots (applicable_day IS NULL)
        List<SlotDefinition> allDaySlots = slotRepository.findByGridIdAndApplicableDayIsNullAndDeletedAtIsNull(gridId);

        // Day-specific overrides: remove all-day slots that overlap with any day-specific slot
        List<SlotDefinition> nonOverriddenAllDay = allDaySlots.stream()
                .filter(allSlot -> daySlots.stream().noneMatch(daySlot -> timesOverlap(
                        allSlot.getStartTime(), allSlot.getEndTime(),
                        daySlot.getStartTime(), daySlot.getEndTime())))
                .collect(Collectors.toList());

        // Merge: day-specific + non-overridden all-day
        List<SlotDefinition> effective = Stream.concat(daySlots.stream(), nonOverriddenAllDay.stream())
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());

        return mapper.toSlotDtoList(effective);
    }

    // --- Overlap validation (KD-41: day-aware) ---

    /**
     * Validates that no two slots in the creation request overlap, considering day-awareness.
     * Rule: NULL vs NULL = overlap check applies.
     *       NULL vs specific-day = NO overlap (override, not conflict).
     *       Same-day vs same-day = overlap check applies.
     */
    private void validateNoOverlaps(List<CreateSlotDefinitionRequest> slots) {
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                CreateSlotDefinitionRequest a = slots.get(i);
                CreateSlotDefinitionRequest b = slots.get(j);

                if (slotsCanOverlap(a.getApplicableDay(), b.getApplicableDay())
                        && timesOverlap(a.getStartTime(), a.getEndTime(), b.getStartTime(), b.getEndTime())) {
                    throw new ConflictException(String.format(
                            "Slots overlap: [%s-%s] and [%s-%s] (day scope: %s / %s)",
                            a.getStartTime(), a.getEndTime(), b.getStartTime(), b.getEndTime(),
                            a.getApplicableDay() == null ? "ALL" : a.getApplicableDay(),
                            b.getApplicableDay() == null ? "ALL" : b.getApplicableDay()));
                }
            }
        }
    }

    /**
     * Validates a new slot does not overlap with any existing active slots (day-aware).
     */
    private void validateNewSlotNoOverlap(CreateSlotDefinitionRequest newSlot, List<SlotDefinition> existing) {
        for (SlotDefinition ex : existing) {
            if (slotsCanOverlap(newSlot.getApplicableDay(), ex.getApplicableDay())
                    && timesOverlap(newSlot.getStartTime(), newSlot.getEndTime(), ex.getStartTime(), ex.getEndTime())) {
                throw new ConflictException(String.format(
                        "New slot [%s-%s] overlaps existing slot id=%d [%s-%s] (day scope: %s / %s)",
                        newSlot.getStartTime(), newSlot.getEndTime(),
                        ex.getId(), ex.getStartTime(), ex.getEndTime(),
                        newSlot.getApplicableDay() == null ? "ALL" : newSlot.getApplicableDay(),
                        ex.getApplicableDay() == null ? "ALL" : ex.getApplicableDay()));
            }
        }
    }

    /**
     * Day-aware overlap eligibility (KD-41):
     * - NULL vs NULL -> can overlap (both apply to all days)
     * - NULL vs specific-day -> CANNOT overlap (day-specific is an override)
     * - Same day vs same day -> can overlap
     * - Different day vs different day -> CANNOT overlap
     */
    private boolean slotsCanOverlap(DayOfWeekEnum dayA, DayOfWeekEnum dayB) {
        if (dayA == null && dayB == null) {
            return true; // Both apply to all days — overlap is possible
        }
        if (dayA == null || dayB == null) {
            return false; // One is all-days, other is day-specific — override, not overlap
        }
        return dayA == dayB; // Same specific day — overlap is possible
    }

    /**
     * Time overlap check per FR-5.3:
     * Overlap if A.start < B.end AND B.start < A.end
     */
    private boolean timesOverlap(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private TimeSlotGrid findActiveGridOrThrow(Long id) {
        return gridRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid", id));
    }
}
