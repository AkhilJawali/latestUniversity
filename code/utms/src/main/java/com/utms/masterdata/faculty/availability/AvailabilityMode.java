package com.utms.masterdata.faculty.availability;

/**
 * Determines how a faculty member's availability windows are interpreted.
 * <p>
 * BLOCKED: Regular faculty "declare blocked" — windows represent times they are NOT available.
 * AVAILABLE: Visiting/Adjunct faculty "declare available" — windows represent times they ARE available.
 */
public enum AvailabilityMode {
    BLOCKED,
    AVAILABLE
}
