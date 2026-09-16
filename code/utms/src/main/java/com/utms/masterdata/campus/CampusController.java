package com.utms.masterdata.campus;

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
@RequestMapping("/api/v1/campuses")
@RequiredArgsConstructor
@Tag(name = "Campuses", description = "Campus master data management")
public class CampusController {

    private final CampusService campusService;

    @PostMapping
    @Operation(summary = "Create a new campus")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateCampusRequest request) {
        CampusDto campus = campusService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", campus));
    }

    @GetMapping
    @Operation(summary = "List all campuses with pagination")
    public ResponseEntity<Map<String, Object>> list(Pageable pageable) {
        Page<CampusDto> page = campusService.findAll(pageable);
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
    @Operation(summary = "Get campus by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        CampusDto campus = campusService.findById(id);
        return ResponseEntity.ok(Map.of("data", campus));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get campus by code")
    public ResponseEntity<Map<String, Object>> getByCode(@PathVariable String code) {
        CampusDto campus = campusService.findByCode(code);
        return ResponseEntity.ok(Map.of("data", campus));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update campus name and location")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateCampusRequest request) {
        CampusDto campus = campusService.update(id, request);
        return ResponseEntity.ok(Map.of("data", campus));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a campus")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        campusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
