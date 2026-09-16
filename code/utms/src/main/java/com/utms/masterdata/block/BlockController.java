package com.utms.masterdata.block;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/resource-blocks")
@RequiredArgsConstructor
@Tag(name = "Resource Blocks", description = "Resource blocking and availability workflow")
public class BlockController {

    private final BlockService blockService;
    private final BlockReportService blockReportService;

    @PostMapping
    @Operation(summary = "Raise a new resource block")
    public ResponseEntity<Map<String, Object>> raiseBlock(@Valid @RequestBody CreateBlockRequest request) {
        ResourceBlockDto block = blockService.raiseBlock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", block));
    }

    @GetMapping
    @Operation(summary = "List resource blocks with filtering and pagination")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<ResourceBlockDto> page = blockService.findAll(resourceType, resourceId, status, pageable);
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
    @Operation(summary = "Get resource block by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        ResourceBlockDto block = blockService.findById(id);
        return ResponseEntity.ok(Map.of("data", block));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending resource block")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id,
                                                        @Valid @RequestBody(required = false) BlockApprovalRequest request) {
        ResourceBlockDto block = blockService.approveBlock(id, request);
        return ResponseEntity.ok(Map.of("data", block));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending resource block")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id,
                                                       @Valid @RequestBody BlockRejectRequest request) {
        ResourceBlockDto block = blockService.rejectBlock(id, request);
        return ResponseEntity.ok(Map.of("data", block));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw a pending resource block")
    public ResponseEntity<Map<String, Object>> withdraw(@PathVariable Long id) {
        ResourceBlockDto block = blockService.withdrawBlock(id);
        return ResponseEntity.ok(Map.of("data", block));
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release an active resource block")
    public ResponseEntity<Map<String, Object>> release(@PathVariable Long id) {
        ResourceBlockDto block = blockService.releaseBlock(id);
        return ResponseEntity.ok(Map.of("data", block));
    }

    @PostMapping("/{id}/record-override")
    @Operation(summary = "Record a soft-block override with justification (KD-33)")
    public ResponseEntity<Void> recordOverride(
            @PathVariable Long id,
            @RequestParam @NotBlank @Size(max = 500) String justification,
            @RequestParam(required = false) Long sessionId) {
        blockService.recordSoftBlockOverride(id, justification, sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports")
    @Operation(summary = "Generate boundary-aware block report for a resource (KD-37)")
    public ResponseEntity<Map<String, Object>> report(
            @RequestParam String resourceType,
            @RequestParam Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> report = blockReportService.generateReport(
                resourceType, resourceId, startDate, endDate);
        return ResponseEntity.ok(Map.of("data", report));
    }
}
