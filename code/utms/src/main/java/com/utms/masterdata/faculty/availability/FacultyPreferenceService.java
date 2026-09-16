package com.utms.masterdata.faculty.availability;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.faculty.Faculty;
import com.utms.masterdata.faculty.FacultyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacultyPreferenceService {

    private static final List<String> VALID_TIME_OF_DAY = List.of("MORNING", "AFTERNOON", "NO_PREFERENCE");
    private static final List<String> VALID_DISTRIBUTION = List.of("CONSECUTIVE", "SPREAD", "NO_PREFERENCE");

    private final FacultyPreferenceRepository preferenceRepository;
    private final FacultyRepository facultyRepository;

    @Transactional
    public FacultyPreferenceDto setPreferences(Long facultyId, SetPreferenceRequest request) {
        Faculty faculty = facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        validatePreferredTimeOfDay(request.getPreferredTimeOfDay());
        validateSessionDistribution(request.getSessionDistribution());

        FacultyPreference preference = preferenceRepository.findByFacultyId(facultyId)
                .orElseGet(() -> {
                    FacultyPreference newPref = new FacultyPreference();
                    newPref.setFaculty(faculty);
                    newPref.setIsActive(true);
                    return newPref;
                });

        preference.setPreferredTimeOfDay(request.getPreferredTimeOfDay());
        preference.setSessionDistribution(request.getSessionDistribution());

        preference = preferenceRepository.save(preference);

        log.info("Faculty preferences set: facultyId={}, timeOfDay={}, distribution={}",
                facultyId, request.getPreferredTimeOfDay(), request.getSessionDistribution());

        return toDto(preference);
    }

    @Transactional(readOnly = true)
    public FacultyPreferenceDto getPreferences(Long facultyId) {
        facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        return preferenceRepository.findByFacultyId(facultyId)
                .map(this::toDto)
                .orElse(FacultyPreferenceDto.builder()
                        .facultyId(facultyId)
                        .preferredTimeOfDay("NO_PREFERENCE")
                        .sessionDistribution("NO_PREFERENCE")
                        .isActive(true)
                        .build());
    }

    private void validatePreferredTimeOfDay(String value) {
        if (!VALID_TIME_OF_DAY.contains(value)) {
            throw new BusinessRuleViolationException(
                    "Invalid preferred time of day '" + value + "'. Valid values: " + VALID_TIME_OF_DAY);
        }
    }

    private void validateSessionDistribution(String value) {
        if (!VALID_DISTRIBUTION.contains(value)) {
            throw new BusinessRuleViolationException(
                    "Invalid session distribution '" + value + "'. Valid values: " + VALID_DISTRIBUTION);
        }
    }

    private FacultyPreferenceDto toDto(FacultyPreference preference) {
        return FacultyPreferenceDto.builder()
                .id(preference.getId())
                .facultyId(preference.getFaculty().getId())
                .preferredTimeOfDay(preference.getPreferredTimeOfDay())
                .sessionDistribution(preference.getSessionDistribution())
                .isActive(preference.getIsActive())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}
