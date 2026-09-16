package com.utms.scheduling.engine.service;

import com.utms.scheduling.engine.config.SchedulingEngineProperties;
import com.utms.scheduling.engine.entity.GenerationRequest;
import com.utms.scheduling.engine.enums.GenerationStatus;
import com.utms.scheduling.engine.repository.GenerationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodically scans for orphaned generation requests that are stuck in IN_PROGRESS
 * beyond the configured timeout + grace period. This handles cases where the JVM crashed
 * or the async thread died without transitioning the request to a terminal state.
 *
 * Runs every 60 seconds. Transitions orphaned requests to FAILED with an explanatory message
 * and publishes an audit event for each reaped request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaleRequestReaper {

    private static final String ORPHAN_MESSAGE = "Generation orphaned (process died or JVM crash)";

    private final GenerationRequestRepository requestRepository;
    private final SchedulingEngineProperties engineProperties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Scheduled task: find and reap stale generation requests.
     * A request is considered stale if:
     *   status = IN_PROGRESS AND
     *   triggered_at < NOW() - (timeout_duration_seconds + grace_period_seconds)
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void reapStaleRequests() {
        int gracePeriodSeconds = engineProperties.getReaperGracePeriodSeconds();

        List<GenerationRequest> staleRequests = requestRepository.findStaleInProgressRequests(gracePeriodSeconds);

        if (staleRequests.isEmpty()) {
            return;
        }

        log.warn("Found {} stale generation request(s) to reap", staleRequests.size());

        for (GenerationRequest request : staleRequests) {
            request.setStatus(GenerationStatus.FAILED);
            request.setErrorMessage(ORPHAN_MESSAGE);
            request.setCompletedAt(LocalDateTime.now());
            requestRepository.save(request);

            eventPublisher.publishEvent(new StaleRequestReapedEvent(this, request.getId(),
                request.getDepartmentId(), request.getTriggeredBy()));

            log.warn("Reaped stale request: id={}, dept={}, triggeredAt={}, triggeredBy={}",
                request.getId(), request.getDepartmentId(), request.getTriggeredAt(), request.getTriggeredBy());
        }
    }

    /**
     * Audit event published when a stale request is reaped.
     */
    public static class StaleRequestReapedEvent extends org.springframework.context.ApplicationEvent {
        private final Long requestId;
        private final Long departmentId;
        private final String triggeredBy;

        public StaleRequestReapedEvent(Object source, Long requestId, Long departmentId, String triggeredBy) {
            super(source);
            this.requestId = requestId;
            this.departmentId = departmentId;
            this.triggeredBy = triggeredBy;
        }

        public Long getRequestId() { return requestId; }
        public Long getDepartmentId() { return departmentId; }
        public String getTriggeredBy() { return triggeredBy; }
    }
}
