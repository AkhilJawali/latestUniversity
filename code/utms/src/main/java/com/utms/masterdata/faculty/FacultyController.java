package com.utms.masterdata.faculty;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/faculty")
@RequiredArgsConstructor
@Validated
@Tag(name = "Faculty", description = "Faculty profile management")
public class FacultyController {

    private final FacultyService facultyService;
    private final FacultyCompetencyService facultyCompetencyService;
    private final FacultyCampusAssociationService facultyCampusAssociationService;

    // --- Faculty CRUD ---

    @PostMapping
    @Operation(summary = "Create a new faculty member")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateFacultyRequest request) {
        FacultyDto faculty = facultyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", faculty));
    }

    @GetMapping
    @Operation(summary = "List all faculty with pagination and filtering")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) Long competencyCourseId,
            Pageable pageable) {
        Page<FacultyDto> page = facultyService.findAll(departmentId, campusId, designation, competencyCourseId, pageable);
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
    @Operation(summary = "Get faculty by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        FacultyDto faculty = facultyService.findById(id);
        return ResponseEntity.ok(Map.of("data", faculty));
    }

    @GetMapping("/by-identifier/{identifier}")
    @Operation(summary = "Get faculty by identifier")
    public ResponseEntity<Map<String, Object>> getByIdentifier(@PathVariable String identifier) {
        FacultyDto faculty = facultyService.findByIdentifier(identifier);
        return ResponseEntity.ok(Map.of("data", faculty));
    }

    @GetMapping("/by-competency/{courseId}")
    @Operation(summary = "Get faculty members with competency for a specific course")
    public ResponseEntity<Map<String, Object>> getByCompetency(
            @PathVariable Long courseId, Pageable pageable) {
        Page<FacultyDto> page = facultyService.findAll(null, null, null, courseId, pageable);
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

    @PutMapping("/{id}")
    @Operation(summary = "Update faculty (name, designation, qualification, homeDepartment, loads)")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateFacultyRequest request) {
        FacultyDto faculty = facultyService.update(id, request);
        return ResponseEntity.ok(Map.of("data", faculty));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a faculty member")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        facultyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Competencies sub-resource ---

    @GetMapping("/{id}/competencies")
    @Operation(summary = "List competency course IDs for a faculty member")
    public ResponseEntity<Map<String, Object>> getCompetencies(@PathVariable Long id) {
        List<Long> courseIds = facultyCompetencyService.getCompetencyCourseIds(id);
        return ResponseEntity.ok(Map.of("data", courseIds));
    }

    @PostMapping("/{id}/competencies")
    @Operation(summary = "Add competencies to a faculty member (bulk, idempotent)")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Void> addCompetencies(@PathVariable Long id,
                                                 @RequestBody @NotEmpty @Size(max = 50) List<Long> courseIds) {
        facultyCompetencyService.addCompetencies(id, courseIds);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/competencies/{courseId}")
    @Operation(summary = "Remove a competency from a faculty member")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Void> removeCompetency(@PathVariable Long id,
                                                  @PathVariable Long courseId) {
        facultyCompetencyService.removeCompetency(id, courseId);
        return ResponseEntity.noContent().build();
    }

    // --- Campus associations sub-resource ---

    @GetMapping("/{id}/campuses")
    @Operation(summary = "List a faculty member's active campus associations")
    public ResponseEntity<Map<String, Object>> getCampusAssociations(@PathVariable Long id) {
        List<FacultyCampusDto> campuses = facultyCampusAssociationService.getAssociationDtos(id);
        return ResponseEntity.ok(Map.of("data", campuses));
    }

    @PostMapping("/{id}/campuses/{campusId}")
    @Operation(summary = "Add a campus association to a faculty member")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Void> addCampusAssociation(@PathVariable Long id,
                                                      @PathVariable Long campusId) {
        facultyCampusAssociationService.addAssociation(id, campusId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/campuses/{campusId}")
    @Operation(summary = "Remove a campus association from a faculty member")
    // TODO: @PreAuthorize("hasAnyRole('ADMIN', 'HOD')") — enable when Auth module is built
    public ResponseEntity<Void> removeCampusAssociation(@PathVariable Long id,
                                                         @PathVariable Long campusId) {
        facultyCampusAssociationService.removeAssociation(id, campusId);
        return ResponseEntity.noContent().build();
    }
}
