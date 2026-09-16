package com.utms.masterdata.batch;

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
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
@Tag(name = "Batches", description = "Batch master data management")
public class BatchController {

    private final BatchService batchService;

    @PostMapping
    @Operation(summary = "Create a new batch")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateBatchRequest request) {
        BatchDto batch = batchService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", batch));
    }

    @GetMapping
    @Operation(summary = "List all batches with optional program filter and pagination")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long programId,
            Pageable pageable) {
        Page<BatchDto> page = batchService.findAll(programId, pageable);
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
    @Operation(summary = "Get batch by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        BatchDto batch = batchService.findById(id);
        return ResponseEntity.ok(Map.of("data", batch));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update batch details")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateBatchRequest request) {
        BatchDto batch = batchService.update(id, request);
        return ResponseEntity.ok(Map.of("data", batch));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a batch")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        batchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
