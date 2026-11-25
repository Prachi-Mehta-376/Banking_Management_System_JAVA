package com.tiet.banking.dto;

import com.tiet.banking.model.AccountStatus;

public record CustomerSearchResult(
        long userId,
        String fullName,
        String email,
        boolean userActive,
        String accountNumber,
        AccountStatus accountStatus,
        long balancePaise,
        String formattedBalance) {
}
