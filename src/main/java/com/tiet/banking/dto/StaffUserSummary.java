package com.tiet.banking.dto;

import java.time.Instant;

public record StaffUserSummary(
        long userId,
        String fullName,
        String email,
        boolean active,
        Instant createdAt) {
}
