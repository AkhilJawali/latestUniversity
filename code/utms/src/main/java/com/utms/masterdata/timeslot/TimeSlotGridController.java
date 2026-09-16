package com.utms.masterdata.timeslot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/time-slots")
@RequiredArgsConstructor
@Tag(name = "Time Slot Grids", description = "Time-slot grid configuration per campus")
public class TimeSlotGridController {

    private final TimeSlotGridService service;

    @PostMapping
    @Operation(summary = "Create a time-slot grid for a campus")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateTimeSlotGridRequest request) {
        TimeSlotGridDto grid = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", grid));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get time-slot grid by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        TimeSlotGridDto grid = service.findById(id);
        return ResponseEntity.ok(Map.of("data", grid));
    }

    @GetMapping("/campus/{campusId}")
    @Operation(summary = "Get time-slot grid by campus ID")
    public ResponseEntity<Map<String, Object>> getByCampusId(@PathVariable Long campusId) {
        TimeSlotGridDto grid = service.findByCampusId(campusId);
        return ResponseEntity.ok(Map.of("data", grid));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update grid name")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateTimeSlotGridRequest request) {
        TimeSlotGridDto grid = service.updateGridName(id, request);
        return ResponseEntity.ok(Map.of("data", grid));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a time-slot grid and all its slots")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteGrid(id);
        return ResponseEntity.noContent().build();
    }

    // --- Slot management endpoints ---

    @PostMapping("/{gridId}/slots")
    @Operation(summary = "Add a slot definition to an existing grid")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> addSlot(@PathVariable Long gridId,
                                                        @Valid @RequestBody CreateSlotDefinitionRequest request) {
        SlotDefinitionDto slot = service.addSlot(gridId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", slot));
    }

    @DeleteMapping("/{gridId}/slots/{slotId}")
    @Operation(summary = "Soft-delete a slot definition from a grid")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Void> removeSlot(@PathVariable Long gridId, @PathVariable Long slotId) {
        service.removeSlot(gridId, slotId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{gridId}/effective")
    @Operation(summary = "Get effective slots for a specific day (merges day-overrides with all-day slots)")
    public ResponseEntity<Map<String, Object>> getEffectiveSlotsForDay(
            @PathVariable Long gridId,
            @RequestParam DayOfWeekEnum day) {
        List<SlotDefinitionDto> slots = service.getEffectiveSlotsForDay(gridId, day);
        return ResponseEntity.ok(Map.of("data", slots));
    }
}
