package com.utms.masterdata.department;

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
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Department master data management")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @Operation(summary = "Create a new department")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentDto department = departmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", department));
    }

    @GetMapping
    @Operation(summary = "List all departments with optional campus filter and pagination")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long campusId,
            Pageable pageable) {
        Page<DepartmentDto> page = departmentService.findAll(campusId, pageable);
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
    @Operation(summary = "Get department by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        DepartmentDto department = departmentService.findById(id);
        return ResponseEntity.ok(Map.of("data", department));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update department name")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateDepartmentRequest request) {
        DepartmentDto department = departmentService.update(id, request);
        return ResponseEntity.ok(Map.of("data", department));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a department")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
