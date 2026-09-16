package com.utms.masterdata.room;

import java.util.List;

public record RoomEquipmentChangedEvent(
        Long roomId,
        List<String> removedTags
) {
}
