package com.tiet.banking.dto;

import com.tiet.banking.model.AccountActionType;
import com.tiet.banking.model.RequestStatus;
import java.time.Instant;

public record AccountActionRequestView(
        long requestId,
        String accountNumber,
        String ownerName,
        AccountActionType actionType,
        String reason,
        String requestedByEmail,
        RequestStatus status,
        String reviewedByEmail,
        Instant createdAt,
        Instant reviewedAt) {
}
