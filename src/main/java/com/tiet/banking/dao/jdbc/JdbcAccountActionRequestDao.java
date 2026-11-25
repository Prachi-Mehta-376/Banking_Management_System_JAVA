package com.tiet.banking.dao.jdbc;

import com.tiet.banking.dao.AccountActionRequestDao;
import com.tiet.banking.dto.AccountActionRequestView;
import com.tiet.banking.exception.ConflictException;
import com.tiet.banking.exception.PersistenceException;
import com.tiet.banking.model.AccountActionRequest;
import com.tiet.banking.model.AccountActionType;
import com.tiet.banking.model.RequestStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAccountActionRequestDao implements AccountActionRequestDao {
    private final Connection connection;

    public JdbcAccountActionRequestDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public AccountActionRequest insert(AccountActionRequest request) {
        String sql = """
                INSERT INTO account_action_requests (account_id, action_type, reason, requested_by, status, created_at)
                VALUES (?, ?, ?, ?, 'PENDING', ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, request.accountId());
            ps.setString(2, request.actionType().name());
            ps.setString(3, request.reason());
            ps.setLong(4, request.requestedByUserId());
            ps.setString(5, request.createdAt().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new PersistenceException("Failed to retrieve generated request ID.");
                }
                return new AccountActionRequest(keys.getLong(1), request.accountId(), request.actionType(),
                        request.reason(), request.requestedByUserId(), RequestStatus.PENDING, null,
                        request.createdAt(), null);
            }
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().toUpperCase().contains("UNIQUE")) {
                throw new ConflictException("A pending freeze/unfreeze request already exists for this account.");
            }
            throw new PersistenceException("Failed to insert account action request.", ex);
        }
    }

    @Override
    public Optional<AccountActionRequest> findById(long requestId) {
        String sql = """
                SELECT id, account_id, action_type, reason, requested_by, status, reviewed_by, created_at, reviewed_at
                FROM account_action_requests WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Long reviewedBy = rs.getObject("reviewed_by") == null ? null : rs.getLong("reviewed_by");
                String reviewedAtRaw = rs.getString("reviewed_at");
                Instant reviewedAt = reviewedAtRaw == null ? null : Instant.parse(reviewedAtRaw);
                return Optional.of(new AccountActionRequest(
                        rs.getLong("id"),
                        rs.getLong("account_id"),
                        AccountActionType.valueOf(rs.getString("action_type")),
                        rs.getString("reason"),
                        rs.getLong("requested_by"),
                        RequestStatus.valueOf(rs.getString("status")),
                        reviewedBy,
                        Instant.parse(rs.getString("created_at")),
                        reviewedAt));
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to load account action request.", ex);
        }
    }

    @Override
    public boolean hasPendingRequestForAccount(long accountId) {
        String sql = "SELECT COUNT(*) FROM account_action_requests WHERE account_id = ? AND status = 'PENDING'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to check pending requests.", ex);
        }
    }

    @Override
    public List<AccountActionRequestView> findPendingViews() {
        return queryViews("WHERE r.status = 'PENDING'", 200);
    }

    @Override
    public List<AccountActionRequestView> findAllViews(int limit) {
        return queryViews("", limit);
    }

    private List<AccountActionRequestView> queryViews(String whereClause, int limit) {
        String sql = """
                SELECT r.id AS request_id, a.account_number, owner.full_name AS owner_name,
                       r.action_type, r.reason, req_user.email AS requested_by_email,
                       r.status, rev_user.email AS reviewed_by_email, r.created_at, r.reviewed_at
                FROM account_action_requests r
                JOIN accounts a ON a.id = r.account_id
                JOIN users owner ON owner.id = a.user_id
                JOIN users req_user ON req_user.id = r.requested_by
                LEFT JOIN users rev_user ON rev_user.id = r.reviewed_by
                """ + whereClause + """
                 ORDER BY r.created_at DESC, r.id DESC
                LIMIT ?
                """;
        List<AccountActionRequestView> views = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String revAtRaw = rs.getString("reviewed_at");
                    views.add(new AccountActionRequestView(
                            rs.getLong("request_id"),
                            rs.getString("account_number"),
                            rs.getString("owner_name"),
                            AccountActionType.valueOf(rs.getString("action_type")),
                            rs.getString("reason"),
                            rs.getString("requested_by_email"),
                            RequestStatus.valueOf(rs.getString("status")),
                            rs.getString("reviewed_by_email"),
                            Instant.parse(rs.getString("created_at")),
                            revAtRaw == null ? null : Instant.parse(revAtRaw)));
                }
            }
            return views;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to list account action requests.", ex);
        }
    }

    @Override
    public boolean updateDecisionIfPending(long requestId, RequestStatus status, long reviewedByUserId, Instant reviewedAt) {
        String sql = """
                UPDATE account_action_requests
                SET status = ?, reviewed_by = ?, reviewed_at = ?
                WHERE id = ? AND status = 'PENDING' AND requested_by != ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, reviewedByUserId);
            ps.setString(3, reviewedAt.toString());
            ps.setLong(4, requestId);
            ps.setLong(5, reviewedByUserId);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to record request review decision.", ex);
        }
    }
}
