package com.utms.masterdata.section;

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
@RequiredArgsConstructor
@Tag(name = "Sections", description = "Section master data management")
public class SectionController {

    private final SectionService sectionService;

    @PostMapping("/api/v1/batches/{batchId}/sections")
    @Operation(summary = "Create a new section within a batch")
    public ResponseEntity<Map<String, Object>> create(
            @PathVariable Long batchId,
            @Valid @RequestBody CreateSectionRequest request) {
        SectionDto section = sectionService.create(batchId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", section));
    }

    @GetMapping("/api/v1/batches/{batchId}/sections")
    @Operation(summary = "List all sections for a batch")
    public ResponseEntity<Map<String, Object>> listByBatch(@PathVariable Long batchId) {
        List<SectionDto> sections = sectionService.findAllByBatchId(batchId);
        return ResponseEntity.ok(Map.of("data", sections));
    }

    @GetMapping("/api/v1/sections/{id}")
    @Operation(summary = "Get section by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        SectionDto section = sectionService.findById(id);
        return ResponseEntity.ok(Map.of("data", section));
    }

    @PutMapping("/api/v1/sections/{id}")
    @Operation(summary = "Update section details")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateSectionRequest request) {
        SectionDto section = sectionService.update(id, request);
        return ResponseEntity.ok(Map.of("data", section));
    }

    @DeleteMapping("/api/v1/sections/{id}")
    @Operation(summary = "Soft-delete a section")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
