package com.utms.masterdata.room;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CreateRoomRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Code must be alphanumeric (hyphens and underscores allowed)")
    private String code;

    @NotNull(message = "Campus ID is required")
    private Long campusId;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @NotNull(message = "Room type is required")
    private RoomType roomType;

    private List<String> equipmentTags;

    @Size(max = 100, message = "Building must not exceed 100 characters")
    private String building;

    @Size(max = 20, message = "Floor must not exceed 20 characters")
    private String floor;
}
