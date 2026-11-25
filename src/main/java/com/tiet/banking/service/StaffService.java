package com.tiet.banking.service;

import com.tiet.banking.db.TransactionExecutor;
import com.tiet.banking.dto.AccountSummary;
import com.tiet.banking.dto.CustomerSearchResult;
import com.tiet.banking.dto.TransactionView;
import com.tiet.banking.exception.ResourceNotFoundException;
import com.tiet.banking.model.BankAccount;
import com.tiet.banking.model.Money;
import com.tiet.banking.model.Role;
import com.tiet.banking.model.User;
import com.tiet.banking.security.SessionManager;
import com.tiet.banking.util.InputValidator;
import java.util.List;

public class StaffService {
    private final TransactionExecutor txExecutor;
    private final SessionManager sessionManager;

    public StaffService(TransactionExecutor txExecutor, SessionManager sessionManager) {
        this.txExecutor = txExecutor;
        this.sessionManager = sessionManager;
    }

    public List<CustomerSearchResult> searchCustomers(String query) {
        sessionManager.requireRole(Role.STAFF, Role.ADMIN);
        return txExecutor.executeReadOnly(uow -> uow.users().searchCustomers(query));
    }

    public AccountSummary getCustomerAccountSummary(String accountNumber) {
        sessionManager.requireRole(Role.STAFF, Role.ADMIN);
        String cleanAccountNumber = InputValidator.normalizeAccountNumber(accountNumber);
        return txExecutor.executeReadOnly(uow -> {
            BankAccount account = uow.accounts().findByAccountNumber(cleanAccountNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + cleanAccountNumber));
            User owner = uow.users().findById(account.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found."));
            return new AccountSummary(
                    account.id(),
                    account.accountNumber(),
                    owner.id(),
                    owner.fullName(),
                    owner.email(),
                    account.balancePaise(),
                    Money.formatInr(account.balancePaise()),
                    account.status(),
                    account.createdAt());
        });
    }

    public List<TransactionView> getCustomerTransactionHistory(String accountNumber, int limit) {
        sessionManager.requireRole(Role.STAFF, Role.ADMIN);
        String cleanAccountNumber = InputValidator.normalizeAccountNumber(accountNumber);
        int validLimit = InputValidator.requireValidLimit(limit);
        return txExecutor.executeReadOnly(uow -> {
            BankAccount account = uow.accounts().findByAccountNumber(cleanAccountNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + cleanAccountNumber));
            return uow.transactions().findViewsByAccountId(account.id(), validLimit);
        });
    }
}
