package com.utms.masterdata.room;

public record RoomCapacityChangedEvent(
        Long roomId,
        Integer oldCapacity,
        Integer newCapacity
) {
}
