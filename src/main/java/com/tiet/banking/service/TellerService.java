package com.tiet.banking.service;

import com.tiet.banking.db.BankingUnitOfWork;
import com.tiet.banking.db.TransactionExecutor;
import com.tiet.banking.dto.TransactionReceipt;
import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.AccountFrozenException;
import com.tiet.banking.exception.AuthorizationException;
import com.tiet.banking.exception.ConflictException;
import com.tiet.banking.exception.InsufficientFundsException;
import com.tiet.banking.exception.PersistenceException;
import com.tiet.banking.exception.ResourceNotFoundException;
import com.tiet.banking.exception.ValidationException;
import com.tiet.banking.model.AccountStatus;
import com.tiet.banking.model.BankAccount;
import com.tiet.banking.model.BankOperation;
import com.tiet.banking.model.BankTransaction;
import com.tiet.banking.model.IdempotencyRecord;
import com.tiet.banking.model.Money;
import com.tiet.banking.model.OperationType;
import com.tiet.banking.model.Role;
import com.tiet.banking.model.User;
import com.tiet.banking.security.SessionManager;
import com.tiet.banking.util.InputValidator;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class TellerService {
    private final TransactionExecutor txExecutor;
    private final SessionManager sessionManager;

    public TellerService(TransactionExecutor txExecutor, SessionManager sessionManager) {
        this.txExecutor = txExecutor;
        this.sessionManager = sessionManager;
    }

    public TransactionReceipt depositCash(String accountNumber, String amountInput) {
        return depositCash(accountNumber, amountInput, null);
    }

    public TransactionReceipt depositCash(String accountNumber, String amountInput, String idempotencyKey) {
        UserSession session = sessionManager.requireRole(Role.STAFF);
        String cleanAccountNumber = InputValidator.normalizeAccountNumber(accountNumber);
        Money amount = Money.parsePositive(amountInput);
        String cleanKey = normalizeOptionalKey(idempotencyKey);
        String signature = OperationType.DEPOSIT.name() + "|" + cleanAccountNumber + "|" + amount.paise();

        return txExecutor.execute(uow -> {
            requireActiveStaff(uow, session.userId());
            if (cleanKey != null) {
                Optional<IdempotencyRecord> existing = uow.idempotencyRecords().findByUserAndKey(session.userId(), cleanKey);
                if (existing.isPresent()) {
                    return receiptFromIdempotency(existing.get(), signature, uow);
                }
            }

            BankAccount account = uow.accounts().findByAccountNumber(cleanAccountNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + cleanAccountNumber));
            if (account.status() != AccountStatus.ACTIVE) {
                throw new AccountFrozenException("Account is frozen and cannot accept deposits.");
            }

            long balanceAfter;
            try {
                balanceAfter = Math.addExact(account.balancePaise(), amount.paise());
            } catch (ArithmeticException ex) {
                throw new ValidationException("Deposit exceeds maximum supported account balance.");
            }

            String referenceId = "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Instant now = Instant.now();
            uow.setActiveOperationReference(referenceId);

            if (!uow.accounts().creditIfActive(account.id(), amount.paise())) {
                throw new AccountFrozenException("Account is no longer active for deposit.");
            }

            BankOperation op = uow.operations().insert(
                    new BankOperation(0, referenceId, OperationType.DEPOSIT, session.userId(), now));
            uow.transactions().insert(
                    new BankTransaction(0, op.id(), account.id(), amount.paise(), balanceAfter, now));

            if (cleanKey != null) {
                uow.idempotencyRecords().insert(new IdempotencyRecord(
                        0, session.userId(), cleanKey, OperationType.DEPOSIT, signature,
                        referenceId, account.id(), null, amount.paise(), balanceAfter, now));
            }

            return new TransactionReceipt(
                    referenceId,
                    OperationType.DEPOSIT,
                    account.accountNumber(),
                    null,
                    amount.paise(),
                    Money.formatInr(amount.paise()),
                    balanceAfter,
                    Money.formatInr(balanceAfter),
                    now);
        });
    }

    public TransactionReceipt withdrawCash(String accountNumber, String amountInput) {
        return withdrawCash(accountNumber, amountInput, null);
    }

    public TransactionReceipt withdrawCash(String accountNumber, String amountInput, String idempotencyKey) {
        UserSession session = sessionManager.requireRole(Role.STAFF);
        String cleanAccountNumber = InputValidator.normalizeAccountNumber(accountNumber);
        Money amount = Money.parsePositive(amountInput);
        String cleanKey = normalizeOptionalKey(idempotencyKey);
        String signature = OperationType.WITHDRAWAL.name() + "|" + cleanAccountNumber + "|" + amount.paise();

        return txExecutor.execute(uow -> {
            requireActiveStaff(uow, session.userId());
            if (cleanKey != null) {
                Optional<IdempotencyRecord> existing = uow.idempotencyRecords().findByUserAndKey(session.userId(), cleanKey);
                if (existing.isPresent()) {
                    return receiptFromIdempotency(existing.get(), signature, uow);
                }
            }

            BankAccount account = uow.accounts().findByAccountNumber(cleanAccountNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + cleanAccountNumber));
            if (account.status() != AccountStatus.ACTIVE) {
                throw new AccountFrozenException("Account is frozen and cannot process withdrawals.");
            }
            if (account.balancePaise() < amount.paise()) {
                throw new InsufficientFundsException("Insufficient account balance for withdrawal.");
            }

            long balanceAfter;
            try {
                balanceAfter = Math.subtractExact(account.balancePaise(), amount.paise());
            } catch (ArithmeticException ex) {
                throw new ValidationException("Withdrawal causes balance underflow.");
            }

            String referenceId = "WDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Instant now = Instant.now();
            uow.setActiveOperationReference(referenceId);

            if (!uow.accounts().debitIfActiveAndSufficient(account.id(), amount.paise())) {
                throw new InsufficientFundsException("Withdrawal rejected due to insufficient balance or frozen status.");
            }

            BankOperation op = uow.operations().insert(
                    new BankOperation(0, referenceId, OperationType.WITHDRAWAL, session.userId(), now));
            uow.transactions().insert(
                    new BankTransaction(0, op.id(), account.id(), -amount.paise(), balanceAfter, now));

            if (cleanKey != null) {
                uow.idempotencyRecords().insert(new IdempotencyRecord(
                        0, session.userId(), cleanKey, OperationType.WITHDRAWAL, signature,
                        referenceId, account.id(), null, amount.paise(), balanceAfter, now));
            }

            return new TransactionReceipt(
                    referenceId,
                    OperationType.WITHDRAWAL,
                    account.accountNumber(),
                    null,
                    amount.paise(),
                    Money.formatInr(amount.paise()),
                    balanceAfter,
                    Money.formatInr(balanceAfter),
                    now);
        });
    }

    private void requireActiveStaff(BankingUnitOfWork uow, long userId) {
        User staff = uow.users().findById(userId)
                .orElseThrow(() -> new AuthorizationException("Staff user record not found."));
        if (!staff.active() || staff.role() != Role.STAFF) {
            throw new AuthorizationException("Active staff authorization required.");
        }
    }

    private TransactionReceipt receiptFromIdempotency(
            IdempotencyRecord existing, String requestSignature, BankingUnitOfWork uow) {
        if (!existing.requestSignature().equals(requestSignature)) {
            throw new ConflictException("Idempotency key already used for a different request.");
        }
        BankAccount account = uow.accounts().findById(existing.primaryAccountId())
                .orElseThrow(() -> new PersistenceException("Account missing for idempotency record."));
        return new TransactionReceipt(
                existing.referenceId(),
                existing.operationType(),
                account.accountNumber(),
                existing.counterpartyAccountNumber(),
                existing.amountPaise(),
                Money.formatInr(existing.amountPaise()),
                existing.balanceAfterPaise(),
                Money.formatInr(existing.balanceAfterPaise()),
                existing.createdAt());
    }

    private String normalizeOptionalKey(String key) {
        return (key == null || key.isBlank()) ? null : key.trim();
    }
}
