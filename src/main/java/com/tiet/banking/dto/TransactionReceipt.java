package com.tiet.banking.dto;

import com.tiet.banking.model.OperationType;
import java.time.Instant;

public record TransactionReceipt(
        String referenceId,
        OperationType operationType,
        String accountNumber,
        String counterpartyAccountNumber,
        long amountPaise,
        String formattedAmount,
        long balanceAfterPaise,
        String formattedBalanceAfter,
        Instant timestamp) {
}
