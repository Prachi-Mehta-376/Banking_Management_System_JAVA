package com.tiet.banking.model;

import java.time.Instant;

public record BankOperation(
        long id,
        String referenceId,
        OperationType operationType,
        long performedByUserId,
        Instant createdAt) {
}
