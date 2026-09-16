package com.utms.masterdata.course;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course master data management")
public class CourseController {

    private final CourseService courseService;
    private final CoursePrerequisiteService coursePrerequisiteService;
    private final CourseCrossListingService courseCrossListingService;

    // --- Course CRUD ---

    @PostMapping
    @Operation(summary = "Create a new course")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateCourseRequest request) {
        CourseDto course = courseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", course));
    }

    @GetMapping
    @Operation(summary = "List all courses with pagination")
    public ResponseEntity<Map<String, Object>> list(Pageable pageable) {
        Page<CourseDto> page = courseService.findAll(pageable);
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
    @Operation(summary = "Get course by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        CourseDto course = courseService.findById(id);
        return ResponseEntity.ok(Map.of("data", course));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update course (name, LTP, credits, type, equipmentTags)")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateCourseRequest request) {
        CourseDto course = courseService.update(id, request);
        return ResponseEntity.ok(Map.of("data", course));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a course")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Prerequisites sub-resource ---

    @GetMapping("/{id}/prerequisites")
    @Operation(summary = "List prerequisite course IDs for a course")
    public ResponseEntity<Map<String, Object>> getPrerequisites(@PathVariable Long id) {
        List<Long> prerequisiteIds = coursePrerequisiteService.getPrerequisiteIds(id);
        return ResponseEntity.ok(Map.of("data", prerequisiteIds));
    }

    @PostMapping("/{id}/prerequisites/{prerequisiteId}")
    @Operation(summary = "Add a prerequisite to a course")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Void> addPrerequisite(@PathVariable Long id,
                                                 @PathVariable Long prerequisiteId) {
        coursePrerequisiteService.addPrerequisite(id, prerequisiteId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/prerequisites/{prerequisiteId}")
    @Operation(summary = "Remove a prerequisite from a course")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Void> removePrerequisite(@PathVariable Long id,
                                                    @PathVariable Long prerequisiteId) {
        coursePrerequisiteService.removePrerequisite(id, prerequisiteId);
        return ResponseEntity.noContent().build();
    }

    // --- Cross-listings sub-resource ---

    @PostMapping("/{id}/cross-listings/{departmentId}")
    @Operation(summary = "Add a cross-listing to another department")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Void> addCrossListing(@PathVariable Long id,
                                                 @PathVariable Long departmentId) {
        courseCrossListingService.addCrossListing(id, departmentId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/cross-listings/{departmentId}")
    @Operation(summary = "Remove a cross-listing from a department")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Void> removeCrossListing(@PathVariable Long id,
                                                    @PathVariable Long departmentId) {
        courseCrossListingService.removeCrossListing(id, departmentId);
        return ResponseEntity.noContent().build();
    }
}
