package com.utms.masterdata.faculty.availability;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.faculty.Faculty;
import com.utms.masterdata.faculty.FacultyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacultyAvailabilityService {

    private static final List<String> VALID_DAYS = List.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    );

    private final FacultyAvailabilityWindowRepository availabilityWindowRepository;
    private final FacultyRepository facultyRepository;

    @Transactional
    public FacultyAvailabilityWindowDto createWindow(Long facultyId, CreateAvailabilityWindowRequest request) {
        Faculty faculty = facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        validateDayOfWeek(request.getDayOfWeek());
        validateTimeOrder(request.getStartTime(), request.getEndTime());

        FacultyAvailabilityWindow window = new FacultyAvailabilityWindow();
        window.setFaculty(faculty);
        window.setDayOfWeek(request.getDayOfWeek());
        window.setStartTime(request.getStartTime());
        window.setEndTime(request.getEndTime());
        window.setReasonCode(request.getReasonCode());
        window.setReasonNote(request.getReasonNote());
        window.setIsActive(true);

        // TODO: Check session overlap — flag if window conflicts with existing scheduled sessions

        window = availabilityWindowRepository.save(window);

        log.info("Faculty availability window created: id={}, facultyId={}, day={}, {}-{}",
                window.getId(), facultyId, request.getDayOfWeek(), request.getStartTime(), request.getEndTime());

        return toDto(window);
    }

    @Transactional
    public FacultyAvailabilityWindowDto updateWindow(Long facultyId, Long windowId, UpdateAvailabilityWindowRequest request) {
        facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        FacultyAvailabilityWindow window = availabilityWindowRepository.findByIdAndDeletedAtIsNull(windowId)
                .orElseThrow(() -> new EntityNotFoundException("FacultyAvailabilityWindow", windowId));

        if (!window.getFaculty().getId().equals(facultyId)) {
            throw new EntityNotFoundException("FacultyAvailabilityWindow", windowId);
        }

        validateDayOfWeek(request.getDayOfWeek());
        validateTimeOrder(request.getStartTime(), request.getEndTime());

        window.setDayOfWeek(request.getDayOfWeek());
        window.setStartTime(request.getStartTime());
        window.setEndTime(request.getEndTime());
        window.setReasonCode(request.getReasonCode());
        window.setReasonNote(request.getReasonNote());

        window = availabilityWindowRepository.save(window);

        log.info("Faculty availability window updated: id={}, facultyId={}", windowId, facultyId);

        return toDto(window);
    }

    @Transactional
    public void deleteWindow(Long facultyId, Long windowId) {
        facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        FacultyAvailabilityWindow window = availabilityWindowRepository.findByIdAndDeletedAtIsNull(windowId)
                .orElseThrow(() -> new EntityNotFoundException("FacultyAvailabilityWindow", windowId));

        if (!window.getFaculty().getId().equals(facultyId)) {
            throw new EntityNotFoundException("FacultyAvailabilityWindow", windowId);
        }

        window.setDeletedAt(LocalDateTime.now());
        window.setIsActive(false);
        availabilityWindowRepository.save(window);

        log.info("Faculty availability window soft-deleted: id={}, facultyId={}", windowId, facultyId);
    }

    @Transactional(readOnly = true)
    public List<FacultyAvailabilityWindowDto> listByFacultyId(Long facultyId) {
        facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        return availabilityWindowRepository.findByFacultyIdAndDeletedAtIsNull(facultyId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void validateDayOfWeek(String dayOfWeek) {
        if (!VALID_DAYS.contains(dayOfWeek)) {
            throw new BusinessRuleViolationException(
                    "Invalid day of week '" + dayOfWeek + "'. Valid values: " + VALID_DAYS);
        }
    }

    private void validateTimeOrder(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BusinessRuleViolationException(
                    "Start time (" + startTime + ") must be before end time (" + endTime + ")");
        }
    }

    private FacultyAvailabilityWindowDto toDto(FacultyAvailabilityWindow window) {
        return FacultyAvailabilityWindowDto.builder()
                .id(window.getId())
                .facultyId(window.getFaculty().getId())
                .dayOfWeek(window.getDayOfWeek())
                .startTime(window.getStartTime())
                .endTime(window.getEndTime())
                .reasonCode(window.getReasonCode())
                .reasonNote(window.getReasonNote())
                .isActive(window.getIsActive())
                .createdAt(window.getCreatedAt())
                .updatedAt(window.getUpdatedAt())
                .build();
    }
}
