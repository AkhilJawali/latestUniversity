package com.utms.masterdata.academiccalendar;

import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkingDayPatternServiceTest {

    @Mock
    private WorkingDayPatternRepository patternRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private AcademicCalendarMapper mapper;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WorkingDayPatternService patternService;

    // --- create (setPattern) ---

    @Test
    void create_validFiveDayPattern_savesSuccessfully() {
        CreateWorkingDayPatternRequest request = CreateWorkingDayPatternRequest.builder()
                .campusId(1L)
                .patternType(PatternType.FIVE_DAY)
                .build();

        Campus campus = new Campus();
        campus.setId(1L);

        WorkingDayPattern pattern = new WorkingDayPattern();
        pattern.setId(1L);
        pattern.setCampusId(1L);
        pattern.setPatternType(PatternType.FIVE_DAY);

        WorkingDayPatternDto expectedDto = WorkingDayPatternDto.builder()
                .id(1L)
                .campusId(1L)
                .patternType(PatternType.FIVE_DAY)
                .build();

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(patternRepository.existsByCampusIdAndDeletedAtIsNull(1L)).thenReturn(false);
        when(campusRepository.getReferenceById(1L)).thenReturn(campus);
        when(patternRepository.save(any(WorkingDayPattern.class))).thenReturn(pattern);
        when(mapper.toPatternDto(pattern)).thenReturn(expectedDto);

        WorkingDayPatternDto result = patternService.create(request);

        assertNotNull(result);
        assertEquals(PatternType.FIVE_DAY, result.getPatternType());
        assertEquals(1L, result.getCampusId());
        verify(patternRepository).save(any(WorkingDayPattern.class));
    }

    @Test
    void create_duplicatePatternForCampus_throwsConflict() {
        CreateWorkingDayPatternRequest request = CreateWorkingDayPatternRequest.builder()
                .campusId(1L)
                .patternType(PatternType.SIX_DAY)
                .build();

        Campus campus = new Campus();
        campus.setId(1L);

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(patternRepository.existsByCampusIdAndDeletedAtIsNull(1L)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> patternService.create(request));

        assertTrue(exception.getMessage().contains("already exists"));
        verify(patternRepository, never()).save(any());
    }

    @Test
    void create_alternateSaturdayWithoutWorkingSaturdays_throwsBusinessRule() {
        CreateWorkingDayPatternRequest request = CreateWorkingDayPatternRequest.builder()
                .campusId(1L)
                .patternType(PatternType.ALTERNATE_SATURDAY)
                .workingSaturdays(null)
                .build();

        Campus campus = new Campus();
        campus.setId(1L);

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(patternRepository.existsByCampusIdAndDeletedAtIsNull(1L)).thenReturn(false);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> patternService.create(request));

        assertTrue(exception.getMessage().contains("ALTERNATE_SATURDAY"));
        assertTrue(exception.getMessage().contains("workingSaturdays"));
        verify(patternRepository, never()).save(any());
    }

    // --- update (pattern change triggers impact detection KD-38) ---

    @Test
    void update_patternChanged_emitsImpactEvent_KD38() {
        WorkingDayPattern existingPattern = new WorkingDayPattern();
        existingPattern.setId(1L);
        existingPattern.setCampusId(2L);
        existingPattern.setPatternType(PatternType.SIX_DAY);
        existingPattern.setWorkingSaturdays(null);

        CreateWorkingDayPatternRequest request = CreateWorkingDayPatternRequest.builder()
                .campusId(2L)
                .patternType(PatternType.FIVE_DAY)
                .build();

        WorkingDayPatternDto expectedDto = WorkingDayPatternDto.builder()
                .id(1L)
                .campusId(2L)
                .patternType(PatternType.FIVE_DAY)
                .build();

        when(patternRepository.findByCampusIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(existingPattern));
        when(patternRepository.save(any(WorkingDayPattern.class))).thenReturn(existingPattern);
        when(mapper.toPatternDto(existingPattern)).thenReturn(expectedDto);

        WorkingDayPatternDto result = patternService.update(2L, request);

        assertNotNull(result);
        assertEquals(PatternType.FIVE_DAY, result.getPatternType());
        // KD-38: pattern change must emit impact detection event for dates that became non-working
        verify(eventPublisher).publishEvent(any(CalendarImpactEvent.class));
    }

    // --- delete (BLOCKED per KD-39 — pattern is required per campus) ---

    @Test
    void delete_anyAttempt_throwsBusinessRuleViolation_KD39() {
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> patternService.delete(1L));

        assertTrue(exception.getMessage().contains("Cannot remove working day pattern"));
        assertTrue(exception.getMessage().contains("KD-39"));
        // Verify no repository interaction — blocked unconditionally
        verify(patternRepository, never()).save(any());
        verify(patternRepository, never()).delete(any());
    }
}
