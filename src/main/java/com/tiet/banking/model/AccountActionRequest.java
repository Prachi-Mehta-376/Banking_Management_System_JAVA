package com.tiet.banking.model;

import java.time.Instant;

public record AccountActionRequest(
        long id,
        long accountId,
        AccountActionType actionType,
        String reason,
        long requestedByUserId,
        RequestStatus status,
        Long reviewedByUserId,
        Instant createdAt,
        Instant reviewedAt) {
}
