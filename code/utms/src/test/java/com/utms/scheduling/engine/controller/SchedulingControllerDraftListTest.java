package com.utms.scheduling.engine.controller;

import com.utms.approval.service.CurrentUserProvider;
import com.utms.publication.service.PublicationService;
import com.utms.scheduling.engine.dto.TimetableDraftDto;
import com.utms.scheduling.engine.entity.SoftConstraintViolation;
import com.utms.scheduling.engine.entity.TimetableDraft;
import com.utms.scheduling.engine.enums.DraftStatus;
import com.utms.scheduling.engine.repository.InfeasibilityReportRepository;
import com.utms.scheduling.engine.repository.ScheduledSessionRepository;
import com.utms.scheduling.engine.repository.SoftConstraintViolationRepository;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;
import com.utms.scheduling.engine.repository.UnplacedSessionRepository;
import com.utms.scheduling.engine.service.SchedulingEngineService;
import com.utms.scheduling.engine.service.SessionLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the draft list endpoint used by the approvals screen, and the shared
 * draft DTO mapping reused by the single-draft endpoint.
 */
@ExtendWith(MockitoExtension.class)
class SchedulingControllerDraftListTest {

    @Mock private SchedulingEngineService engineService;
    @Mock private SessionLockService sessionLockService;
    @Mock private PublicationService publicationService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private TimetableDraftRepository draftRepository;
    @Mock private ScheduledSessionRepository sessionRepository;
    @Mock private SoftConstraintViolationRepository violationRepository;
    @Mock private InfeasibilityReportRepository infeasibilityReportRepository;
    @Mock private UnplacedSessionRepository unplacedSessionRepository;

    @InjectMocks
    private SchedulingController controller;

    private TimetableDraft draft(Long id, int version, DraftStatus status) {
        TimetableDraft d = new TimetableDraft();
        d.setId(id);
        d.setDepartmentId(1L);
        d.setSemester("ODD");
        d.setAcademicYear("2024-25");
        d.setVersion(version);
        d.setStatus(status);
        d.setTotalSessionsRequired(34);
        d.setTotalSessionsPlaced(30);
        return d;
    }

    @Test
    void listDrafts_departmentWithDrafts_returnsDraftsNewestFirstWithStatus() {
        when(draftRepository.findByDepartmentIdAndDeletedAtIsNullOrderByIdDesc(1L))
            .thenReturn(List.of(draft(12L, 2, DraftStatus.UNDER_REVIEW), draft(9L, 1, DraftStatus.SUPERSEDED)));

        List<TimetableDraftDto> body = controller.listDrafts(1L).getBody();

        assertThat(body).extracting(TimetableDraftDto::getId).containsExactly(12L, 9L);
        TimetableDraftDto newest = body.get(0);
        assertThat(newest.getStatus()).isEqualTo("UNDER_REVIEW");
        assertThat(newest.getVersion()).isEqualTo(2);
        assertThat(newest.getSemester()).isEqualTo("ODD");
        assertThat(newest.getAcademicYear()).isEqualTo("2024-25");
        assertThat(newest.getTotalSessionsPlaced()).isEqualTo(30);
        assertThat(newest.getViolationCount()).isNull();
        verifyNoInteractions(violationRepository);
    }

    @Test
    void listDrafts_departmentWithoutDrafts_returnsEmptyList() {
        when(draftRepository.findByDepartmentIdAndDeletedAtIsNullOrderByIdDesc(5L)).thenReturn(List.of());

        assertThat(controller.listDrafts(5L).getBody()).isEmpty();
    }

    @Test
    void getDraft_existingDraft_includesViolationCount() {
        when(draftRepository.findByIdAndDeletedAtIsNull(12L))
            .thenReturn(Optional.of(draft(12L, 2, DraftStatus.DRAFT)));
        when(violationRepository.findByDraftIdAndDeletedAtIsNull(12L))
            .thenReturn(List.of(new SoftConstraintViolation(), new SoftConstraintViolation()));

        TimetableDraftDto dto = controller.getDraft(12L).getBody();

        assertThat(dto.getStatus()).isEqualTo("DRAFT");
        assertThat(dto.getViolationCount()).isEqualTo(2);
    }
}
