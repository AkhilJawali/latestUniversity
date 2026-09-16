package com.utms.masterdata.faculty.availability;

/**
 * Read-only representation of a faculty member's soft scheduling preferences.
 */
public record FacultyPreferences(String preferredTimeOfDay, String sessionDistribution) {

    public static final FacultyPreferences NO_PREFERENCE =
            new FacultyPreferences("NO_PREFERENCE", "NO_PREFERENCE");
}
