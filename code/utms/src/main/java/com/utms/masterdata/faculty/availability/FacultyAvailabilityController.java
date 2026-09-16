package com.utms.masterdata.faculty.availability;

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
@RequestMapping("/api/v1/faculty/{facultyId}")
@RequiredArgsConstructor
@Tag(name = "Faculty Availability", description = "Faculty availability windows and preference management")
public class FacultyAvailabilityController {

    private final FacultyAvailabilityService availabilityService;
    private final FacultyPreferenceService preferenceService;

    // --- Availability Windows ---

    @PostMapping("/availability")
    @Operation(summary = "Create a new availability window for a faculty member")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> createWindow(
            @PathVariable Long facultyId,
            @Valid @RequestBody CreateAvailabilityWindowRequest request) {
        FacultyAvailabilityWindowDto window = availabilityService.createWindow(facultyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", window));
    }

    @GetMapping("/availability")
    @Operation(summary = "List all active availability windows for a faculty member")
    public ResponseEntity<Map<String, Object>> listWindows(@PathVariable Long facultyId) {
        List<FacultyAvailabilityWindowDto> windows = availabilityService.listByFacultyId(facultyId);
        return ResponseEntity.ok(Map.of("data", windows));
    }

    @PutMapping("/availability/{windowId}")
    @Operation(summary = "Update an availability window")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> updateWindow(
            @PathVariable Long facultyId,
            @PathVariable Long windowId,
            @Valid @RequestBody UpdateAvailabilityWindowRequest request) {
        FacultyAvailabilityWindowDto window = availabilityService.updateWindow(facultyId, windowId, request);
        return ResponseEntity.ok(Map.of("data", window));
    }

    @DeleteMapping("/availability/{windowId}")
    @Operation(summary = "Soft-delete an availability window")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')") — enable when Auth module is built
    public ResponseEntity<Void> deleteWindow(
            @PathVariable Long facultyId,
            @PathVariable Long windowId) {
        availabilityService.deleteWindow(facultyId, windowId);
        return ResponseEntity.noContent().build();
    }

    // --- Preferences ---

    @GetMapping("/preferences")
    @Operation(summary = "Get faculty scheduling preferences")
    public ResponseEntity<Map<String, Object>> getPreferences(@PathVariable Long facultyId) {
        FacultyPreferenceDto preferences = preferenceService.getPreferences(facultyId);
        return ResponseEntity.ok(Map.of("data", preferences));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Set faculty scheduling preferences (upsert)")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> setPreferences(
            @PathVariable Long facultyId,
            @Valid @RequestBody SetPreferenceRequest request) {
        FacultyPreferenceDto preferences = preferenceService.setPreferences(facultyId, request);
        return ResponseEntity.ok(Map.of("data", preferences));
    }
}
