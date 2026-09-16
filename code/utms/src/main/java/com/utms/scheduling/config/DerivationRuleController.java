package com.utms.scheduling.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/scheduling-config/derivation-rules")
@RequiredArgsConstructor
@Tag(name = "Scheduling Config — Derivation Rules",
        description = "CRUD for session-derivation rules (per campus, per component type)")
public class DerivationRuleController {

    private final DerivationRuleService service;

    @PostMapping
    @Operation(summary = "Create a session-derivation rule")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateDerivationRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", service.create(request)));
    }

    @GetMapping
    @Operation(summary = "List session-derivation rules (paged, filtered by campus)")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long campusId,
            Pageable pageable) {
        Page<DerivationRuleDto> page = service.findAll(campusId, pageable);
        return ResponseEntity.ok(Map.of(
                "data", page.getContent(),
                "meta", Map.of(
                        "page", page.getNumber(),
                        "size", page.getSize(),
                        "totalElements", page.getTotalElements(),
                        "totalPages", page.getTotalPages()
                )
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a session-derivation rule by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("data", service.findById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a session-derivation rule")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateDerivationRuleRequest request) {
        return ResponseEntity.ok(Map.of("data", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a session-derivation rule")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
