package com.tiet.banking.db;

import com.tiet.banking.dao.AccountActionRequestDao;
import com.tiet.banking.dao.AccountDao;
import com.tiet.banking.dao.AuditEventDao;
import com.tiet.banking.dao.IdempotencyDao;
import com.tiet.banking.dao.OperationDao;
import com.tiet.banking.dao.TransactionDao;
import com.tiet.banking.dao.UserDao;

public interface BankingUnitOfWork {
    UserDao users();

    AccountDao accounts();

    OperationDao operations();

    TransactionDao transactions();

    AuditEventDao auditEvents();

    AccountActionRequestDao accountActionRequests();

    IdempotencyDao idempotencyRecords();

    void setActiveOperationReference(String referenceId);
}
