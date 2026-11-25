package com.tiet.banking.dao.jdbc;

import com.tiet.banking.dao.AuditEventDao;
import com.tiet.banking.dto.AuditEventView;
import com.tiet.banking.exception.PersistenceException;
import com.tiet.banking.model.AuditEvent;
import com.tiet.banking.model.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JdbcAuditEventDao implements AuditEventDao {
    private final Connection connection;

    public JdbcAuditEventDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public AuditEvent insert(AuditEvent event) {
        String sql = "INSERT INTO audit_events (actor_user_id, action, target_type, target_id, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, event.actorUserId());
            ps.setString(2, event.action());
            ps.setString(3, event.targetType());
            ps.setString(4, event.targetId());
            ps.setString(5, event.createdAt().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new PersistenceException("Failed to retrieve generated audit event ID.");
                }
                return new AuditEvent(keys.getLong(1), event.actorUserId(), event.action(),
                        event.targetType(), event.targetId(), event.createdAt());
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to insert audit event.", ex);
        }
    }

    @Override
    public List<AuditEventView> findRecent(int limit, String actionFilter) {
        boolean hasFilter = actionFilter != null && !actionFilter.isBlank();
        String sql = hasFilter
                ? """
                  SELECT e.id, e.actor_user_id, u.email AS actor_email, u.role AS actor_role,
                         e.action, e.target_type, e.target_id, e.created_at
                  FROM audit_events e
                  JOIN users u ON u.id = e.actor_user_id
                  WHERE upper(e.action) LIKE ?
                  ORDER BY e.created_at DESC, e.id DESC
                  LIMIT ?
                  """
                : """
                  SELECT e.id, e.actor_user_id, u.email AS actor_email, u.role AS actor_role,
                         e.action, e.target_type, e.target_id, e.created_at
                  FROM audit_events e
                  JOIN users u ON u.id = e.actor_user_id
                  ORDER BY e.created_at DESC, e.id DESC
                  LIMIT ?
                  """;
        List<AuditEventView> events = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (hasFilter) {
                ps.setString(1, "%" + actionFilter.trim().toUpperCase() + "%");
                ps.setInt(2, limit);
            } else {
                ps.setInt(1, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(new AuditEventView(
                            rs.getLong("id"),
                            rs.getLong("actor_user_id"),
                            rs.getString("actor_email"),
                            Role.valueOf(rs.getString("actor_role")),
                            rs.getString("action"),
                            rs.getString("target_type"),
                            rs.getString("target_id"),
                            Instant.parse(rs.getString("created_at"))));
                }
            }
            return events;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to fetch audit events.", ex);
        }
    }
}
