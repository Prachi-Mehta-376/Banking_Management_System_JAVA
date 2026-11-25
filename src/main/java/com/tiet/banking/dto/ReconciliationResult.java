package com.tiet.banking.dto;

public record ReconciliationResult(
        long accountId,
        String accountNumber,
        long storedBalancePaise,
        long expectedBalancePaise,
        long discrepancyPaise,
        boolean matched,
        String details) {
}
