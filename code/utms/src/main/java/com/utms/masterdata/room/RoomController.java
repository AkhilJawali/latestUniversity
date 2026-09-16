package com.utms.masterdata.room;

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
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room and lab master data management")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @Operation(summary = "Create a new room")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateRoomRequest request) {
        RoomDto room = roomService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", room));
    }

    @GetMapping
    @Operation(summary = "List rooms with filtering and pagination")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String equipmentTag,
            Pageable pageable) {
        Page<RoomDto> page = roomService.findAll(campusId, roomType, building, minCapacity, equipmentTag, pageable);
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
    @Operation(summary = "Get room by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        RoomDto room = roomService.findById(id);
        return ResponseEntity.ok(Map.of("data", room));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update room details")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateRoomRequest request) {
        RoomDto room = roomService.update(id, request);
        return ResponseEntity.ok(Map.of("data", room));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a room")
    // TODO: @PreAuthorize("hasRole('ADMIN')") — enable when Auth module is built
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
