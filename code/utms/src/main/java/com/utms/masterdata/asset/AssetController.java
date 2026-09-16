package com.utms.masterdata.asset;

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
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
@Tag(name = "Assets", description = "Schedulable asset master data management")
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    @Operation(summary = "Create a new schedulable asset")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateAssetRequest request) {
        AssetDto asset = assetService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", asset));
    }

    @GetMapping
    @Operation(summary = "List assets with filtering and pagination")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String assetType,
            Pageable pageable) {
        Page<AssetDto> page = assetService.findAll(campusId, departmentId, assetType, pageable);
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
    @Operation(summary = "Get asset by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        AssetDto asset = assetService.findById(id);
        return ResponseEntity.ok(Map.of("data", asset));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update asset details and availability windows")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateAssetRequest request) {
        AssetDto asset = assetService.update(id, request);
        return ResponseEntity.ok(Map.of("data", asset));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a schedulable asset")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
