package com.tiet.banking.service;

import com.tiet.banking.db.TransactionExecutor;
import com.tiet.banking.dto.AdministrativeSummary;
import com.tiet.banking.dto.AuditEventView;
import com.tiet.banking.dto.StaffUserSummary;
import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.AuthorizationException;
import com.tiet.banking.exception.ResourceNotFoundException;
import com.tiet.banking.exception.ValidationException;
import com.tiet.banking.model.AuditEvent;
import com.tiet.banking.model.Role;
import com.tiet.banking.model.User;
import com.tiet.banking.security.PasswordHasher;
import com.tiet.banking.security.SessionManager;
import com.tiet.banking.util.InputValidator;
import java.time.Instant;
import java.util.List;

public class AdminService {
    private final TransactionExecutor txExecutor;
    private final PasswordHasher passwordHasher;
    private final SessionManager sessionManager;

    public AdminService(TransactionExecutor txExecutor, PasswordHasher passwordHasher, SessionManager sessionManager) {
        this.txExecutor = txExecutor;
        this.passwordHasher = passwordHasher;
        this.sessionManager = sessionManager;
    }

    public StaffUserSummary createStaffUser(String fullName, String email, char[] password) {
        UserSession session = sessionManager.requireRole(Role.ADMIN);
        String cleanName = InputValidator.requireNonBlank(fullName, "Full name");
        String cleanEmail = InputValidator.normalizeEmail(email);
        InputValidator.requireValidPassword(password);
        PasswordHasher.HashedPassword hashed = passwordHasher.hash(password);
        Instant now = Instant.now();

        return txExecutor.execute(uow -> {
            User actor = uow.users().findById(session.userId())
                    .orElseThrow(() -> new AuthorizationException("Administrator record not found."));
            if (!actor.active() || actor.role() != Role.ADMIN) {
                throw new AuthorizationException("Active administrator authorization required.");
            }
            User staff = uow.users().insert(new User(
                    0, cleanName, cleanEmail, hashed.hashBase64(), hashed.saltBase64(),
                    hashed.algoMetadata(), Role.STAFF, true, now));
            uow.auditEvents().insert(new AuditEvent(
                    0, session.userId(), "STAFF_CREATED", "USER", String.valueOf(staff.id()), now));
            return new StaffUserSummary(staff.id(), staff.fullName(), staff.email(), staff.active(), staff.createdAt());
        });
    }

    public void setStaffActiveStatus(long staffUserId, boolean active) {
        UserSession session = sessionManager.requireRole(Role.ADMIN);
        if (staffUserId <= 0) {
            throw new ValidationException("Invalid staff user ID.");
        }
        Instant now = Instant.now();
        txExecutor.executeVoid(uow -> {
            User target = uow.users().findById(staffUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff user not found."));
            if (target.role() != Role.STAFF) {
                throw new ValidationException("Only STAFF user accounts can be activated or deactivated here.");
            }
            if (!uow.users().updateActiveStatus(staffUserId, active)) {
                throw new ResourceNotFoundException("Staff user not found.");
            }
            String action = active ? "STAFF_ACTIVATED" : "STAFF_DEACTIVATED";
            uow.auditEvents().insert(new AuditEvent(
                    0, session.userId(), action, "USER", String.valueOf(staffUserId), now));
        });
    }

    public List<StaffUserSummary> listStaffUsers() {
        sessionManager.requireRole(Role.ADMIN);
        return txExecutor.executeReadOnly(uow ->
                uow.users().findByRole(Role.STAFF).stream()
                        .map(u -> new StaffUserSummary(u.id(), u.fullName(), u.email(), u.active(), u.createdAt()))
                        .toList());
    }

    public AdministrativeSummary getAdministrativeSummary() {
        sessionManager.requireRole(Role.ADMIN);
        return txExecutor.executeReadOnly(uow -> uow.accounts().getAdministrativeSummary());
    }

    public List<AuditEventView> getAuditEvents(int limit, String actionFilter) {
        sessionManager.requireRole(Role.ADMIN);
        int validLimit = InputValidator.requireValidLimit(limit);
        return txExecutor.executeReadOnly(uow -> uow.auditEvents().findRecent(validLimit, actionFilter));
    }
}
