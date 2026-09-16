package com.utms.masterdata.course;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoursePrerequisiteService {

    private final CourseRepository courseRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;

    @Transactional
    public void addPrerequisite(Long courseId, Long prerequisiteId) {
        if (courseId.equals(prerequisiteId)) {
            throw new BusinessRuleViolationException("A course cannot be its own prerequisite");
        }

        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));

        Course prerequisite = courseRepository.findByIdAndDeletedAtIsNull(prerequisiteId)
                .orElseThrow(() -> new EntityNotFoundException("Course", prerequisiteId));

        // Check if link already exists
        if (coursePrerequisiteRepository.findByCourseIdAndPrerequisiteCourseId(courseId, prerequisiteId).isPresent()) {
            throw new BusinessRuleViolationException(
                    "Prerequisite link already exists between course " + courseId + " and " + prerequisiteId);
        }

        // Cycle detection using BFS with parent tracking for path reconstruction
        detectCycle(courseId, prerequisiteId);

        CoursePrerequisite cp = new CoursePrerequisite();
        cp.setCourse(course);
        cp.setPrerequisiteCourse(prerequisite);
        cp.setIsActive(true);
        coursePrerequisiteRepository.save(cp);

        log.info("Prerequisite added: course={} requires prerequisite={}", courseId, prerequisiteId);
    }

    @Transactional
    public void removePrerequisite(Long courseId, Long prerequisiteId) {
        CoursePrerequisite cp = coursePrerequisiteRepository
                .findByCourseIdAndPrerequisiteCourseId(courseId, prerequisiteId)
                .orElseThrow(() -> new EntityNotFoundException("CoursePrerequisite",
                        "courseId=" + courseId + ",prerequisiteId=" + prerequisiteId, "not found"));

        coursePrerequisiteRepository.delete(cp);
        log.info("Prerequisite removed: course={} no longer requires prerequisite={}", courseId, prerequisiteId);
    }

    @Transactional(readOnly = true)
    public List<Long> getPrerequisiteIds(Long courseId) {
        return coursePrerequisiteRepository.findPrerequisiteIdsByCourseId(courseId);
    }

    /**
     * BFS-based cycle detection with parent tracking for path reconstruction.
     * Checks if adding prerequisiteId as a prerequisite of courseId would create a cycle.
     * A cycle exists if courseId is reachable from prerequisiteId through existing prerequisites.
     */
    private void detectCycle(Long courseId, Long prerequisiteId) {
        // BFS from prerequisiteId's prerequisites — if we can reach courseId, it's a cycle
        Queue<Long> queue = new LinkedList<>();
        Map<Long, Long> parentMap = new HashMap<>();
        Set<Long> visited = new HashSet<>();

        queue.add(prerequisiteId);
        visited.add(prerequisiteId);
        parentMap.put(prerequisiteId, null);

        while (!queue.isEmpty()) {
            Long current = queue.poll();

            List<Long> prereqs = coursePrerequisiteRepository.findPrerequisiteIdsByCourseId(current);
            for (Long prereqId : prereqs) {
                if (prereqId.equals(courseId)) {
                    // Cycle detected — reconstruct path
                    List<Long> cyclePath = reconstructPath(parentMap, current);
                    cyclePath.add(0, courseId);
                    cyclePath.add(prereqId);

                    throw new BusinessRuleViolationException(
                            "Adding this prerequisite would create a cycle: " + cyclePath);
                }

                if (!visited.contains(prereqId)) {
                    visited.add(prereqId);
                    parentMap.put(prereqId, current);
                    queue.add(prereqId);
                }
            }
        }
    }

    private List<Long> reconstructPath(Map<Long, Long> parentMap, Long end) {
        List<Long> path = new ArrayList<>();
        Long current = end;
        while (current != null) {
            path.add(0, current);
            current = parentMap.get(current);
        }
        return path;
    }
}
