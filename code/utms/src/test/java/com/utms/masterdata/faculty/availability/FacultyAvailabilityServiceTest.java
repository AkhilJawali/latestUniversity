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

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultyAvailabilityServiceTest {

    @Mock
    private FacultyAvailabilityWindowRepository availabilityWindowRepository;

    @Mock
    private FacultyRepository facultyRepository;

    @InjectMocks
    private FacultyAvailabilityService availabilityService;

    // --- createWindow ---

    @Test
    void createWindow_validRequest_returnsDto() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);
        faculty.setName("Dr. Smith");

        CreateAvailabilityWindowRequest request = CreateAvailabilityWindowRequest.builder()
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .reasonCode("PERSONAL")
                .reasonNote("Doctor appointment")
                .build();

        FacultyAvailabilityWindow savedWindow = new FacultyAvailabilityWindow();
        savedWindow.setId(10L);
        savedWindow.setFaculty(faculty);
        savedWindow.setDayOfWeek("MONDAY");
        savedWindow.setStartTime(LocalTime.of(9, 0));
        savedWindow.setEndTime(LocalTime.of(11, 0));
        savedWindow.setReasonCode("PERSONAL");
        savedWindow.setReasonNote("Doctor appointment");
        savedWindow.setIsActive(true);

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(availabilityWindowRepository.save(any(FacultyAvailabilityWindow.class))).thenReturn(savedWindow);

        FacultyAvailabilityWindowDto result = availabilityService.createWindow(1L, request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getFacultyId());
        assertEquals("MONDAY", result.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), result.getStartTime());
        assertEquals(LocalTime.of(11, 0), result.getEndTime());
        assertEquals("PERSONAL", result.getReasonCode());
        verify(availabilityWindowRepository).save(any(FacultyAvailabilityWindow.class));
    }

    @Test
    void createWindow_startTimeAfterEndTime_throwsBusinessRuleViolation() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);

        CreateAvailabilityWindowRequest request = CreateAvailabilityWindowRequest.builder()
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(10, 0))
                .reasonCode("PERSONAL")
                .build();

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));

        assertThrows(BusinessRuleViolationException.class,
                () -> availabilityService.createWindow(1L, request));

        verify(availabilityWindowRepository, never()).save(any());
    }

    @Test
    void createWindow_startTimeEqualsEndTime_throwsBusinessRuleViolation() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);

        CreateAvailabilityWindowRequest request = CreateAvailabilityWindowRequest.builder()
                .dayOfWeek("TUESDAY")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 0))
                .reasonCode("MEETING")
                .build();

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));

        assertThrows(BusinessRuleViolationException.class,
                () -> availabilityService.createWindow(1L, request));

        verify(availabilityWindowRepository, never()).save(any());
    }

    @Test
    void createWindow_facultyNotFound_throwsEntityNotFound() {
        CreateAvailabilityWindowRequest request = CreateAvailabilityWindowRequest.builder()
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .reasonCode("PERSONAL")
                .build();

        when(facultyRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> availabilityService.createWindow(999L, request));
    }

    @Test
    void createWindow_invalidDayOfWeek_throwsBusinessRuleViolation() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);

        CreateAvailabilityWindowRequest request = CreateAvailabilityWindowRequest.builder()
                .dayOfWeek("FUNDAY")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .reasonCode("PERSONAL")
                .build();

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));

        assertThrows(BusinessRuleViolationException.class,
                () -> availabilityService.createWindow(1L, request));
    }

    // --- deleteWindow ---

    @Test
    void deleteWindow_validRequest_softDeletes() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);

        FacultyAvailabilityWindow window = new FacultyAvailabilityWindow();
        window.setId(10L);
        window.setFaculty(faculty);
        window.setIsActive(true);

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(availabilityWindowRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(window));
        when(availabilityWindowRepository.save(any(FacultyAvailabilityWindow.class))).thenReturn(window);

        availabilityService.deleteWindow(1L, 10L);

        assertNotNull(window.getDeletedAt());
        assertFalse(window.getIsActive());
        verify(availabilityWindowRepository).save(window);
    }

    @Test
    void deleteWindow_windowNotFound_throwsEntityNotFound() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(availabilityWindowRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> availabilityService.deleteWindow(1L, 999L));
    }

    // --- listByFacultyId ---

    @Test
    void listByFacultyId_returnsWindowDtos() {
        Faculty faculty = new Faculty();
        faculty.setId(1L);

        FacultyAvailabilityWindow window1 = new FacultyAvailabilityWindow();
        window1.setId(10L);
        window1.setFaculty(faculty);
        window1.setDayOfWeek("MONDAY");
        window1.setStartTime(LocalTime.of(9, 0));
        window1.setEndTime(LocalTime.of(11, 0));
        window1.setReasonCode("PERSONAL");
        window1.setIsActive(true);

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(availabilityWindowRepository.findByFacultyIdAndDeletedAtIsNull(1L))
                .thenReturn(List.of(window1));

        List<FacultyAvailabilityWindowDto> result = availabilityService.listByFacultyId(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals("MONDAY", result.get(0).getDayOfWeek());
    }
}
