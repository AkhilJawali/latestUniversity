package com.utms.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(AuditEvent auditEvent) {
        log.debug("Publishing audit event: entityType={}, entityId={}, action={}",
                auditEvent.entityType(), auditEvent.entityId(), auditEvent.action());
        applicationEventPublisher.publishEvent(auditEvent);
    }
}
