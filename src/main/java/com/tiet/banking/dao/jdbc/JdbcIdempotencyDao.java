package com.tiet.banking.dao.jdbc;

import com.tiet.banking.dao.IdempotencyDao;
import com.tiet.banking.exception.ConflictException;
import com.tiet.banking.exception.PersistenceException;
import com.tiet.banking.model.IdempotencyRecord;
import com.tiet.banking.model.OperationType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;

public class JdbcIdempotencyDao implements IdempotencyDao {
    private final Connection connection;

    public JdbcIdempotencyDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<IdempotencyRecord> findByUserAndKey(long userId, String idempotencyKey) {
        String sql = """
                SELECT id, user_id, idempotency_key, operation_type, request_signature, reference_id,
                       primary_account_id, counterparty_account_number, amount_paise, balance_after_paise, created_at
                FROM idempotency_records
                WHERE user_id = ? AND idempotency_key = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, idempotencyKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new IdempotencyRecord(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("idempotency_key"),
                        OperationType.valueOf(rs.getString("operation_type")),
                        rs.getString("request_signature"),
                        rs.getString("reference_id"),
                        rs.getLong("primary_account_id"),
                        rs.getString("counterparty_account_number"),
                        rs.getLong("amount_paise"),
                        rs.getLong("balance_after_paise"),
                        Instant.parse(rs.getString("created_at"))));
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to look up idempotency key.", ex);
        }
    }

    @Override
    public IdempotencyRecord insert(IdempotencyRecord record) {
        String sql = """
                INSERT INTO idempotency_records (
                    user_id, idempotency_key, operation_type, request_signature, reference_id,
                    primary_account_id, counterparty_account_number, amount_paise, balance_after_paise, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, record.userId());
            ps.setString(2, record.idempotencyKey());
            ps.setString(3, record.operationType().name());
            ps.setString(4, record.requestSignature());
            ps.setString(5, record.referenceId());
            ps.setLong(6, record.primaryAccountId());
            ps.setString(7, record.counterpartyAccountNumber());
            ps.setLong(8, record.amountPaise());
            ps.setLong(9, record.balanceAfterPaise());
            ps.setString(10, record.createdAt().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new PersistenceException("Failed to retrieve generated idempotency record ID.");
                }
                return new IdempotencyRecord(keys.getLong(1), record.userId(), record.idempotencyKey(),
                        record.operationType(), record.requestSignature(), record.referenceId(),
                        record.primaryAccountId(), record.counterpartyAccountNumber(), record.amountPaise(),
                        record.balanceAfterPaise(), record.createdAt());
            }
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().toUpperCase().contains("UNIQUE")) {
                throw new ConflictException("Duplicate idempotency key detected.");
            }
            throw new PersistenceException("Failed to store idempotency record.", ex);
        }
    }
}
