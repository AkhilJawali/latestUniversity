package com.utms.masterdata.hierarchy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Hierarchy Tree", description = "Institution hierarchy tree views")
public class HierarchyTreeController {

    private final HierarchyTreeService hierarchyTreeService;

    @GetMapping("/hierarchy/tree")
    @Operation(summary = "Get full institution hierarchy tree")
    public ResponseEntity<Map<String, Object>> getFullTree() {
        List<CampusTreeDto> tree = hierarchyTreeService.getFullTree();
        return ResponseEntity.ok(Map.of("data", tree));
    }

    @GetMapping("/campuses/{id}/tree")
    @Operation(summary = "Get hierarchy tree for a specific campus")
    public ResponseEntity<Map<String, Object>> getCampusTree(@PathVariable Long id) {
        CampusTreeDto tree = hierarchyTreeService.getCampusTree(id);
        return ResponseEntity.ok(Map.of("data", tree));
    }

    @GetMapping("/departments/{id}/tree")
    @Operation(summary = "Get hierarchy tree under a department")
    public ResponseEntity<Map<String, Object>> getDepartmentTree(@PathVariable Long id) {
        DepartmentTreeDto tree = hierarchyTreeService.getDepartmentTree(id);
        return ResponseEntity.ok(Map.of("data", tree));
    }

    @GetMapping("/programs/{id}/tree")
    @Operation(summary = "Get hierarchy tree under a program")
    public ResponseEntity<Map<String, Object>> getProgramTree(@PathVariable Long id) {
        ProgramTreeDto tree = hierarchyTreeService.getProgramTree(id);
        return ResponseEntity.ok(Map.of("data", tree));
    }

    @GetMapping("/batches/{id}/tree")
    @Operation(summary = "Get hierarchy tree under a batch (sections)")
    public ResponseEntity<Map<String, Object>> getBatchTree(@PathVariable Long id) {
        BatchTreeDto tree = hierarchyTreeService.getBatchTree(id);
        return ResponseEntity.ok(Map.of("data", tree));
    }
}
