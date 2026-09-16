package com.utms.scheduling.engine.service;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.scheduling.engine.entity.SessionDerivationRule;
import com.utms.scheduling.engine.enums.SessionType;
import com.utms.scheduling.engine.model.CourseAssignment;
import com.utms.scheduling.engine.model.SchedulingInput;
import com.utms.scheduling.engine.model.SessionVariable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Derives CSP variables (sessions) from course L-T-P splits (FR-3.1 through FR-3.3).
 * PD-74: Uses campus-specific derivation rules for duration mapping.
 * KD-47: Each session carries requiredDurationMinutes for slot matching.
 * KD-50: Sort uses stable sequenceIndex (Fix #9).
 */
@Service
@Slf4j
public class SessionDeriver {

    /**
     * FR-3.1: Derive sessions from L-T-P.
     * Each derived session = one CSP variable (FR-3.2).
     * Total sessions = sum of derived L + T + P sessions (FR-3.3).
     */
    public List<SessionVariable> derive(SchedulingInput input) {
        List<SessionVariable> variables = new ArrayList<>();
        AtomicInteger sequenceCounter = new AtomicInteger(0);

        for (CourseAssignment course : input.getCourses()) {
            // Derive LECTURE sessions
            SessionDerivationRule lectureRule = findRule("LECTURE", input.getDerivationRules());
            int lectureSessions = computeSessionCount(course.getLectureHours(), lectureRule);
            for (int i = 0; i < lectureSessions; i++) {
                variables.add(SessionVariable.builder()
                    .courseId(course.getCourseId())
                    .facultyId(course.getFacultyId())
                    .batchId(course.getBatchId())
                    .sectionId(course.getSectionId())
                    .sessionType(SessionType.LECTURE)
                    .requiredDurationMinutes(lectureRule.getSlotDurationMinutes())
                    .requiredEquipment(course.getEquipmentTags())
                    .batchStrength(course.getBatchStrength())
                    .sequenceIndex(sequenceCounter.getAndIncrement())
                    .build());
            }

            // Derive TUTORIAL sessions
            SessionDerivationRule tutorialRule = findRule("TUTORIAL", input.getDerivationRules());
            int tutorialSessions = computeSessionCount(course.getTutorialHours(), tutorialRule);
            for (int i = 0; i < tutorialSessions; i++) {
                variables.add(SessionVariable.builder()
                    .courseId(course.getCourseId())
                    .facultyId(course.getFacultyId())
                    .batchId(course.getBatchId())
                    .sectionId(course.getSectionId())
                    .sessionType(SessionType.TUTORIAL)
                    .requiredDurationMinutes(tutorialRule.getSlotDurationMinutes())
                    .requiredEquipment(List.of()) // Tutorials don't require equipment
                    .batchStrength(course.getBatchStrength())
                    .sequenceIndex(sequenceCounter.getAndIncrement())
                    .build());
            }

            // Derive PRACTICAL sessions (contiguous blocking handled by A4-24)
            SessionDerivationRule practicalRule = findRule("PRACTICAL", input.getDerivationRules());
            int practicalSessions = computeSessionCount(course.getPracticalHours(), practicalRule);
            for (int i = 0; i < practicalSessions; i++) {
                variables.add(SessionVariable.builder()
                    .courseId(course.getCourseId())
                    .facultyId(course.getFacultyId())
                    .batchId(course.getBatchId())
                    .sectionId(course.getSectionId())
                    .sessionType(SessionType.PRACTICAL)
                    .requiredDurationMinutes(practicalRule.getSlotDurationMinutes())
                    .requiredEquipment(course.getEquipmentTags())
                    .batchStrength(course.getBatchStrength())
                    .sequenceIndex(sequenceCounter.getAndIncrement())
                    .build());
            }
        }

        // KD-50 + Fix #9: Sort by stable attributes (sequenceIndex as tiebreaker)
        variables.sort(Comparator.comparing(SessionVariable::getCourseId)
            .thenComparing(SessionVariable::getBatchId)
            .thenComparing(SessionVariable::getSessionType)
            .thenComparing(SessionVariable::getSequenceIndex));

        log.info("Derived {} sessions from {} course assignments", variables.size(), input.getCourses().size());
        return variables;
    }

    /**
     * PD-74: Compute session count from hours using campus derivation rules.
     * Ceiling division: hours / hoursPerSession, rounded up.
     */
    private int computeSessionCount(int hours, SessionDerivationRule rule) {
        if (hours <= 0) return 0;
        double hoursPerSession = rule.getHoursPerSession().doubleValue();
        return (int) Math.ceil((double) hours / hoursPerSession);
    }

    /**
     * Find the derivation rule for a given component type.
     * Accepts both the long form ("LECTURE"/"TUTORIAL"/"PRACTICAL") and the short code
     * ("L"/"T"/"P") so rules seeded in either convention match. Throws if not found.
     */
    private SessionDerivationRule findRule(String componentType, List<SessionDerivationRule> rules) {
        String shortCode = toShortCode(componentType);
        return rules.stream()
            .filter(r -> {
                String rc = r.getComponentType();
                return rc.equalsIgnoreCase(componentType) || rc.equalsIgnoreCase(shortCode);
            })
            .findFirst()
            .orElseThrow(() -> new BusinessRuleViolationException(
                "No derivation rule for component type: " + componentType, List.of()));
    }

    /** Maps a long component name to its single-letter code (LECTURE->L, TUTORIAL->T, PRACTICAL->P). */
    private String toShortCode(String componentType) {
        return switch (componentType.toUpperCase()) {
            case "LECTURE" -> "L";
            case "TUTORIAL" -> "T";
            case "PRACTICAL" -> "P";
            default -> componentType;
        };
    }
}
