package com.utms.scheduling.engine.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Room data loaded for scheduling (from RoomService).
 */
@Getter
@Builder
public class RoomInfo {
    private final Long roomId;
    private final String roomName;
    private final String building;
    private final String floor;
    private final int capacity;
    private final List<String> equipmentTags;
}
