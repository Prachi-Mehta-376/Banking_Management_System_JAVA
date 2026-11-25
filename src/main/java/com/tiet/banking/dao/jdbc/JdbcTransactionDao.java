package com.tiet.banking.dao.jdbc;

import com.tiet.banking.dao.TransactionDao;
import com.tiet.banking.dto.TransactionView;
import com.tiet.banking.exception.PersistenceException;
import com.tiet.banking.model.BankTransaction;
import com.tiet.banking.model.Money;
import com.tiet.banking.model.OperationType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JdbcTransactionDao implements TransactionDao {
    private final Connection connection;

    public JdbcTransactionDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public BankTransaction insert(BankTransaction transaction) {
        String sql = """
                INSERT INTO transactions (operation_id, account_id, delta_paise, balance_after_paise, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, transaction.operationId());
            ps.setLong(2, transaction.accountId());
            ps.setLong(3, transaction.deltaPaise());
            ps.setLong(4, transaction.balanceAfterPaise());
            ps.setString(5, transaction.createdAt().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new PersistenceException("Failed to retrieve generated transaction ID.");
                }
                return new BankTransaction(keys.getLong(1), transaction.operationId(), transaction.accountId(),
                        transaction.deltaPaise(), transaction.balanceAfterPaise(), transaction.createdAt());
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to insert transaction journal entry.", ex);
        }
    }

    @Override
    public List<TransactionView> findViewsByAccountId(long accountId, int limit) {
        String sql = """
                SELECT o.reference_id, o.operation_type, t.delta_paise, t.balance_after_paise, u.role AS actor_role, t.created_at
                FROM transactions t
                JOIN operations o ON o.id = t.operation_id
                JOIN users u ON u.id = o.performed_by
                WHERE t.account_id = ?
                ORDER BY t.created_at DESC, t.id DESC
                LIMIT ?
                """;
        List<TransactionView> views = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long delta = rs.getLong("delta_paise");
                    long balanceAfter = rs.getLong("balance_after_paise");
                    views.add(new TransactionView(
                            rs.getString("reference_id"),
                            OperationType.valueOf(rs.getString("operation_type")),
                            delta,
                            Money.formatSignedInr(delta),
                            balanceAfter,
                            Money.formatInr(balanceAfter),
                            rs.getString("actor_role"),
                            Instant.parse(rs.getString("created_at"))));
                }
            }
            return views;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to query account transaction history.", ex);
        }
    }

    @Override
    public List<BankTransaction> findByAccountIdAsc(long accountId) {
        String sql = """
                SELECT id, operation_id, account_id, delta_paise, balance_after_paise, created_at
                FROM transactions
                WHERE account_id = ?
                ORDER BY id ASC
                """;
        List<BankTransaction> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to fetch account journal entries.", ex);
        }
    }

    @Override
    public List<BankTransaction> findByOperationId(long operationId) {
        String sql = """
                SELECT id, operation_id, account_id, delta_paise, balance_after_paise, created_at
                FROM transactions
                WHERE operation_id = ?
                ORDER BY id ASC
                """;
        List<BankTransaction> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, operationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to fetch operation journal entries.", ex);
        }
    }

    @Override
    public long sumDeltasByAccountId(long accountId) {
        String sql = "SELECT COALESCE(SUM(delta_paise), 0) FROM transactions WHERE account_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to sum transaction deltas.", ex);
        }
    }

    private BankTransaction mapTransaction(ResultSet rs) throws SQLException {
        return new BankTransaction(
                rs.getLong("id"),
                rs.getLong("operation_id"),
                rs.getLong("account_id"),
                rs.getLong("delta_paise"),
                rs.getLong("balance_after_paise"),
                Instant.parse(rs.getString("created_at")));
    }
}
