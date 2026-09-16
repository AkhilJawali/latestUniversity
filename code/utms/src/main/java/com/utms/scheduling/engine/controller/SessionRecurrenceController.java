package com.utms.scheduling.engine.controller;

import com.utms.scheduling.engine.dto.OccurrenceDatesDto;
import com.utms.scheduling.engine.dto.SessionRecurrenceDto;
import com.utms.scheduling.engine.dto.UpdateRecurrenceRequest;
import com.utms.scheduling.engine.service.SessionRecurrenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for a session's recurrence pattern (A4-13, design section 3).
 *
 * <p>PUT /recurrence sets/changes the pattern, DELETE /recurrence reverts to weekly,
 * GET /occurrences lists the actual occurrence dates for feed/display. Department
 * scoping (COORDINATOR own-dept / HOD / REGISTRAR) will be enforced via RBAC when the
 * auth module is wired; the actor is currently "system", consistent with
 * {@code SchedulingController}.</p>
 */
@RestController
@RequestMapping("/api/v1/timetables/sessions/{sessionId}")
@RequiredArgsConstructor
@Tag(name = "Session Recurrence", description = "Fortnightly / alternate-week recurrence patterns for scheduled sessions (A4-13)")
public class SessionRecurrenceController {

    private final SessionRecurrenceService recurrenceService;

    // TODO: add @PreAuthorize("hasAnyRole('COORDINATOR','HOD','REGISTRAR')") + own-dept
    //       check when the auth/RBAC module is available (same status as SchedulingController).
    //       Tracked as a blocking follow-up gated on the auth module (see A4-13 code-review #1).

    @PutMapping("/recurrence")
    @Operation(summary = "Set or change a session's recurrence pattern",
            description = "Sets the recurrence to WEEKLY or FORTNIGHTLY. For FORTNIGHTLY a weekGroup "
                    + "(WEEK_A/WEEK_B) is required; for WEEKLY it must be omitted. Day and slot are unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pattern updated"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "422", description = "Invalid recurrence type / week-group combination")
    })
    public ResponseEntity<SessionRecurrenceDto> setRecurrence(
            @Parameter(description = "Scheduled session id") @PathVariable Long sessionId,
            @Valid @RequestBody UpdateRecurrenceRequest request) {
        SessionRecurrenceDto dto = recurrenceService.setRecurrence(
                sessionId, request.getRecurrenceType(), request.getWeekGroup(), "system");
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/recurrence")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revert a session to weekly recurrence",
            description = "Clears any fortnightly pattern, returning the session to the default weekly recurrence.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reverted to weekly"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<Void> revertToWeekly(
            @Parameter(description = "Scheduled session id") @PathVariable Long sessionId) {
        recurrenceService.revertToWeekly(sessionId, "system");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/occurrences")
    @Operation(summary = "List the dates a session actually occurs",
            description = "Returns occurrence dates within the semester. Fortnightly sessions are filtered "
                    + "to their week group; holidays/non-working days are excluded. Used by calendar feed and display.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Occurrence dates"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "422", description = "No academic calendar anchor for the session's campus/semester")
    })
    public ResponseEntity<OccurrenceDatesDto> getOccurrences(
            @Parameter(description = "Scheduled session id") @PathVariable Long sessionId) {
        return ResponseEntity.ok(recurrenceService.getOccurrenceDates(sessionId));
    }
}
