package com.utms.masterdata.faculty.availability;

import java.time.LocalTime;

/**
 * Represents a time range within a day.
 */
public record TimeRange(LocalTime start, LocalTime end) {

    public boolean overlaps(TimeRange other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }
}
