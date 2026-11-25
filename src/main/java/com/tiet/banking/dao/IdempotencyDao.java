package com.tiet.banking.dao;

import com.tiet.banking.model.IdempotencyRecord;
import java.util.Optional;

public interface IdempotencyDao {
    Optional<IdempotencyRecord> findByUserAndKey(long userId, String idempotencyKey);

    IdempotencyRecord insert(IdempotencyRecord record);
}
