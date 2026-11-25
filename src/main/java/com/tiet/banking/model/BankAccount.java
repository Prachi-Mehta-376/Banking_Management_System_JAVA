package com.tiet.banking.model;

import java.time.Instant;

public record BankAccount(
        long id,
        long userId,
        String accountNumber,
        long balancePaise,
        AccountStatus status,
        Instant createdAt) {
}
