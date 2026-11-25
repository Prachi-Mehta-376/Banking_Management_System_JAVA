package com.tiet.banking.dto;

import com.tiet.banking.model.OperationType;
import java.time.Instant;

public record TransactionView(
        String referenceId,
        OperationType operationType,
        long deltaPaise,
        String formattedDelta,
        long balanceAfterPaise,
        String formattedBalanceAfter,
        String performedByRole,
        Instant timestamp) {
}
