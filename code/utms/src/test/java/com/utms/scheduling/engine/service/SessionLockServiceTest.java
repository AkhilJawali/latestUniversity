package com.utms.scheduling.engine.service;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.repository.ScheduledSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SessionLockService (A4-14, FR-1). Verifies lock/unlock behavior,
 * placement validation, draft-ownership checks, and audit emission.
 */
@ExtendWith(MockitoExtension.class)
class SessionLockServiceTest {

    @Mock
    private ScheduledSessionRepository sessionRepository;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private SessionLockService service;

    private ScheduledSession placedSession(Long id, Long draftId) {
        ScheduledSession s = new ScheduledSession();
        s.setId(id);
        s.setDraftId(draftId);
        s.setFacultyId(10L);
        s.setBatchId(20L);
        s.setRoomId(30L);
        s.setDayOfWeek("MONDAY");
        s.setSlotDefinitionId(40L);
        s.setSessionType("LECTURE");
        s.setIsLocked(false);
        return s;
    }

    @Test
    void lock_validSession_setsLockedAndAudits() {
        ScheduledSession s = placedSession(1L, 17L);
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));
        when(sessionRepository.save(any(ScheduledSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ScheduledSession result = service.lock(17L, 1L, "coordinator");

        assertTrue(result.getIsLocked());
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).publish(captor.capture());
        assertEquals(AuditEvent.Action.UPDATED, captor.getValue().action());
        assertEquals("ScheduledSession", captor.getValue().entityType());
    }

    @Test
    void lock_sessionNotFound_throwsEntityNotFound() {
        when(sessionRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.lock(17L, 99L, "coordinator"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void lock_sessionInDifferentDraft_throwsEntityNotFound() {
        ScheduledSession s = placedSession(1L, 17L);
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));

        // Request targets draft 999, but the session belongs to draft 17
        assertThrows(EntityNotFoundException.class, () -> service.lock(999L, 1L, "coordinator"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void lock_incompletePlacement_throwsBusinessRuleViolation() {
        ScheduledSession s = placedSession(1L, 17L);
        s.setRoomId(null); // incomplete placement
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));

        assertThrows(BusinessRuleViolationException.class, () -> service.lock(17L, 1L, "coordinator"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void unlock_lockedSession_clearsLockedAndAudits() {
        ScheduledSession s = placedSession(1L, 17L);
        s.setIsLocked(true);
        when(sessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(s));
        when(sessionRepository.save(any(ScheduledSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ScheduledSession result = service.unlock(17L, 1L, "coordinator");

        assertFalse(result.getIsLocked());
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).publish(captor.capture());
        assertEquals(AuditEvent.Action.UPDATED, captor.getValue().action());
    }

    @Test
    void unlock_sessionNotFound_throwsEntityNotFound() {
        when(sessionRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.unlock(17L, 99L, "coordinator"));
        verify(sessionRepository, never()).save(any());
    }
}
