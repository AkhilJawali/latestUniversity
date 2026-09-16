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
@RequestMapping("/api/v1/scheduling-config/soft-constraint-weights")
@RequiredArgsConstructor
@Tag(name = "Scheduling Config — Soft-Constraint Weights",
        description = "CRUD for soft-constraint weights (per campus, per constraint type)")
public class SoftConstraintWeightController {

    private final SoftConstraintWeightService service;

    @PostMapping
    @Operation(summary = "Create a soft-constraint weight")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateSoftConstraintWeightRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", service.create(request)));
    }

    @GetMapping
    @Operation(summary = "List soft-constraint weights (paged, filtered by campus)")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long campusId,
            Pageable pageable) {
        Page<SoftConstraintWeightDto> page = service.findAll(campusId, pageable);
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
    @Operation(summary = "Get a soft-constraint weight by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("data", service.findById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a soft-constraint weight")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateSoftConstraintWeightRequest request) {
        return ResponseEntity.ok(Map.of("data", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a soft-constraint weight")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
