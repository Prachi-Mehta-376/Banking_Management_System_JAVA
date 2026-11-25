package com.tiet.banking.model;

import java.time.Instant;

public record BankTransaction(
        long id,
        long operationId,
        long accountId,
        long deltaPaise,
        long balanceAfterPaise,
        Instant createdAt) {
}
