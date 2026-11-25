package com.tiet.banking.dto;

import com.tiet.banking.model.Role;

public record UserSession(
        long userId,
        String fullName,
        String email,
        Role role,
        Long accountId,
        String accountNumber) {
}
