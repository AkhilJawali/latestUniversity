package com.utms.masterdata.room;

public record RoomAvailabilityStatus(
        Long roomId,
        String name,
        String code,
        RoomStatus status
) {
}
