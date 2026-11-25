package com.tiet.banking.model;

import java.time.Instant;

public record AuditEvent(
        long id,
        long actorUserId,
        String action,
        String targetType,
        String targetId,
        Instant createdAt) {
}
