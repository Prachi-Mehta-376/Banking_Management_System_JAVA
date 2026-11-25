package com.tiet.banking.dao.jdbc;

import com.tiet.banking.dao.OperationDao;
import com.tiet.banking.exception.PersistenceException;
import com.tiet.banking.model.BankOperation;
import com.tiet.banking.model.OperationType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcOperationDao implements OperationDao {
    private final Connection connection;

    public JdbcOperationDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public BankOperation insert(BankOperation operation) {
        String sql = "INSERT INTO operations (reference_id, operation_type, performed_by, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, operation.referenceId());
            ps.setString(2, operation.operationType().name());
            ps.setLong(3, operation.performedByUserId());
            ps.setString(4, operation.createdAt().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new PersistenceException("Failed to retrieve generated operation ID.");
                }
                return new BankOperation(keys.getLong(1), operation.referenceId(), operation.operationType(),
                        operation.performedByUserId(), operation.createdAt());
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to insert operation record.", ex);
        }
    }

    @Override
    public Optional<BankOperation> findByReferenceId(String referenceId) {
        String sql = "SELECT id, reference_id, operation_type, performed_by, created_at FROM operations WHERE reference_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, referenceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapOperation(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to find operation by reference ID.", ex);
        }
    }

    @Override
    public List<BankOperation> findAllOrderById() {
        String sql = "SELECT id, reference_id, operation_type, performed_by, created_at FROM operations ORDER BY id ASC";
        List<BankOperation> ops = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ops.add(mapOperation(rs));
            }
            return ops;
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to list operations.", ex);
        }
    }

    private BankOperation mapOperation(ResultSet rs) throws SQLException {
        return new BankOperation(
                rs.getLong("id"),
                rs.getString("reference_id"),
                OperationType.valueOf(rs.getString("operation_type")),
                rs.getLong("performed_by"),
                Instant.parse(rs.getString("created_at")));
    }
}
