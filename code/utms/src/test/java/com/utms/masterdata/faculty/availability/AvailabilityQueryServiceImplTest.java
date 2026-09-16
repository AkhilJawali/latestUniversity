package com.utms.masterdata.faculty.availability;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.faculty.Faculty;
import com.utms.masterdata.faculty.FacultyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityQueryServiceImplTest {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private FacultyAvailabilityWindowRepository windowRepository;

    @Mock
    private FacultyPreferenceRepository preferenceRepository;

    @InjectMocks
    private AvailabilityQueryServiceImpl queryService;

    // --- isAvailable BLOCKED mode ---

    @Test
    void isAvailable_blockedMode_noOverlap_returnsTrue() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);
        faculty.setDesignation("Professor");

        FacultyAvailabilityWindow blockedWindow = new FacultyAvailabilityWindow();
        blockedWindow.setFaculty(faculty);
        blockedWindow.setStartTime(LocalTime.of(9, 0));
        blockedWindow.setEndTime(LocalTime.of(11, 0));

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(windowRepository.findByFacultyIdAndDayOfWeekAndDeletedAtIsNull(1L, "MONDAY"))
                .thenReturn(List.of(blockedWindow));

        // Query for 14:00-16:00 — no overlap with 09:00-11:00 blocked window
        boolean result = queryService.isAvailable(1L, "MONDAY", LocalTime.of(14, 0), LocalTime.of(16, 0));

        assertTrue(result);
    }

    @Test
    void isAvailable_blockedMode_overlap_returnsFalse() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);
        faculty.setDesignation("Associate Professor");

        FacultyAvailabilityWindow blockedWindow = new FacultyAvailabilityWindow();
        blockedWindow.setFaculty(faculty);
        blockedWindow.setStartTime(LocalTime.of(9, 0));
        blockedWindow.setEndTime(LocalTime.of(11, 0));

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(windowRepository.findByFacultyIdAndDayOfWeekAndDeletedAtIsNull(1L, "MONDAY"))
                .thenReturn(List.of(blockedWindow));

        // Query for 10:00-12:00 — overlaps with 09:00-11:00 blocked window
        boolean result = queryService.isAvailable(1L, "MONDAY", LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertFalse(result);
    }

    @Test
    void isAvailable_blockedMode_noWindows_returnsTrue() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);
        faculty.setDesignation("Lecturer");

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(windowRepository.findByFacultyIdAndDayOfWeekAndDeletedAtIsNull(1L, "WEDNESDAY"))
                .thenReturn(List.of());

        boolean result = queryService.isAvailable(1L, "WEDNESDAY", LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertTrue(result);
    }

    // --- isAvailable AVAILABLE mode ---

    @Test
    void isAvailable_availableMode_coveredByWindow_returnsTrue() {
        Faculty faculty = new Faculty();
        faculty.setId(2L);
        faculty.setDesignation("Visiting Faculty");

        FacultyAvailabilityWindow availableWindow = new FacultyAvailabilityWindow();
        availableWindow.setFaculty(faculty);
        availableWindow.setStartTime(LocalTime.of(9, 0));
        availableWindow.setEndTime(LocalTime.of(13, 0));

        when(facultyRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(faculty));
        when(windowRepository.findByFacultyIdAndDayOfWeekAndDeletedAtIsNull(2L, "TUESDAY"))
                .thenReturn(List.of(availableWindow));

        // Query for 10:00-12:00 — fully covered by 09:00-13:00 available window
        boolean result = queryService.isAvailable(2L, "TUESDAY", LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertTrue(result);
    }

    @Test
    void isAvailable_availableMode_notCovered_returnsFalse() {
        Faculty faculty = new Faculty();
        faculty.setId(2L);
        faculty.setDesignation("Visiting Faculty");

        FacultyAvailabilityWindow availableWindow = new FacultyAvailabilityWindow();
        availableWindow.setFaculty(faculty);
        availableWindow.setStartTime(LocalTime.of(9, 0));
        availableWindow.setEndTime(LocalTime.of(11, 0));

        when(facultyRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(faculty));
        when(windowRepository.findByFacultyIdAndDayOfWeekAndDeletedAtIsNull(2L, "TUESDAY"))
                .thenReturn(List.of(availableWindow));

        // Query for 10:00-13:00 — NOT fully covered by 09:00-11:00 available window
        boolean result = queryService.isAvailable(2L, "TUESDAY", LocalTime.of(10, 0), LocalTime.of(13, 0));

        assertFalse(result);
    }

    @Test
    void isAvailable_availableMode_noWindows_returnsFalse() {
        Faculty faculty = new Faculty();
        faculty.setId(2L);
        faculty.setDesignation("Visiting Faculty");

        when(facultyRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(faculty));
        when(windowRepository.findByFacultyIdAndDayOfWeekAndDeletedAtIsNull(2L, "FRIDAY"))
                .thenReturn(List.of());

        // No declared available windows — visiting faculty is NOT available
        boolean result = queryService.isAvailable(2L, "FRIDAY", LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertFalse(result);
    }

    // --- getMode ---

    @Test
    void getMode_regularFaculty_returnsBLOCKED() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);
        faculty.setDesignation("Professor");

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));

        assertEquals(AvailabilityMode.BLOCKED, queryService.getMode(1L));
    }

    @Test
    void getMode_visitingFaculty_returnsAVAILABLE() {
        Faculty faculty = new Faculty();
        faculty.setId(2L);
        faculty.setDesignation("Visiting Faculty");

        when(facultyRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(faculty));

        assertEquals(AvailabilityMode.AVAILABLE, queryService.getMode(2L));
    }

    @Test
    void getMode_facultyNotFound_throwsEntityNotFound() {
        when(facultyRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> queryService.getMode(999L));
    }

    // --- getSoftPreferences ---

    @Test
    void getSoftPreferences_preferencesExist_returnsThem() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);
        faculty.setDesignation("Professor");

        FacultyPreference pref = new FacultyPreference();
        pref.setFaculty(faculty);
        pref.setPreferredTimeOfDay("MORNING");
        pref.setSessionDistribution("CONSECUTIVE");

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(preferenceRepository.findByFacultyId(1L)).thenReturn(Optional.of(pref));

        FacultyPreferences result = queryService.getSoftPreferences(1L);

        assertEquals("MORNING", result.preferredTimeOfDay());
        assertEquals("CONSECUTIVE", result.sessionDistribution());
    }

    @Test
    void getSoftPreferences_noPreferences_returnsNoPreference() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);
        faculty.setDesignation("Professor");

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(preferenceRepository.findByFacultyId(1L)).thenReturn(Optional.empty());

        FacultyPreferences result = queryService.getSoftPreferences(1L);

        assertEquals(FacultyPreferences.NO_PREFERENCE, result);
        assertEquals("NO_PREFERENCE", result.preferredTimeOfDay());
        assertEquals("NO_PREFERENCE", result.sessionDistribution());
    }
}
