package com.utms.scheduling.engine.model;

import com.utms.scheduling.engine.entity.ScheduledSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RegenerationScope (A4-14, KD-62). Verifies isEmpty() and matches()
 * across batch / section / course id-sets.
 */
class RegenerationScopeTest {

    private ScheduledSession session(Long batchId, Long sectionId, Long courseId) {
        ScheduledSession s = new ScheduledSession();
        s.setBatchId(batchId);
        s.setSectionId(sectionId);
        s.setCourseId(courseId);
        return s;
    }

    @Test
    @DisplayName("isEmpty true when all id-sets are null or empty")
    void isEmpty_allBlank_returnsTrue() {
        RegenerationScope scope = RegenerationScope.builder()
                .batchIds(List.of()).sectionIds(List.of()).courseIds(List.of()).build();
        assertThat(scope.isEmpty()).isTrue();

        RegenerationScope nullScope = RegenerationScope.builder().build();
        assertThat(nullScope.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("isEmpty false when any id-set has values")
    void isEmpty_hasBatchIds_returnsFalse() {
        RegenerationScope scope = RegenerationScope.builder()
                .batchIds(List.of(1L)).sectionIds(List.of()).courseIds(List.of()).build();
        assertThat(scope.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("matches by batch id")
    void matches_byBatchId_returnsTrue() {
        RegenerationScope scope = RegenerationScope.builder()
                .batchIds(List.of(12L, 13L)).sectionIds(List.of()).courseIds(List.of()).build();
        assertThat(scope.matches(session(12L, null, 500L))).isTrue();
        assertThat(scope.matches(session(99L, null, 500L))).isFalse();
    }

    @Test
    @DisplayName("matches by section id")
    void matches_bySectionId_returnsTrue() {
        RegenerationScope scope = RegenerationScope.builder()
                .batchIds(List.of()).sectionIds(List.of(7L)).courseIds(List.of()).build();
        assertThat(scope.matches(session(1L, 7L, 500L))).isTrue();
        assertThat(scope.matches(session(1L, 8L, 500L))).isFalse();
    }

    @Test
    @DisplayName("matches by course id")
    void matches_byCourseId_returnsTrue() {
        RegenerationScope scope = RegenerationScope.builder()
                .batchIds(List.of()).sectionIds(List.of()).courseIds(List.of(500L)).build();
        assertThat(scope.matches(session(1L, 2L, 500L))).isTrue();
        assertThat(scope.matches(session(1L, 2L, 999L))).isFalse();
    }

    @Test
    @DisplayName("matches true if ANY id-set matches (OR semantics)")
    void matches_anyIdSet_returnsTrue() {
        RegenerationScope scope = RegenerationScope.builder()
                .batchIds(List.of(1L)).sectionIds(List.of(7L)).courseIds(List.of(500L)).build();
        // matches on course only
        assertThat(scope.matches(session(99L, 88L, 500L))).isTrue();
    }

    @Test
    @DisplayName("null section id does not match a section scope")
    void matches_nullSectionId_returnsFalse() {
        RegenerationScope scope = RegenerationScope.builder()
                .batchIds(List.of()).sectionIds(List.of(7L)).courseIds(List.of()).build();
        assertThat(scope.matches(session(1L, null, 500L))).isFalse();
    }
}
