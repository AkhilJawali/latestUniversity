package com.utms.masterdata.faculty;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.course.Course;
import com.utms.masterdata.course.CourseRepository;
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
class FacultyCompetencyServiceTest {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private FacultyCompetencyRepository facultyCompetencyRepository;

    @InjectMocks
    private FacultyCompetencyService facultyCompetencyService;

    // --- addCompetencies ---

    @Test
    void addCompetencies_newCompetency_createsLink() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        Course course = new Course();
        course.setId(5L);

        when(facultyRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(faculty));
        when(facultyCompetencyRepository.existsByFacultyIdAndCourseIdAndDeletedAtIsNull(10L, 5L)).thenReturn(false);
        when(courseRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(course));
        when(facultyCompetencyRepository.findByFacultyIdAndCourseId(10L, 5L)).thenReturn(Optional.empty());
        when(facultyCompetencyRepository.save(any(FacultyCompetency.class))).thenAnswer(inv -> inv.getArgument(0));

        facultyCompetencyService.addCompetencies(10L, List.of(5L));

        verify(facultyCompetencyRepository).save(any(FacultyCompetency.class));
    }

    @Test
    void addCompetencies_alreadyActive_skips() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        when(facultyRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(faculty));
        when(facultyCompetencyRepository.existsByFacultyIdAndCourseIdAndDeletedAtIsNull(10L, 5L)).thenReturn(true);

        facultyCompetencyService.addCompetencies(10L, List.of(5L));

        verify(facultyCompetencyRepository, never()).save(any());
    }

    @Test
    void addCompetencies_softDeletedExists_reactivates() {
        Faculty faculty = new Faculty();
        faculty.setId(10L);

        Course course = new Course();
        course.setId(5L);

        FacultyCompetency existingFc = new FacultyCompetency();
        existingFc.setId(99L);
        existingFc.setFaculty(faculty);
        existingFc.setCourse(course);
        existingFc.setIsActive(false);
        existingFc.setDeletedAt(java.time.LocalDateTime.now());

        when(facultyRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(faculty));
        when(facultyCompetencyRepository.existsByFacultyIdAndCourseIdAndDeletedAtIsNull(10L, 5L)).thenReturn(false);
        when(courseRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(course));
        when(facultyCompetencyRepository.findByFacultyIdAndCourseId(10L, 5L)).thenReturn(Optional.of(existingFc));
        when(facultyCompetencyRepository.save(any(FacultyCompetency.class))).thenAnswer(inv -> inv.getArgument(0));

        facultyCompetencyService.addCompetencies(10L, List.of(5L));

        assertNull(existingFc.getDeletedAt());
        assertTrue(existingFc.getIsActive());
        verify(facultyCompetencyRepository).save(existingFc);
    }

    // --- removeCompetency ---

    @Test
    void removeCompetency_existing_softDeletes() {
        FacultyCompetency fc = new FacultyCompetency();
        fc.setId(99L);
        fc.setIsActive(true);
        fc.setDeletedAt(null);

        when(facultyCompetencyRepository.findByFacultyIdAndCourseId(10L, 5L)).thenReturn(Optional.of(fc));
        when(facultyCompetencyRepository.save(any(FacultyCompetency.class))).thenAnswer(inv -> inv.getArgument(0));

        facultyCompetencyService.removeCompetency(10L, 5L);

        assertNotNull(fc.getDeletedAt());
        assertFalse(fc.getIsActive());
        verify(facultyCompetencyRepository).save(fc);
    }

    @Test
    void removeCompetency_notFound_throwsNotFound() {
        when(facultyCompetencyRepository.findByFacultyIdAndCourseId(10L, 5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> facultyCompetencyService.removeCompetency(10L, 5L));

        verify(facultyCompetencyRepository, never()).save(any());
    }
}
