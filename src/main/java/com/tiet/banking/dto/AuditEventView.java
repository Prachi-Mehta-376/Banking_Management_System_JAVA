package com.tiet.banking.dto;

import com.tiet.banking.model.Role;
import java.time.Instant;

public record AuditEventView(
        long auditId,
        long actorUserId,
        String actorEmail,
        Role actorRole,
        String action,
        String targetType,
        String targetId,
        Instant timestamp) {
}
