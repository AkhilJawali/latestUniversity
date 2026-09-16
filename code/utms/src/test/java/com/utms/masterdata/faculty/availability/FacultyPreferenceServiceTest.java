package com.utms.masterdata.faculty.availability;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.faculty.Faculty;
import com.utms.masterdata.faculty.FacultyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultyPreferenceServiceTest {

    @Mock
    private FacultyPreferenceRepository preferenceRepository;

    @Mock
    private FacultyRepository facultyRepository;

    @InjectMocks
    private FacultyPreferenceService preferenceService;

    // --- setPreferences ---

    @Test
    void setPreferences_newPreference_createsAndReturnsDto() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);

        SetPreferenceRequest request = SetPreferenceRequest.builder()
                .preferredTimeOfDay("MORNING")
                .sessionDistribution("CONSECUTIVE")
                .build();

        FacultyPreference savedPref = new FacultyPreference();
        savedPref.setId(5L);
        savedPref.setFaculty(faculty);
        savedPref.setPreferredTimeOfDay("MORNING");
        savedPref.setSessionDistribution("CONSECUTIVE");
        savedPref.setIsActive(true);

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(preferenceRepository.findByFacultyId(1L)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(FacultyPreference.class))).thenReturn(savedPref);

        FacultyPreferenceDto result = preferenceService.setPreferences(1L, request);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals(1L, result.getFacultyId());
        assertEquals("MORNING", result.getPreferredTimeOfDay());
        assertEquals("CONSECUTIVE", result.getSessionDistribution());
        verify(preferenceRepository).save(any(FacultyPreference.class));
    }

    @Test
    void setPreferences_existingPreference_updatesAndReturnsDto() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);

        FacultyPreference existingPref = new FacultyPreference();
        existingPref.setId(5L);
        existingPref.setFaculty(faculty);
        existingPref.setPreferredTimeOfDay("MORNING");
        existingPref.setSessionDistribution("CONSECUTIVE");
        existingPref.setIsActive(true);

        SetPreferenceRequest request = SetPreferenceRequest.builder()
                .preferredTimeOfDay("AFTERNOON")
                .sessionDistribution("SPREAD")
                .build();

        FacultyPreference updatedPref = new FacultyPreference();
        updatedPref.setId(5L);
        updatedPref.setFaculty(faculty);
        updatedPref.setPreferredTimeOfDay("AFTERNOON");
        updatedPref.setSessionDistribution("SPREAD");
        updatedPref.setIsActive(true);

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(preferenceRepository.findByFacultyId(1L)).thenReturn(Optional.of(existingPref));
        when(preferenceRepository.save(any(FacultyPreference.class))).thenReturn(updatedPref);

        FacultyPreferenceDto result = preferenceService.setPreferences(1L, request);

        assertEquals("AFTERNOON", result.getPreferredTimeOfDay());
        assertEquals("SPREAD", result.getSessionDistribution());
        verify(preferenceRepository).save(existingPref);
    }

    @Test
    void setPreferences_invalidTimeOfDay_throwsBusinessRuleViolation() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);

        SetPreferenceRequest request = SetPreferenceRequest.builder()
                .preferredTimeOfDay("EVENING")
                .sessionDistribution("CONSECUTIVE")
                .build();

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));

        assertThrows(BusinessRuleViolationException.class,
                () -> preferenceService.setPreferences(1L, request));

        verify(preferenceRepository, never()).save(any());
    }

    // --- getPreferences ---

    @Test
    void getPreferences_existingPreference_returnsDto() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);

        FacultyPreference pref = new FacultyPreference();
        pref.setId(5L);
        pref.setFaculty(faculty);
        pref.setPreferredTimeOfDay("MORNING");
        pref.setSessionDistribution("SPREAD");
        pref.setIsActive(true);

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(preferenceRepository.findByFacultyId(1L)).thenReturn(Optional.of(pref));

        FacultyPreferenceDto result = preferenceService.getPreferences(1L);

        assertEquals("MORNING", result.getPreferredTimeOfDay());
        assertEquals("SPREAD", result.getSessionDistribution());
    }

    @Test
    void getPreferences_noPreferenceSet_returnsDefault() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(preferenceRepository.findByFacultyId(1L)).thenReturn(Optional.empty());

        FacultyPreferenceDto result = preferenceService.getPreferences(1L);

        assertNotNull(result);
        assertEquals(1L, result.getFacultyId());
        assertEquals("NO_PREFERENCE", result.getPreferredTimeOfDay());
        assertEquals("NO_PREFERENCE", result.getSessionDistribution());
        assertTrue(result.getIsActive());
    }

    @Test
    void getPreferences_facultyNotFound_throwsEntityNotFound() {
        when(facultyRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> preferenceService.getPreferences(999L));
    }
}
