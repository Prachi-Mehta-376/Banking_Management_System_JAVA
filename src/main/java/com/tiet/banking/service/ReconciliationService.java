package com.tiet.banking.service;

import com.tiet.banking.db.BankingUnitOfWork;
import com.tiet.banking.db.TransactionExecutor;
import com.tiet.banking.dto.ReconciliationReport;
import com.tiet.banking.dto.ReconciliationResult;
import com.tiet.banking.exception.ResourceNotFoundException;
import com.tiet.banking.model.BankAccount;
import com.tiet.banking.model.BankOperation;
import com.tiet.banking.model.BankTransaction;
import com.tiet.banking.model.OperationType;
import com.tiet.banking.model.Role;
import com.tiet.banking.security.SessionManager;
import com.tiet.banking.util.InputValidator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationService {
    private final TransactionExecutor txExecutor;
    private final SessionManager sessionManager;

    public ReconciliationService(TransactionExecutor txExecutor, SessionManager sessionManager) {
        this.txExecutor = txExecutor;
        this.sessionManager = sessionManager;
    }

    public ReconciliationResult reconcileAccount(String accountNumber) {
        sessionManager.requireRole(Role.ADMIN);
        String cleanAccountNumber = InputValidator.normalizeAccountNumber(accountNumber);
        return txExecutor.executeReadOnly(uow -> {
            BankAccount account = uow.accounts().findByAccountNumber(cleanAccountNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + cleanAccountNumber));
            return checkSingleAccount(account, uow);
        });
    }

    public ReconciliationReport reconcileAllAccounts() {
        sessionManager.requireRole(Role.ADMIN);
        return txExecutor.executeReadOnly(uow -> {
            List<BankAccount> accounts = uow.accounts().findAllOrderById();
            List<ReconciliationResult> results = new ArrayList<>();
            int consistent = 0;
            int mismatched = 0;

            for (BankAccount account : accounts) {
                ReconciliationResult result = checkSingleAccount(account, uow);
                results.add(result);
                if (result.matched()) {
                    consistent++;
                } else {
                    mismatched++;
                }
            }

            List<String> journalFindings = verifyOperations(uow);
            return new ReconciliationReport(
                    accounts.size(),
                    consistent,
                    mismatched,
                    journalFindings.size(),
                    results,
                    journalFindings,
                    Instant.now());
        });
    }

    private ReconciliationResult checkSingleAccount(BankAccount account, BankingUnitOfWork uow) {
        List<BankTransaction> entries = uow.transactions().findByAccountIdAsc(account.id());
        long expectedBalance = 0L;
        boolean sequenceValid = true;
        StringBuilder details = new StringBuilder();

        for (BankTransaction entry : entries) {
            try {
                expectedBalance = Math.addExact(expectedBalance, entry.deltaPaise());
            } catch (ArithmeticException ex) {
                sequenceValid = false;
                details.append("Arithmetic overflow in journal deltas; ");
                break;
            }
            if (expectedBalance != entry.balanceAfterPaise()) {
                sequenceValid = false;
                details.append("Balance-after mismatch at tx #").append(entry.id()).append("; ");
            }
        }

        long discrepancy = account.balancePaise() - expectedBalance;
        boolean balanceMatched = discrepancy == 0 && sequenceValid;
        if (discrepancy != 0) {
            details.append("Stored balance differs from committed journal sum by ").append(discrepancy).append(" paise.");
        } else if (sequenceValid) {
            details.append("Balanced (" + entries.size() + " journal entries verified)");
        }
        return new ReconciliationResult(
                account.id(),
                account.accountNumber(),
                account.balancePaise(),
                expectedBalance,
                discrepancy,
                balanceMatched,
                details.toString().trim());
    }

    private List<String> verifyOperations(BankingUnitOfWork uow) {
        List<String> findings = new ArrayList<>();
        List<BankOperation> operations = uow.operations().findAllOrderById();
        for (BankOperation op : operations) {
            List<BankTransaction> entries = uow.transactions().findByOperationId(op.id());
            if (op.operationType() == OperationType.TRANSFER) {
                if (entries.size() != 2) {
                    findings.add("Transfer " + op.referenceId() + " has " + entries.size() + " entries (expected 2).");
                } else {
                    long sum = Math.addExact(entries.get(0).deltaPaise(), entries.get(1).deltaPaise());
                    if (sum != 0) {
                        findings.add("Transfer " + op.referenceId() + " deltas sum to " + sum + " (expected 0).");
                    }
                }
            } else if (op.operationType() == OperationType.DEPOSIT) {
                if (entries.size() != 1 || entries.get(0).deltaPaise() <= 0) {
                    findings.add("Deposit " + op.referenceId() + " has invalid entry count or non-positive delta.");
                }
            } else if (op.operationType() == OperationType.WITHDRAWAL) {
                if (entries.size() != 1 || entries.get(0).deltaPaise() >= 0) {
                    findings.add("Withdrawal " + op.referenceId() + " has invalid entry count or non-negative delta.");
                }
            }
        }
        return findings;
    }
}
