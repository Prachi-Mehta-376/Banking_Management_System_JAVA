package com.tiet.banking.model;

import java.time.Instant;

public record User(
        long id,
        String fullName,
        String email,
        String passwordHash,
        String passwordSalt,
        String passwordAlgo,
        Role role,
        boolean active,
        Instant createdAt) {
}
