package com.utms.masterdata.faculty.availability;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.faculty.Faculty;
import com.utms.masterdata.faculty.FacultyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the AvailabilityQueryService read contract.
 * Mode is determined by faculty designation:
 * - VISITING/ADJUNCT designations → AVAILABLE mode (declare when available)
 * - All other designations → BLOCKED mode (declare when blocked)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityQueryServiceImpl implements AvailabilityQueryService {

    private static final Set<String> AVAILABLE_MODE_DESIGNATIONS = Set.of(
            "Visiting Faculty"
    );

    private final FacultyRepository facultyRepository;
    private final FacultyAvailabilityWindowRepository windowRepository;
    private final FacultyPreferenceRepository preferenceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TimeRange> getHardBlockedSlots(Long facultyId, String dayOfWeek) {
        Faculty faculty = findFacultyOrThrow(facultyId);
        AvailabilityMode mode = resolveMode(faculty);

        List<FacultyAvailabilityWindow> windows =
                windowRepository.findByFacultyIdAndDayOfWeekAndDeletedAtIsNull(facultyId, dayOfWeek);

        if (mode == AvailabilityMode.BLOCKED) {
            // Windows represent blocked times — return them directly
            return windows.stream()
                    .map(w -> new TimeRange(w.getStartTime(), w.getEndTime()))
                    .toList();
        } else {
            // AVAILABLE mode: windows represent available times
            // Return declared windows; caller uses isAvailable() for actual checks
            return windows.stream()
                    .map(w -> new TimeRange(w.getStartTime(), w.getEndTime()))
                    .toList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FacultyPreferences getSoftPreferences(Long facultyId) {
        findFacultyOrThrow(facultyId);

        return preferenceRepository.findByFacultyId(facultyId)
                .map(p -> new FacultyPreferences(p.getPreferredTimeOfDay(), p.getSessionDistribution()))
                .orElse(FacultyPreferences.NO_PREFERENCE);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAvailable(Long facultyId, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
        Faculty faculty = findFacultyOrThrow(facultyId);
        AvailabilityMode mode = resolveMode(faculty);

        List<FacultyAvailabilityWindow> windows =
                windowRepository.findByFacultyIdAndDayOfWeekAndDeletedAtIsNull(facultyId, dayOfWeek);

        TimeRange queryRange = new TimeRange(startTime, endTime);

        if (mode == AvailabilityMode.BLOCKED) {
            // BLOCKED mode: faculty is available UNLESS the query range overlaps a blocked window
            return windows.stream()
                    .map(w -> new TimeRange(w.getStartTime(), w.getEndTime()))
                    .noneMatch(queryRange::overlaps);
        } else {
            // AVAILABLE mode: faculty is available ONLY IF the query range is fully covered
            // by at least one declared available window
            return windows.stream()
                    .map(w -> new TimeRange(w.getStartTime(), w.getEndTime()))
                    .anyMatch(available ->
                            !available.start().isAfter(startTime) && !available.end().isBefore(endTime));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityMode getMode(Long facultyId) {
        Faculty faculty = findFacultyOrThrow(facultyId);
        return resolveMode(faculty);
    }

    private AvailabilityMode resolveMode(Faculty faculty) {
        if (AVAILABLE_MODE_DESIGNATIONS.contains(faculty.getDesignation())) {
            return AvailabilityMode.AVAILABLE;
        }
        return AvailabilityMode.BLOCKED;
    }

    private Faculty findFacultyOrThrow(Long facultyId) {
        return facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));
    }
}
