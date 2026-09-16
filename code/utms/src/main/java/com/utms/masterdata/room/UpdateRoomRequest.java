package com.utms.masterdata.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class UpdateRoomRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

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
