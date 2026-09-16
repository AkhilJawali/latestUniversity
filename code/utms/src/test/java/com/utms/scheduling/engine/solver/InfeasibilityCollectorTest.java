package com.utms.scheduling.engine.solver;

import com.utms.scheduling.engine.enums.SessionType;
import com.utms.scheduling.engine.model.SessionVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for InfeasibilityCollector — verifies record, recordDeadEnd, buildReport.
 */
class InfeasibilityCollectorTest {

    private List<SessionVariable> variables;
    private InfeasibilityCollector collector;

    @BeforeEach
    void setUp() {
        variables = List.of(
            SessionVariable.builder()
                .courseId(101L).facultyId(201L).batchId(301L).sectionId(401L)
                .sessionType(SessionType.LECTURE).requiredDurationMinutes(60)
                .requiredEquipment(List.of()).batchStrength(50).sequenceIndex(0)
                .build(),
            SessionVariable.builder()
                .courseId(102L).facultyId(202L).batchId(302L).sectionId(402L)
                .sessionType(SessionType.PRACTICAL).requiredDurationMinutes(180)
                .requiredEquipment(List.of("PROJECTOR")).batchStrength(30).sequenceIndex(1)
                .build(),
            SessionVariable.builder()
                .courseId(103L).facultyId(203L).batchId(303L).sectionId(403L)
                .sessionType(SessionType.TUTORIAL).requiredDurationMinutes(60)
                .requiredEquipment(List.of()).batchStrength(40).sequenceIndex(2)
                .build()
        );
        collector = new InfeasibilityCollector(variables);
    }

    @Test
    @DisplayName("record adds an infeasibility entry with session description and constraints")
    void record_addsEntry() {
        List<String> constraints = List.of("EQUIPMENT_MISMATCH", "ROOM_CAPACITY");

        collector.record(variables.get(0), constraints);

        assertThat(collector.hasEntries()).isTrue();
        assertThat(collector.getEntries()).hasSize(1);

        InfeasibilityEntry entry = collector.getEntries().get(0);
        assertThat(entry.sessionDescription()).contains("Course=101");
        assertThat(entry.sessionDescription()).contains("Faculty=201");
        assertThat(entry.constraintTypes()).containsExactly("EQUIPMENT_MISMATCH", "ROOM_CAPACITY");
        assertThat(entry.explanation()).contains("constraint propagation");
    }

    @Test
    @DisplayName("record multiple entries accumulates them")
    void record_multipleEntries_accumulates() {
        collector.record(variables.get(0), List.of("FACULTY_AVAILABILITY"));
        collector.record(variables.get(1), List.of("EQUIPMENT_MISMATCH"));

        assertThat(collector.hasEntries()).isTrue();
        assertThat(collector.getEntries()).hasSize(2);
    }

    @Test
    @DisplayName("hasEntries returns false when no records added")
    void hasEntries_noRecords_returnsFalse() {
        assertThat(collector.hasEntries()).isFalse();
    }

    @Test
    @DisplayName("recordDeadEnd increments frequency for variable index")
    void recordDeadEnd_incrementsFrequency() {
        collector.recordDeadEnd(0);
        collector.recordDeadEnd(0);
        collector.recordDeadEnd(1);

        Map<Integer, Integer> freq = collector.getDeadEndFrequency();
        assertThat(freq.get(0)).isEqualTo(2);
        assertThat(freq.get(1)).isEqualTo(1);
        assertThat(freq.get(2)).isNull();
    }

    @Test
    @DisplayName("buildReport returns propagation entries if present")
    void buildReport_withPropagationEntries_returnsThem() {
        collector.record(variables.get(0), List.of("ROOM_CAPACITY"));
        collector.recordDeadEnd(1); // Dead-end should be ignored in output

        CSPState mockState = mock(CSPState.class);
        List<InfeasibilityEntry> report = collector.buildReport(mockState);

        assertThat(report).hasSize(1);
        assertThat(report.get(0).sessionDescription()).contains("Course=101");
    }

    @Test
    @DisplayName("buildReport generates entries from dead-end data when no propagation entries")
    void buildReport_noPropagationEntries_usesDeadEndData() {
        // Simulate dead-ends without propagation-phase infeasibility
        collector.recordDeadEnd(0);
        collector.recordDeadEnd(0);
        collector.recordDeadEnd(1);

        CSPState mockState = mock(CSPState.class);
        when(mockState.getVariableCount()).thenReturn(3);
        when(mockState.isAssigned(0)).thenReturn(true);  // Placed
        when(mockState.isAssigned(1)).thenReturn(false); // Unplaced
        when(mockState.isAssigned(2)).thenReturn(false); // Unplaced

        List<InfeasibilityEntry> report = collector.buildReport(mockState);

        assertThat(report).hasSize(2);
        // All unplaced variables should appear in the report
        assertThat(report.stream().map(InfeasibilityEntry::sessionDescription))
            .anyMatch(s -> s.contains("Course=102"))
            .anyMatch(s -> s.contains("Course=103"));
    }

    @Test
    @DisplayName("buildReport sorts by dead-end frequency descending")
    void buildReport_sortsByFrequencyDescending() {
        // Variable 2 has more dead-ends than variable 1
        collector.recordDeadEnd(2);
        collector.recordDeadEnd(2);
        collector.recordDeadEnd(2);
        collector.recordDeadEnd(1);

        CSPState mockState = mock(CSPState.class);
        when(mockState.getVariableCount()).thenReturn(3);
        when(mockState.isAssigned(0)).thenReturn(true);
        when(mockState.isAssigned(1)).thenReturn(false);
        when(mockState.isAssigned(2)).thenReturn(false);

        List<InfeasibilityEntry> report = collector.buildReport(mockState);

        assertThat(report).hasSize(2);
        // First entry should be the most constrained (variable 2 with 3 dead-ends)
        assertThat(report.get(0).sessionDescription()).contains("Course=103");
        assertThat(report.get(1).sessionDescription()).contains("Course=102");
    }

    @Test
    @DisplayName("getEntries returns defensive copy")
    void getEntries_returnsDefensiveCopy() {
        collector.record(variables.get(0), List.of("TEST"));

        List<InfeasibilityEntry> entries1 = collector.getEntries();
        List<InfeasibilityEntry> entries2 = collector.getEntries();

        assertThat(entries1).isNotSameAs(entries2);
        assertThat(entries1).isEqualTo(entries2);
    }

    @Test
    @DisplayName("getDeadEndFrequency returns defensive copy")
    void getDeadEndFrequency_returnsDefensiveCopy() {
        collector.recordDeadEnd(0);

        Map<Integer, Integer> freq1 = collector.getDeadEndFrequency();
        Map<Integer, Integer> freq2 = collector.getDeadEndFrequency();

        assertThat(freq1).isNotSameAs(freq2);
        assertThat(freq1).isEqualTo(freq2);
    }
}
