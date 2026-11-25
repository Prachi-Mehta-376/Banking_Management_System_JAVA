package com.tiet.banking.service;

import com.tiet.banking.db.TransactionExecutor;
import com.tiet.banking.dto.AccountSummary;
import com.tiet.banking.dto.TransactionReceipt;
import com.tiet.banking.dto.TransactionView;
import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.AccountFrozenException;
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
import com.tiet.banking.util.CsvStatementExporter;
import com.tiet.banking.util.InputValidator;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CustomerBankingService {
    private final TransactionExecutor txExecutor;
    private final SessionManager sessionManager;
    private final Runnable postDebitTestHook;

    public CustomerBankingService(TransactionExecutor txExecutor, SessionManager sessionManager) {
        this(txExecutor, sessionManager, null);
    }

    // Package-private test constructor to inject deterministic failures between debit and commit.
    CustomerBankingService(TransactionExecutor txExecutor, SessionManager sessionManager, Runnable postDebitTestHook) {
        this.txExecutor = txExecutor;
        this.sessionManager = sessionManager;
        this.postDebitTestHook = postDebitTestHook;
    }

    public AccountSummary getOwnAccountSummary() {
        UserSession session = sessionManager.requireRole(Role.CUSTOMER);
        return txExecutor.executeReadOnly(uow -> {
            User owner = uow.users().findById(session.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found."));
            BankAccount account = uow.accounts().findByUserId(session.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer account not found."));
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

    public List<TransactionView> getOwnTransactionHistory(int limit) {
        UserSession session = sessionManager.requireRole(Role.CUSTOMER);
        int validLimit = InputValidator.requireValidLimit(limit);
        return txExecutor.executeReadOnly(uow -> {
            BankAccount account = uow.accounts().findByUserId(session.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer account not found."));
            return uow.transactions().findViewsByAccountId(account.id(), validLimit);
        });
    }

    public TransactionReceipt transferFunds(String destinationAccountNumber, String amountInput) {
        return transferFunds(destinationAccountNumber, amountInput, null);
    }

    public TransactionReceipt transferFunds(String destinationAccountNumber, String amountInput, String idempotencyKey) {
        UserSession session = sessionManager.requireRole(Role.CUSTOMER);
        String destNumber = InputValidator.normalizeAccountNumber(destinationAccountNumber);
        Money amount = Money.parsePositive(amountInput);
        String cleanKey = normalizeOptionalKey(idempotencyKey);
        String requestSignature = OperationType.TRANSFER.name() + "|" + destNumber + "|" + amount.paise();

        return txExecutor.execute(uow -> {
            if (cleanKey != null) {
                Optional<IdempotencyRecord> existing = uow.idempotencyRecords().findByUserAndKey(session.userId(), cleanKey);
                if (existing.isPresent()) {
                    return handleExistingTransferKey(existing.get(), requestSignature, uow);
                }
            }

            BankAccount source = uow.accounts().findByUserId(session.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Source customer account not found."));
            BankAccount destination = uow.accounts().findByAccountNumber(destNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Destination account not found: " + destNumber));

            if (source.id() == destination.id()) {
                throw new ValidationException("Cannot transfer funds to your own account.");
            }
            if (source.status() != AccountStatus.ACTIVE) {
                throw new AccountFrozenException("Your account is frozen and cannot initiate transfers.");
            }
            if (destination.status() != AccountStatus.ACTIVE) {
                throw new AccountFrozenException("Destination account is frozen and cannot receive transfers.");
            }
            if (source.balancePaise() < amount.paise()) {
                throw new InsufficientFundsException("Insufficient balance for transfer.");
            }

            long sourceAfter = safeSubtract(source.balancePaise(), amount.paise());
            long destAfter = safeAdd(destination.balancePaise(), amount.paise());
            String referenceId = generateReference("TRF");
            Instant now = Instant.now();
            uow.setActiveOperationReference(referenceId);

            if (!uow.accounts().debitIfActiveAndSufficient(source.id(), amount.paise())) {
                throw new InsufficientFundsException("Transfer failed due to insufficient balance or frozen status.");
            }
            if (postDebitTestHook != null) {
                postDebitTestHook.run();
            }
            if (!uow.accounts().creditIfActive(destination.id(), amount.paise())) {
                throw new AccountFrozenException("Destination account is no longer active.");
            }

            BankOperation operation = uow.operations().insert(
                    new BankOperation(0, referenceId, OperationType.TRANSFER, session.userId(), now));
            uow.transactions().insert(
                    new BankTransaction(0, operation.id(), source.id(), -amount.paise(), sourceAfter, now));
            uow.transactions().insert(
                    new BankTransaction(0, operation.id(), destination.id(), amount.paise(), destAfter, now));

            if (cleanKey != null) {
                uow.idempotencyRecords().insert(new IdempotencyRecord(
                        0, session.userId(), cleanKey, OperationType.TRANSFER, requestSignature,
                        referenceId, source.id(), destination.accountNumber(), amount.paise(), sourceAfter, now));
            }

            return new TransactionReceipt(
                    referenceId,
                    OperationType.TRANSFER,
                    source.accountNumber(),
                    destination.accountNumber(),
                    amount.paise(),
                    Money.formatInr(amount.paise()),
                    sourceAfter,
                    Money.formatInr(sourceAfter),
                    now);
        });
    }

    public Path exportOwnStatementCsv(Path targetFile) {
        UserSession session = sessionManager.requireRole(Role.CUSTOMER);
        AccountSummary summary = getOwnAccountSummary();
        List<TransactionView> transactions = getOwnTransactionHistory(500);
        if (summary.ownerUserId() != session.userId()) {
            throw new PersistenceException("Ownership verification failed during statement export.");
        }
        return CsvStatementExporter.export(targetFile, summary, transactions);
    }

    private TransactionReceipt handleExistingTransferKey(
            IdempotencyRecord existing, String requestSignature, com.tiet.banking.db.BankingUnitOfWork uow) {
        if (!existing.requestSignature().equals(requestSignature)) {
            throw new ConflictException("Idempotency key already used for a different request.");
        }
        BankAccount primary = uow.accounts().findById(existing.primaryAccountId())
                .orElseThrow(() -> new PersistenceException("Account missing for idempotency record."));
        return new TransactionReceipt(
                existing.referenceId(),
                existing.operationType(),
                primary.accountNumber(),
                existing.counterpartyAccountNumber(),
                existing.amountPaise(),
                Money.formatInr(existing.amountPaise()),
                existing.balanceAfterPaise(),
                Money.formatInr(existing.balanceAfterPaise()),
                existing.createdAt());
    }

    private String normalizeOptionalKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.trim();
    }

    private long safeAdd(long a, long b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException ex) {
            throw new ValidationException("Transfer exceeds recipient maximum supported balance.");
        }
    }

    private long safeSubtract(long a, long b) {
        try {
            return Math.subtractExact(a, b);
        } catch (ArithmeticException ex) {
            throw new ValidationException("Balance underflow detected.");
        }
    }

    private String generateReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
