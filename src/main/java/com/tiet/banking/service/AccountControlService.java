package com.tiet.banking.service;

import com.tiet.banking.db.TransactionExecutor;
import com.tiet.banking.dto.AccountActionRequestView;
import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.AuthorizationException;
import com.tiet.banking.exception.ConflictException;
import com.tiet.banking.exception.ResourceNotFoundException;
import com.tiet.banking.exception.ValidationException;
import com.tiet.banking.model.AccountActionRequest;
import com.tiet.banking.model.AccountActionType;
import com.tiet.banking.model.AccountStatus;
import com.tiet.banking.model.AuditEvent;
import com.tiet.banking.model.BankAccount;
import com.tiet.banking.model.RequestStatus;
import com.tiet.banking.model.Role;
import com.tiet.banking.security.SessionManager;
import com.tiet.banking.util.InputValidator;
import java.time.Instant;
import java.util.List;

public class AccountControlService {
    private final TransactionExecutor txExecutor;
    private final SessionManager sessionManager;

    public AccountControlService(TransactionExecutor txExecutor, SessionManager sessionManager) {
        this.txExecutor = txExecutor;
        this.sessionManager = sessionManager;
    }

    public AccountActionRequestView requestAccountFreezeChange(
            String accountNumber, AccountActionType actionType, String reason) {
        UserSession session = sessionManager.requireRole(Role.STAFF);
        String cleanAccountNumber = InputValidator.normalizeAccountNumber(accountNumber);
        if (actionType == null) {
            throw new ValidationException("Action type is required.");
        }
        String cleanReason = InputValidator.requireNonBlank(reason, "Reason");
        Instant now = Instant.now();

        return txExecutor.execute(uow -> {
            BankAccount account = uow.accounts().findByAccountNumber(cleanAccountNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + cleanAccountNumber));
            AccountStatus targetStatus = actionType == AccountActionType.FREEZE ? AccountStatus.FROZEN : AccountStatus.ACTIVE;
            if (account.status() == targetStatus) {
                throw new ValidationException("Account is already " + targetStatus + ".");
            }
            if (uow.accountActionRequests().hasPendingRequestForAccount(account.id())) {
                throw new ConflictException("A pending request already exists for account " + cleanAccountNumber + ".");
            }

            AccountActionRequest created = uow.accountActionRequests().insert(new AccountActionRequest(
                    0, account.id(), actionType, cleanReason, session.userId(),
                    RequestStatus.PENDING, null, now, null));
            String auditAction = actionType == AccountActionType.FREEZE ? "FREEZE_REQUESTED" : "UNFREEZE_REQUESTED";
            uow.auditEvents().insert(new AuditEvent(
                    0, session.userId(), auditAction, "ACCOUNT", cleanAccountNumber, now));

            return uow.accountActionRequests().findAllViews(200).stream()
                    .filter(v -> v.requestId() == created.id())
                    .findFirst()
                    .orElseThrow();
        });
    }

    public List<AccountActionRequestView> listPendingRequests() {
        sessionManager.requireRole(Role.ADMIN, Role.STAFF);
        return txExecutor.executeReadOnly(uow -> uow.accountActionRequests().findPendingViews());
    }

    public List<AccountActionRequestView> listAllRequests() {
        sessionManager.requireRole(Role.ADMIN, Role.STAFF);
        return txExecutor.executeReadOnly(uow -> uow.accountActionRequests().findAllViews(200));
    }

    public AccountActionRequestView reviewAccountFreezeChange(long requestId, boolean approve) {
        UserSession session = sessionManager.requireRole(Role.ADMIN);
        if (requestId <= 0) {
            throw new ValidationException("Invalid request ID.");
        }
        Instant now = Instant.now();

        return txExecutor.execute(uow -> {
            AccountActionRequest request = uow.accountActionRequests().findById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account action request not found."));
            if (request.status() != RequestStatus.PENDING) {
                throw new ConflictException("This request has already been reviewed.");
            }
            if (request.requestedByUserId() == session.userId()) {
                throw new AuthorizationException("Requester cannot approve or reject their own request.");
            }

            RequestStatus decision = approve ? RequestStatus.APPROVED : RequestStatus.REJECTED;
            if (!uow.accountActionRequests().updateDecisionIfPending(requestId, decision, session.userId(), now)) {
                throw new ConflictException("Request could not be updated because it is no longer pending.");
            }

            BankAccount account = uow.accounts().findById(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target account not found."));
            if (approve) {
                AccountStatus newStatus = request.actionType() == AccountActionType.FREEZE
                        ? AccountStatus.FROZEN : AccountStatus.ACTIVE;
                uow.accounts().updateStatus(account.id(), newStatus);
            }

            String auditAction = approve
                    ? (request.actionType() == AccountActionType.FREEZE ? "ACCOUNT_FREEZE_APPROVED" : "ACCOUNT_UNFREEZE_APPROVED")
                    : "ACCOUNT_ACTION_REJECTED";
            uow.auditEvents().insert(new AuditEvent(
                    0, session.userId(), auditAction, "ACCOUNT", account.accountNumber(), now));

            return uow.accountActionRequests().findAllViews(200).stream()
                    .filter(v -> v.requestId() == requestId)
                    .findFirst()
                    .orElseThrow();
        });
    }
}
