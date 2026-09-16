package com.utms.masterdata.course;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoursePrerequisiteServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CoursePrerequisiteRepository coursePrerequisiteRepository;

    @InjectMocks
    private CoursePrerequisiteService coursePrerequisiteService;

    // --- addPrerequisite ---

    @Test
    void addPrerequisite_validRequest_persists() {
        Course course = new Course();
        course.setId(1L);

        Course prerequisite = new Course();
        prerequisite.setId(2L);

        when(courseRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(course));
        when(courseRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(prerequisite));
        when(coursePrerequisiteRepository.findByCourseIdAndPrerequisiteCourseId(1L, 2L))
                .thenReturn(Optional.empty());
        when(coursePrerequisiteRepository.findPrerequisiteIdsByCourseId(2L))
                .thenReturn(List.of());

        coursePrerequisiteService.addPrerequisite(1L, 2L);

        verify(coursePrerequisiteRepository).save(any(CoursePrerequisite.class));
    }

    @Test
    void addPrerequisite_createsCycle_throwsBusinessRuleViolation() {
        // Setup: Course 1 requires Course 2. Now trying to add Course 2 requires Course 1.
        // This creates a cycle: 1 -> 2 -> 1
        Course course2 = new Course();
        course2.setId(2L);

        Course course1 = new Course();
        course1.setId(1L);

        when(courseRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(course2));
        when(courseRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(course1));
        when(coursePrerequisiteRepository.findByCourseIdAndPrerequisiteCourseId(2L, 1L))
                .thenReturn(Optional.empty());
        // BFS: from courseId=1's prerequisites, we find courseId=2 (the target)
        when(coursePrerequisiteRepository.findPrerequisiteIdsByCourseId(1L))
                .thenReturn(List.of(2L));

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> coursePrerequisiteService.addPrerequisite(2L, 1L));

        assertTrue(exception.getMessage().contains("cycle"));
        verify(coursePrerequisiteRepository, never()).save(any());
    }

    @Test
    void addPrerequisite_selfReference_throwsBusinessRuleViolation() {
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> coursePrerequisiteService.addPrerequisite(1L, 1L));

        assertTrue(exception.getMessage().contains("its own prerequisite"));
        verify(coursePrerequisiteRepository, never()).save(any());
    }

    @Test
    void addPrerequisite_softDeletedCourse_throwsNotFound() {
        when(courseRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> coursePrerequisiteService.addPrerequisite(1L, 2L));

        assertTrue(exception.getMessage().contains("Course"));
        verify(coursePrerequisiteRepository, never()).save(any());
    }

    // --- removePrerequisite ---

    @Test
    void removePrerequisite_exists_deletesPhysically() {
        CoursePrerequisite cp = new CoursePrerequisite();
        cp.setId(5L);

        when(coursePrerequisiteRepository.findByCourseIdAndPrerequisiteCourseId(1L, 2L))
                .thenReturn(Optional.of(cp));

        coursePrerequisiteService.removePrerequisite(1L, 2L);

        verify(coursePrerequisiteRepository).delete(cp);
    }

    @Test
    void removePrerequisite_notFound_throwsEntityNotFoundException() {
        when(coursePrerequisiteRepository.findByCourseIdAndPrerequisiteCourseId(1L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> coursePrerequisiteService.removePrerequisite(1L, 2L));
    }
}
