package com.tiet.banking.dao.jdbc;

import com.tiet.banking.dao.UserDao;
import com.tiet.banking.dto.CustomerSearchResult;
import com.tiet.banking.exception.ConflictException;
import com.tiet.banking.exception.PersistenceException;
import com.tiet.banking.model.AccountStatus;
import com.tiet.banking.model.Money;
import com.tiet.banking.model.Role;
import com.tiet.banking.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcUserDao implements UserDao {
    private final Connection connection;

    public JdbcUserDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public User insert(User user) {
        String sql = """
                INSERT INTO users (full_name, email, password_hash, password_salt, password_algo, role, is_active, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.fullName());
            ps.setString(2, user.email());
            ps.setString(3, user.passwordHash());
            ps.setString(4, user.passwordSalt());
            ps.setString(5, user.passwordAlgo());
            ps.setString(6, user.role().name());
            ps.setInt(7, user.active() ? 1 : 0);
            ps.setString(8, user.createdAt().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new PersistenceException("Failed to retrieve generated user ID.");
                }
                long id = keys.getLong(1);
                return new User(id, user.fullName(), user.email(), user.passwordHash(), user.passwordSalt(),
                        user.passwordAlgo(), user.role(), user.active(), user.createdAt());
            }
        } catch (SQLException ex) {
            if (isUniqueViolation(ex)) {
                throw new ConflictException("An account with this email already exists.");
            }
            throw new PersistenceException("Failed to insert user record.", ex);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = """
                SELECT id, full_name, email, password_hash, password_salt, password_algo, role, is_active, created_at
                FROM users WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to fetch user by ID.", ex);
        }
    }

    @Override
    public Optional<User> findByEmail(String normalizedEmail) {
        String sql = """
                SELECT id, full_name, email, password_hash, password_salt, password_algo, role, is_active, created_at
                FROM users WHERE email = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalizedEmail);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to fetch user by email.", ex);
        }
    }

    @Override
    public long countByRole(Role role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to count users by role.", ex);
        }
    }

    @Override
    public List<User> findByRole(Role role) {
        String sql = """
                SELECT id, full_name, email, password_hash, password_salt, password_algo, role, is_active, created_at
                FROM users WHERE role = ? ORDER BY id ASC
                """;
        List<User> results = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapUser(rs));
                }
            }
            return results;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to list users by role.", ex);
        }
    }

    @Override
    public List<CustomerSearchResult> searchCustomers(String query) {
        String filter = query == null ? "" : query.trim().toLowerCase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        String pattern = "%" + filter + "%";
        String sql = """
                SELECT u.id AS user_id, u.full_name, u.email, u.is_active,
                       a.account_number, a.status AS account_status, a.balance_paise
                FROM users u
                JOIN accounts a ON a.user_id = u.id
                WHERE u.role = 'CUSTOMER'
                  AND (lower(u.full_name) LIKE ? ESCAPE '\\'
                    OR lower(u.email) LIKE ? ESCAPE '\\'
                    OR lower(a.account_number) LIKE ? ESCAPE '\\')
                ORDER BY u.id ASC
                LIMIT 200
                """;
        List<CustomerSearchResult> results = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long balance = rs.getLong("balance_paise");
                    results.add(new CustomerSearchResult(
                            rs.getLong("user_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getInt("is_active") == 1,
                            rs.getString("account_number"),
                            AccountStatus.valueOf(rs.getString("account_status")),
                            balance,
                            Money.formatInr(balance)));
                }
            }
            return results;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to search customer accounts.", ex);
        }
    }

    @Override
    public boolean updateActiveStatus(long userId, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to update user active status.", ex);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("password_salt"),
                rs.getString("password_algo"),
                Role.valueOf(rs.getString("role")),
                rs.getInt("is_active") == 1,
                Instant.parse(rs.getString("created_at")));
    }

    private boolean isUniqueViolation(SQLException ex) {
        String msg = ex.getMessage();
        return msg != null && msg.toUpperCase().contains("UNIQUE");
    }
}
