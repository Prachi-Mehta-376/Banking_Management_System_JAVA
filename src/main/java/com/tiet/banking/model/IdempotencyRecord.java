package com.tiet.banking.model;

import java.time.Instant;

public record IdempotencyRecord(
        long id,
        long userId,
        String idempotencyKey,
        OperationType operationType,
        String requestSignature,
        String referenceId,
        long primaryAccountId,
        String counterpartyAccountNumber,
        long amountPaise,
        long balanceAfterPaise,
        Instant createdAt) {
}
