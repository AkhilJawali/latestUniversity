package com.utms.masterdata.course;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CoursePrerequisiteService coursePrerequisiteService;

    @Mock
    private CoursePrerequisiteRepository coursePrerequisiteRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private CourseService courseService;

    // --- create ---

    @Test
    void create_validRequest_returnsCourseDto() {
        CreateCourseRequest request = CreateCourseRequest.builder()
                .name("Data Structures")
                .code("CS201")
                .departmentId(1L)
                .lectureHours(3)
                .tutorialHours(1)
                .practicalHours(0)
                .credits(new BigDecimal("4.0"))
                .courseType("CORE")
                .equipmentTags(List.of("projector"))
                .build();

        Department department = new Department();
        department.setId(1L);
        department.setName("Computer Science");

        Course course = new Course();
        course.setId(10L);
        course.setCode("CS201");
        course.setName("Data Structures");
        course.setDepartment(department);
        course.setLectureHours(3);
        course.setTutorialHours(1);
        course.setPracticalHours(0);
        course.setCredits(new BigDecimal("4.0"));
        course.setCourseType("CORE");

        CourseDto expectedDto = CourseDto.builder()
                .id(10L)
                .name("Data Structures")
                .code("CS201")
                .departmentId(1L)
                .departmentName("Computer Science")
                .lectureHours(3)
                .tutorialHours(1)
                .practicalHours(0)
                .credits(new BigDecimal("4.0"))
                .courseType("CORE")
                .build();

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(courseRepository.existsByCodeAndDepartmentIdAndDeletedAtIsNull("CS201", 1L)).thenReturn(false);
        when(courseMapper.toEntity(request)).thenReturn(course);
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toDto(course)).thenReturn(expectedDto);

        CourseDto result = courseService.create(request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Data Structures", result.getName());
        assertEquals("CS201", result.getCode());
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void create_duplicateCode_throwsConflict() {
        CreateCourseRequest request = CreateCourseRequest.builder()
                .name("Data Structures")
                .code("CS201")
                .departmentId(1L)
                .lectureHours(3)
                .tutorialHours(1)
                .practicalHours(0)
                .credits(new BigDecimal("4.0"))
                .courseType("CORE")
                .build();

        Department department = new Department();
        department.setId(1L);

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(courseRepository.existsByCodeAndDepartmentIdAndDeletedAtIsNull("CS201", 1L)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> courseService.create(request));

        assertTrue(exception.getMessage().contains("CS201"));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void create_invalidDepartment_throwsNotFound() {
        CreateCourseRequest request = CreateCourseRequest.builder()
                .name("Data Structures")
                .code("CS201")
                .departmentId(99L)
                .lectureHours(3)
                .tutorialHours(1)
                .practicalHours(0)
                .credits(new BigDecimal("4.0"))
                .courseType("CORE")
                .build();

        when(departmentRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> courseService.create(request));

        assertTrue(exception.getMessage().contains("Department"));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void create_ltpAllZero_throwsBusinessRuleViolation() {
        CreateCourseRequest request = CreateCourseRequest.builder()
                .name("Empty Course")
                .code("CS000")
                .departmentId(1L)
                .lectureHours(0)
                .tutorialHours(0)
                .practicalHours(0)
                .credits(new BigDecimal("1.0"))
                .courseType("CORE")
                .build();

        Department department = new Department();
        department.setId(1L);

        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(department));
        when(courseRepository.existsByCodeAndDepartmentIdAndDeletedAtIsNull("CS000", 1L)).thenReturn(false);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> courseService.create(request));

        assertTrue(exception.getMessage().contains("Lecture, Tutorial, or Practical"));
        verify(courseRepository, never()).save(any());
    }

    // --- update ---

    @Test
    void update_ltpChange_emitsCourseLtpChangedEvent() {
        Course course = new Course();
        course.setId(10L);
        course.setLectureHours(3);
        course.setTutorialHours(1);
        course.setPracticalHours(0);
        course.setCourseType("CORE");

        UpdateCourseRequest request = UpdateCourseRequest.builder()
                .name("Data Structures")
                .lectureHours(4)
                .tutorialHours(1)
                .practicalHours(2)
                .credits(new BigDecimal("5.0"))
                .courseType("CORE")
                .equipmentTags(List.of())
                .build();

        CourseDto expectedDto = CourseDto.builder().id(10L).build();

        when(courseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toDto(course)).thenReturn(expectedDto);

        courseService.update(10L, request);

        ArgumentCaptor<CourseLtpChangedEvent> eventCaptor = ArgumentCaptor.forClass(CourseLtpChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        CourseLtpChangedEvent event = eventCaptor.getValue();
        assertEquals(10L, event.courseId());
        assertEquals(3, event.oldLectureHours());
        assertEquals(1, event.oldTutorialHours());
        assertEquals(0, event.oldPracticalHours());
        assertEquals(4, event.newLectureHours());
        assertEquals(1, event.newTutorialHours());
        assertEquals(2, event.newPracticalHours());
    }

    @Test
    void update_typeChange_emitsCourseTypeChangedEvent() {
        Course course = new Course();
        course.setId(10L);
        course.setLectureHours(3);
        course.setTutorialHours(1);
        course.setPracticalHours(0);
        course.setCourseType("CORE");

        UpdateCourseRequest request = UpdateCourseRequest.builder()
                .name("Elective Course")
                .lectureHours(3)
                .tutorialHours(1)
                .practicalHours(0)
                .credits(new BigDecimal("4.0"))
                .courseType("ELECTIVE")
                .equipmentTags(List.of())
                .build();

        CourseDto expectedDto = CourseDto.builder().id(10L).build();

        when(courseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toDto(course)).thenReturn(expectedDto);

        courseService.update(10L, request);

        ArgumentCaptor<CourseTypeChangedEvent> eventCaptor = ArgumentCaptor.forClass(CourseTypeChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        CourseTypeChangedEvent event = eventCaptor.getValue();
        assertEquals(10L, event.courseId());
        assertEquals("CORE", event.previousType());
        assertEquals("ELECTIVE", event.newType());
    }

    // --- delete ---

    @Test
    void delete_withActiveDependents_throwsBusinessRuleViolation() {
        Course course = new Course();
        course.setId(10L);
        course.setCode("CS201");
        course.setIsActive(true);

        when(courseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(course));
        when(coursePrerequisiteRepository.countByPrerequisiteCourseIdAndCourseDeletedAtIsNull(10L)).thenReturn(2L);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> courseService.delete(10L));

        assertTrue(exception.getMessage().contains("Cannot delete course"));
        assertFalse(exception.getDetails().isEmpty());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void delete_withOnlySoftDeletedDependents_softDeletes() {
        Course course = new Course();
        course.setId(10L);
        course.setCode("CS201");
        course.setIsActive(true);

        when(courseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(course));
        when(coursePrerequisiteRepository.countByPrerequisiteCourseIdAndCourseDeletedAtIsNull(10L)).thenReturn(0L);

        courseService.delete(10L);

        assertNotNull(course.getDeletedAt());
        assertFalse(course.getIsActive());
        verify(courseRepository).save(course);
    }
}
