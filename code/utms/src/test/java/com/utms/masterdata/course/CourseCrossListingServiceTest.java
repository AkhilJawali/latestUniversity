package com.utms.masterdata.course;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
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
class CourseCrossListingServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseDepartmentLinkRepository courseDepartmentLinkRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private CourseCrossListingService courseCrossListingService;

    // --- addCrossListing ---

    @Test
    void addCrossListing_validRequest_persistsAndSetsFlag() {
        Department owningDept = new Department();
        owningDept.setId(1L);

        Course course = new Course();
        course.setId(10L);
        course.setCode("CS201");
        course.setDepartment(owningDept);
        course.setIsCrossListed(false);

        Department targetDept = new Department();
        targetDept.setId(2L);

        when(courseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(course));
        when(departmentRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(targetDept));
        when(courseDepartmentLinkRepository.existsByCourseIdAndDepartmentId(10L, 2L)).thenReturn(false);
        when(courseDepartmentLinkRepository.existsOtherCourseWithCodeInDepartment("CS201", 2L, 10L))
                .thenReturn(false);

        courseCrossListingService.addCrossListing(10L, 2L);

        verify(courseDepartmentLinkRepository).save(any(CourseDepartmentLink.class));
        assertTrue(course.getIsCrossListed());
        verify(courseRepository).save(course);
    }

    @Test
    void addCrossListing_codeCollision_throwsConflict() {
        Department owningDept = new Department();
        owningDept.setId(1L);

        Course course = new Course();
        course.setId(10L);
        course.setCode("CS201");
        course.setDepartment(owningDept);

        Department targetDept = new Department();
        targetDept.setId(2L);

        when(courseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(course));
        when(departmentRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(targetDept));
        when(courseDepartmentLinkRepository.existsByCourseIdAndDepartmentId(10L, 2L)).thenReturn(false);
        when(courseDepartmentLinkRepository.existsOtherCourseWithCodeInDepartment("CS201", 2L, 10L))
                .thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> courseCrossListingService.addCrossListing(10L, 2L));

        assertTrue(exception.getMessage().contains("CS201"));
        verify(courseDepartmentLinkRepository, never()).save(any());
    }

    @Test
    void addCrossListing_toOwningDepartment_throwsBusinessRuleViolation() {
        Department owningDept = new Department();
        owningDept.setId(1L);

        Course course = new Course();
        course.setId(10L);
        course.setCode("CS201");
        course.setDepartment(owningDept);

        when(courseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(course));
        when(departmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(owningDept));

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> courseCrossListingService.addCrossListing(10L, 1L));

        assertTrue(exception.getMessage().contains("owning department"));
        verify(courseDepartmentLinkRepository, never()).save(any());
    }

    // --- removeCrossListing ---

    @Test
    void removeCrossListing_lastLink_updatesFlagToFalse() {
        CourseDepartmentLink link = new CourseDepartmentLink();
        link.setId(5L);

        Course course = new Course();
        course.setId(10L);
        course.setIsCrossListed(true);

        when(courseDepartmentLinkRepository.findByCourseIdAndDepartmentId(10L, 2L))
                .thenReturn(Optional.of(link));
        when(courseDepartmentLinkRepository.countByCourseId(10L)).thenReturn(0L);
        when(courseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(course));

        courseCrossListingService.removeCrossListing(10L, 2L);

        verify(courseDepartmentLinkRepository).delete(link);
        assertFalse(course.getIsCrossListed());
        verify(courseRepository).save(course);
    }

    @Test
    void removeCrossListing_otherLinksRemain_flagStaysTrue() {
        CourseDepartmentLink link = new CourseDepartmentLink();
        link.setId(5L);

        when(courseDepartmentLinkRepository.findByCourseIdAndDepartmentId(10L, 2L))
                .thenReturn(Optional.of(link));
        when(courseDepartmentLinkRepository.countByCourseId(10L)).thenReturn(1L);

        courseCrossListingService.removeCrossListing(10L, 2L);

        verify(courseDepartmentLinkRepository).delete(link);
        verify(courseRepository, never()).save(any());
    }
}
