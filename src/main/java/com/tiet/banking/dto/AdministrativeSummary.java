package com.tiet.banking.dto;

public record AdministrativeSummary(
        long totalCustomers,
        long totalActiveStaff,
        long totalAccounts,
        long frozenAccounts,
        long totalOperationsCount,
        long totalSystemBalancePaise,
        String formattedTotalBalance) {
}
