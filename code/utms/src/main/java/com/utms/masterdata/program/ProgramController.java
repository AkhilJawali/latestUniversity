package com.utms.masterdata.program;

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
@RequestMapping("/api/v1/programs")
@RequiredArgsConstructor
@Tag(name = "Programs", description = "Program master data management")
public class ProgramController {

    private final ProgramService programService;

    @PostMapping
    @Operation(summary = "Create a new program")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateProgramRequest request) {
        ProgramDto program = programService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", program));
    }

    @GetMapping
    @Operation(summary = "List all programs with optional department filter and pagination")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long departmentId,
            Pageable pageable) {
        Page<ProgramDto> page = programService.findAll(departmentId, pageable);
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
    @Operation(summary = "Get program by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        ProgramDto program = programService.findById(id);
        return ResponseEntity.ok(Map.of("data", program));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update program details")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateProgramRequest request) {
        ProgramDto program = programService.update(id, request);
        return ResponseEntity.ok(Map.of("data", program));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a program")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        programService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
