package com.tiet.banking.dto;

import com.tiet.banking.model.AccountStatus;
import java.time.Instant;

public record AccountSummary(
        long accountId,
        String accountNumber,
        long ownerUserId,
        String ownerName,
        String ownerEmail,
        long balancePaise,
        String formattedBalance,
        AccountStatus status,
        Instant createdAt) {
}
