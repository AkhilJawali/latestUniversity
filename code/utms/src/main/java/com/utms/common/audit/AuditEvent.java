package com.utms.common.audit;

import java.time.Instant;

public record AuditEvent(
        String entityType,
        Long entityId,
        Action action,
        Object previousValue,
        Object newValue,
        String userId,
        Instant timestamp
) {
    public enum Action {
        CREATED,
        UPDATED,
        DELETED
    }
}
