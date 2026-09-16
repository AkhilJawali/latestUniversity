package com.utms.masterdata.room;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Query service for determining room availability status (free/occupied/blocked)
 * for a given campus, day, and time window.
 */
public interface RoomAvailabilityQueryService {

    /**
     * Returns availability status for all active rooms in a campus during a specified time window.
     *
     * @param campusId  the campus to query
     * @param day       the day of week
     * @param startTime start of the time window
     * @param endTime   end of the time window
     * @return list of room availability statuses
     */
    List<RoomAvailabilityStatus> getAvailability(Long campusId, DayOfWeek day, LocalTime startTime, LocalTime endTime);
}
