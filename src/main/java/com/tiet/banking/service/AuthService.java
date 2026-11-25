package com.tiet.banking.service;

import com.tiet.banking.db.TransactionExecutor;
import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.AuthenticationException;
import com.tiet.banking.exception.ConflictException;
import com.tiet.banking.model.AuditEvent;
import com.tiet.banking.model.BankAccount;
import com.tiet.banking.model.Role;
import com.tiet.banking.model.User;
import com.tiet.banking.security.PasswordHasher;
import com.tiet.banking.security.SessionManager;
import com.tiet.banking.util.AppLogger;
import com.tiet.banking.util.InputValidator;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

public class AuthService {
    private static final Logger LOG = AppLogger.getLogger(AuthService.class);

    private final TransactionExecutor txExecutor;
    private final PasswordHasher passwordHasher;
    private final SessionManager sessionManager;

    public AuthService(TransactionExecutor txExecutor, PasswordHasher passwordHasher, SessionManager sessionManager) {
        this.txExecutor = txExecutor;
        this.passwordHasher = passwordHasher;
        this.sessionManager = sessionManager;
    }

    public boolean isInitialAdminSetupRequired() {
        return txExecutor.executeReadOnly(uow -> uow.users().countByRole(Role.ADMIN) == 0);
    }

    public UserSession setupInitialAdmin(String fullName, String email, char[] password) {
        String cleanName = InputValidator.requireNonBlank(fullName, "Full name");
        String cleanEmail = InputValidator.normalizeEmail(email);
        InputValidator.requireValidPassword(password);
        PasswordHasher.HashedPassword hashed = passwordHasher.hash(password);
        Instant now = Instant.now();

        UserSession session = txExecutor.execute(uow -> {
            // Re-verify under SQLite write lock so concurrent setup calls cannot create two bootstrap admins.
            if (uow.users().countByRole(Role.ADMIN) > 0) {
                throw new ConflictException("Initial administrator setup has already been completed.");
            }
            User admin = uow.users().insert(new User(
                    0, cleanName, cleanEmail, hashed.hashBase64(), hashed.saltBase64(),
                    hashed.algoMetadata(), Role.ADMIN, true, now));
            uow.auditEvents().insert(new AuditEvent(
                    0, admin.id(), "INITIAL_ADMIN_CREATED", "USER", String.valueOf(admin.id()), now));
            return new UserSession(admin.id(), admin.fullName(), admin.email(), Role.ADMIN, null, null);
        });
        sessionManager.setSession(session);
        LOG.info("Initial administrator bootstrap completed for userId=" + session.userId());
        return session;
    }

    public UserSession registerCustomer(String fullName, String email, char[] password) {
        String cleanName = InputValidator.requireNonBlank(fullName, "Full name");
        String cleanEmail = InputValidator.normalizeEmail(email);
        InputValidator.requireValidPassword(password);
        PasswordHasher.HashedPassword hashed = passwordHasher.hash(password);
        Instant now = Instant.now();

        UserSession session = txExecutor.execute(uow -> {
            User customer = uow.users().insert(new User(
                    0, cleanName, cleanEmail, hashed.hashBase64(), hashed.saltBase64(),
                    hashed.algoMetadata(), Role.CUSTOMER, true, now));
            BankAccount account = uow.accounts().createAccount(customer.id(), now);
            return new UserSession(customer.id(), customer.fullName(), customer.email(),
                    Role.CUSTOMER, account.id(), account.accountNumber());
        });
        sessionManager.setSession(session);
        LOG.info("Registered new customer userId=" + session.userId() + " account=" + session.accountNumber());
        return session;
    }

    public UserSession login(String email, char[] password) {
        String cleanEmail = InputValidator.normalizeEmail(email);
        if (password == null || password.length == 0) {
            throw new AuthenticationException("Invalid email or password.");
        }

        UserSession session = txExecutor.executeReadOnly(uow -> {
            User user = uow.users().findByEmail(cleanEmail)
                    .orElseThrow(() -> new AuthenticationException("Invalid email or password."));
            if (!user.active()) {
                throw new AuthenticationException("This user account has been deactivated.");
            }
            if (!passwordHasher.verify(password, user.passwordHash(), user.passwordSalt(), user.passwordAlgo())) {
                throw new AuthenticationException("Invalid email or password.");
            }
            if (user.role() == Role.CUSTOMER) {
                BankAccount account = uow.accounts().findByUserId(user.id())
                        .orElseThrow(() -> new AuthenticationException("Customer bank account is missing."));
                return new UserSession(user.id(), user.fullName(), user.email(),
                        Role.CUSTOMER, account.id(), account.accountNumber());
            }
            return new UserSession(user.id(), user.fullName(), user.email(), user.role(), null, null);
        });

        sessionManager.setSession(session);
        LOG.info("User logged in: userId=" + session.userId() + " role=" + session.role());
        return session;
    }

    public void logout() {
        sessionManager.getCurrentSession().ifPresent(s ->
                LOG.info("User logged out: userId=" + s.userId() + " role=" + s.role()));
        sessionManager.clear();
    }

    public Optional<UserSession> getCurrentSession() {
        return sessionManager.getCurrentSession();
    }
}
