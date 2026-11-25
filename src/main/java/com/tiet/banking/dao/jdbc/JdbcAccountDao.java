package com.tiet.banking.dao.jdbc;

import com.tiet.banking.dao.AccountDao;
import com.tiet.banking.dto.AdministrativeSummary;
import com.tiet.banking.exception.PersistenceException;
import com.tiet.banking.model.AccountStatus;
import com.tiet.banking.model.BankAccount;
import com.tiet.banking.model.Money;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcAccountDao implements AccountDao {
    private final Connection connection;

    public JdbcAccountDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public BankAccount createAccount(long userId, Instant createdAt) {
        String insertSql = """
                INSERT INTO accounts (user_id, account_number, balance_paise, status, created_at)
                VALUES (?, ?, 0, 'ACTIVE', ?)
                """;
        String updateNumberSql = "UPDATE accounts SET account_number = ? WHERE id = ?";
        // Temporary unique placeholder satisfies NOT NULL UNIQUE until SQLite assigns the row ID.
        String tempPlaceholder = "TMP-" + UUID.randomUUID();
        try (PreparedStatement insertPs = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            insertPs.setLong(1, userId);
            insertPs.setString(2, tempPlaceholder);
            insertPs.setString(3, createdAt.toString());
            insertPs.executeUpdate();

            long id;
            try (ResultSet keys = insertPs.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new PersistenceException("Failed to retrieve generated account ID.");
                }
                id = keys.getLong(1);
            }

            String accountNumber = String.format("AC%06d", id);
            try (PreparedStatement updatePs = connection.prepareStatement(updateNumberSql)) {
                updatePs.setString(1, accountNumber);
                updatePs.setLong(2, id);
                if (updatePs.executeUpdate() != 1) {
                    throw new PersistenceException("Failed to assign generated account number.");
                }
            }
            return new BankAccount(id, userId, accountNumber, 0L, AccountStatus.ACTIVE, createdAt);
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to create customer bank account.", ex);
        }
    }

    @Override
    public Optional<BankAccount> findById(long accountId) {
        String sql = "SELECT id, user_id, account_number, balance_paise, status, created_at FROM accounts WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapAccount(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to fetch account by ID.", ex);
        }
    }

    @Override
    public Optional<BankAccount> findByUserId(long userId) {
        String sql = "SELECT id, user_id, account_number, balance_paise, status, created_at FROM accounts WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapAccount(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to fetch account by user ID.", ex);
        }
    }

    @Override
    public Optional<BankAccount> findByAccountNumber(String accountNumber) {
        String sql = """
                SELECT id, user_id, account_number, balance_paise, status, created_at
                FROM accounts WHERE account_number = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapAccount(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to fetch account by account number.", ex);
        }
    }

    @Override
    public List<BankAccount> findAllOrderById() {
        String sql = "SELECT id, user_id, account_number, balance_paise, status, created_at FROM accounts ORDER BY id ASC";
        List<BankAccount> accounts = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                accounts.add(mapAccount(rs));
            }
            return accounts;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to list bank accounts.", ex);
        }
    }

    @Override
    public boolean creditIfActive(long accountId, long positiveDeltaPaise) {
        String sql = "UPDATE accounts SET balance_paise = balance_paise + ? WHERE id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, positiveDeltaPaise);
            ps.setLong(2, accountId);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to credit account balance.", ex);
        }
    }

    @Override
    public boolean debitIfActiveAndSufficient(long accountId, long positiveDebitPaise) {
        String sql = """
                UPDATE accounts
                SET balance_paise = balance_paise - ?
                WHERE id = ? AND status = 'ACTIVE' AND balance_paise >= ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, positiveDebitPaise);
            ps.setLong(2, accountId);
            ps.setLong(3, positiveDebitPaise);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to debit account balance.", ex);
        }
    }

    @Override
    public boolean updateStatus(long accountId, AccountStatus status) {
        String sql = "UPDATE accounts SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, accountId);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to update account status.", ex);
        }
    }

    @Override
    public AdministrativeSummary getAdministrativeSummary() {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER') AS total_customers,
                    (SELECT COUNT(*) FROM users WHERE role = 'STAFF' AND is_active = 1) AS total_active_staff,
                    (SELECT COUNT(*) FROM accounts) AS total_accounts,
                    (SELECT COUNT(*) FROM accounts WHERE status = 'FROZEN') AS frozen_accounts,
                    (SELECT COUNT(*) FROM operations) AS total_operations,
                    (SELECT COALESCE(SUM(balance_paise), 0) FROM accounts) AS total_balance_paise
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return new AdministrativeSummary(0, 0, 0, 0, 0, 0, Money.formatInr(0));
            }
            long totalBalance = rs.getLong("total_balance_paise");
            return new AdministrativeSummary(
                    rs.getLong("total_customers"),
                    rs.getLong("total_active_staff"),
                    rs.getLong("total_accounts"),
                    rs.getLong("frozen_accounts"),
                    rs.getLong("total_operations"),
                    totalBalance,
                    Money.formatInr(totalBalance));
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to query administrative summary.", ex);
        }
    }

    private BankAccount mapAccount(ResultSet rs) throws SQLException {
        return new BankAccount(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("account_number"),
                rs.getLong("balance_paise"),
                AccountStatus.valueOf(rs.getString("status")),
                Instant.parse(rs.getString("created_at")));
    }
}
