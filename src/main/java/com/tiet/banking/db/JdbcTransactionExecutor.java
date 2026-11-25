package com.tiet.banking.db;

import com.tiet.banking.dao.AccountActionRequestDao;
import com.tiet.banking.dao.AccountDao;
import com.tiet.banking.dao.AuditEventDao;
import com.tiet.banking.dao.IdempotencyDao;
import com.tiet.banking.dao.OperationDao;
import com.tiet.banking.dao.TransactionDao;
import com.tiet.banking.dao.UserDao;
import com.tiet.banking.dao.jdbc.JdbcAccountActionRequestDao;
import com.tiet.banking.dao.jdbc.JdbcAccountDao;
import com.tiet.banking.dao.jdbc.JdbcAuditEventDao;
import com.tiet.banking.dao.jdbc.JdbcIdempotencyDao;
import com.tiet.banking.dao.jdbc.JdbcOperationDao;
import com.tiet.banking.dao.jdbc.JdbcTransactionDao;
import com.tiet.banking.dao.jdbc.JdbcUserDao;
import com.tiet.banking.exception.BankingException;
import com.tiet.banking.exception.CommitOutcomeUncertainException;
import com.tiet.banking.exception.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;

public class JdbcTransactionExecutor implements TransactionExecutor {
    private final DatabaseManager databaseManager;

    public JdbcTransactionExecutor(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public <T> T execute(TransactionWork<T> work) {
        Connection connection;
        try {
            connection = databaseManager.openWriteConnection();
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to open database connection.", ex);
        }

        try (connection) {
            connection.setAutoCommit(false);
            JdbcUnitOfWork uow = new JdbcUnitOfWork(connection);
            T result;
            try {
                result = work.execute(uow);
            } catch (RuntimeException ex) {
                rollbackQuietly(connection, ex);
                throw ex;
            }

            try {
                connection.commit();
                return result;
            } catch (SQLException commitEx) {
                // Do not roll back or retry blindly when commit itself fails; outcome may already be persisted.
                throw new CommitOutcomeUncertainException(
                        "Transaction commit outcome is uncertain. Inspect transaction history before retrying.",
                        uow.activeOperationReference(),
                        commitEx);
            }
        } catch (BankingException ex) {
            throw ex;
        } catch (SQLException ex) {
            throw new PersistenceException("Database transaction failed.", ex);
        }
    }

    @Override
    public <T> T executeReadOnly(TransactionWork<T> work) {
        Connection connection;
        try {
            connection = databaseManager.openReadConnection();
        } catch (SQLException ex) {
            throw new PersistenceException("Failed to open read-only database connection.", ex);
        }

        try (connection) {
            connection.setAutoCommit(false);
            JdbcUnitOfWork uow = new JdbcUnitOfWork(connection);
            T result = work.execute(uow);
            connection.rollback();
            return result;
        } catch (BankingException ex) {
            throw ex;
        } catch (SQLException ex) {
            throw new PersistenceException("Read transaction failed.", ex);
        }
    }

    private void rollbackQuietly(Connection connection, RuntimeException primary) {
        try {
            connection.rollback();
        } catch (SQLException rollbackEx) {
            primary.addSuppressed(rollbackEx);
        }
    }

    private static final class JdbcUnitOfWork implements BankingUnitOfWork {
        private final UserDao userDao;
        private final AccountDao accountDao;
        private final OperationDao operationDao;
        private final TransactionDao transactionDao;
        private final AuditEventDao auditEventDao;
        private final AccountActionRequestDao actionRequestDao;
        private final IdempotencyDao idempotencyDao;
        private String activeReferenceId;

        private JdbcUnitOfWork(Connection connection) {
            this.userDao = new JdbcUserDao(connection);
            this.accountDao = new JdbcAccountDao(connection);
            this.operationDao = new JdbcOperationDao(connection);
            this.transactionDao = new JdbcTransactionDao(connection);
            this.auditEventDao = new JdbcAuditEventDao(connection);
            this.actionRequestDao = new JdbcAccountActionRequestDao(connection);
            this.idempotencyDao = new JdbcIdempotencyDao(connection);
        }

        @Override
        public UserDao users() {
            return userDao;
        }

        @Override
        public AccountDao accounts() {
            return accountDao;
        }

        @Override
        public OperationDao operations() {
            return operationDao;
        }

        @Override
        public TransactionDao transactions() {
            return transactionDao;
        }

        @Override
        public AuditEventDao auditEvents() {
            return auditEventDao;
        }

        @Override
        public AccountActionRequestDao accountActionRequests() {
            return actionRequestDao;
        }

        @Override
        public IdempotencyDao idempotencyRecords() {
            return idempotencyDao;
        }

        @Override
        public void setActiveOperationReference(String referenceId) {
            this.activeReferenceId = referenceId;
        }

        private String activeOperationReference() {
            return activeReferenceId;
        }
    }
}
