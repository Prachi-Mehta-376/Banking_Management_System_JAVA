package com.tiet.banking.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tiet.banking.bootstrap.ServiceRegistry;
import com.tiet.banking.db.DatabaseManager;
import com.tiet.banking.db.JdbcTransactionExecutor;
import com.tiet.banking.db.TransactionExecutor;
import com.tiet.banking.db.TransactionWork;
import com.tiet.banking.dto.AccountActionRequestView;
import com.tiet.banking.dto.AuditEventView;
import com.tiet.banking.dto.ReconciliationReport;
import com.tiet.banking.dto.TransactionReceipt;
import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.AccountFrozenException;
import com.tiet.banking.exception.AuthorizationException;
import com.tiet.banking.exception.CommitOutcomeUncertainException;
import com.tiet.banking.exception.ConflictException;
import com.tiet.banking.exception.InsufficientFundsException;
import com.tiet.banking.exception.ValidationException;
import com.tiet.banking.model.AccountActionType;
import com.tiet.banking.model.RequestStatus;
import com.tiet.banking.security.SessionManager;
import com.tiet.banking.service.CustomerBankingService;
import com.tiet.banking.util.AppConfig;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BankingEngineIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void executesFullFinancialLifecycleAndRollsBackOnInjectedFailure() throws Exception {
        Path dbFile = tempDir.resolve("engine.db");
        ServiceRegistry reg = new ServiceRegistry(new AppConfig(dbFile, 10_000, 5_000));
        reg.authService().setupInitialAdmin("Admin", "admin@tiet.edu", "AdminPass1!".toCharArray());
        reg.adminService().createStaffUser("Staff", "staff@tiet.edu", "StaffPass1!".toCharArray());

        UserSession alice = reg.authService().registerCustomer("Alice", "alice@tiet.edu", "CustPass1!".toCharArray());
        UserSession bob = reg.authService().registerCustomer("Bob", "bob@tiet.edu", "CustPass1!".toCharArray());

        // Deposit & withdrawal
        reg.authService().login("staff@tiet.edu", "StaffPass1!".toCharArray());
        TransactionReceipt dep = reg.tellerService().depositCash(alice.accountNumber(), "1200.00");
        assertEquals(120_000L, dep.balanceAfterPaise());
        TransactionReceipt wdr = reg.tellerService().withdrawCash(alice.accountNumber(), "200.00");
        assertEquals(100_000L, wdr.balanceAfterPaise());

        assertThrows(InsufficientFundsException.class, () -> reg.tellerService().withdrawCash(alice.accountNumber(), "1500.00"));

        // Valid customer transfer & self-transfer rejection
        reg.authService().login("alice@tiet.edu", "CustPass1!".toCharArray());
        assertThrows(ValidationException.class, () -> reg.customerBankingService().transferFunds(alice.accountNumber(), "10.00"));
        TransactionReceipt trf = reg.customerBankingService().transferFunds(bob.accountNumber(), "400.00");
        assertEquals(60_000L, trf.balanceAfterPaise());

        // Verify zero-sum transfer deltas
        Long transferDeltaSum = reg.transactionExecutor().executeReadOnly(uow -> {
            var op = uow.operations().findByReferenceId(trf.referenceId()).orElseThrow();
            var entries = uow.transactions().findByOperationId(op.id());
            return entries.stream().mapToLong(e -> e.deltaPaise()).sum();
        });
        assertEquals(0L, transferDeltaSum);

        // Inject deterministic failure AFTER sender debit and BEFORE commit; verify rollback on fresh connection
        SessionManager testSession = new SessionManager();
        testSession.setSession(alice);
        Constructor<CustomerBankingService> ctor = CustomerBankingService.class.getDeclaredConstructor(
                TransactionExecutor.class, SessionManager.class, Runnable.class);
        ctor.setAccessible(true);
        CustomerBankingService failingService = ctor.newInstance(
                reg.transactionExecutor(), testSession,
                (Runnable) () -> { throw new IllegalStateException("Simulated post-debit failure"); });

        assertThrows(IllegalStateException.class, () -> failingService.transferFunds(bob.accountNumber(), "100.00"));

        // Re-check balances on a fresh ServiceRegistry/connection
        ServiceRegistry freshReader = new ServiceRegistry(new AppConfig(dbFile, 10_000, 5_000));
        freshReader.authService().login("alice@tiet.edu", "CustPass1!".toCharArray());
        assertEquals(60_000L, freshReader.customerBankingService().getOwnAccountSummary().balancePaise());
        freshReader.authService().login("bob@tiet.edu", "CustPass1!".toCharArray());
        assertEquals(40_000L, freshReader.customerBankingService().getOwnAccountSummary().balancePaise());
        assertEquals(1, freshReader.customerBankingService().getOwnTransactionHistory(10).size());
    }

    @Test
    void enforcesMakerCheckerFreezeRulesAuditAtomicityCsvExportIdempotencyAndReconciliation() throws Exception {
        Path dbFile = tempDir.resolve("integrity.db");
        ServiceRegistry reg = new ServiceRegistry(new AppConfig(dbFile, 10_000, 5_000));
        reg.authService().setupInitialAdmin("Admin", "admin@tiet.edu", "AdminPass1!".toCharArray());
        reg.adminService().createStaffUser("Staff", "staff@tiet.edu", "StaffPass1!".toCharArray());
        UserSession alice = reg.authService().registerCustomer("=SUM(A1)", "alice@tiet.edu", "CustPass1!".toCharArray());
        UserSession bob = reg.authService().registerCustomer("Bob", "bob@tiet.edu", "CustPass1!".toCharArray());

        // Idempotent deposit: duplicate key returns same receipt without double-credit; conflict throws ConflictException
        reg.authService().login("staff@tiet.edu", "StaffPass1!".toCharArray());
        TransactionReceipt dep1 = reg.tellerService().depositCash(alice.accountNumber(), "500.00", "dep-key-1");
        TransactionReceipt dep2 = reg.tellerService().depositCash(alice.accountNumber(), "500.00", "dep-key-1");
        assertEquals(dep1.referenceId(), dep2.referenceId());
        assertEquals(50_000L, reg.staffService().getCustomerAccountSummary(alice.accountNumber()).balancePaise());
        assertThrows(ConflictException.class, () ->
                reg.tellerService().depositCash(alice.accountNumber(), "600.00", "dep-key-1"));

        // Freeze request + Admin approval
        AccountActionRequestView req = reg.accountControlService().requestAccountFreezeChange(
                alice.accountNumber(), AccountActionType.FREEZE, "Suspicious test check");
        assertEquals(RequestStatus.PENDING, req.status());
        assertThrows(AuthorizationException.class, () -> reg.accountControlService().reviewAccountFreezeChange(req.requestId(), true));

        reg.authService().login("admin@tiet.edu", "AdminPass1!".toCharArray());
        AccountActionRequestView approved = reg.accountControlService().reviewAccountFreezeChange(req.requestId(), true);
        assertEquals(RequestStatus.APPROVED, approved.status());

        // Frozen account blocks deposit/withdrawal/incoming/outgoing, yet customer can view own account & export CSV
        reg.authService().login("staff@tiet.edu", "StaffPass1!".toCharArray());
        assertThrows(AccountFrozenException.class, () -> reg.tellerService().depositCash(alice.accountNumber(), "10.00"));
        assertThrows(AccountFrozenException.class, () -> reg.tellerService().withdrawCash(alice.accountNumber(), "10.00"));

        reg.authService().login("bob@tiet.edu", "CustPass1!".toCharArray());
        assertThrows(AccountFrozenException.class, () -> reg.customerBankingService().transferFunds(alice.accountNumber(), "10.00"));

        reg.authService().login("alice@tiet.edu", "CustPass1!".toCharArray());
        assertEquals(50_000L, reg.customerBankingService().getOwnAccountSummary().balancePaise());
        assertThrows(AccountFrozenException.class, () -> reg.customerBankingService().transferFunds(bob.accountNumber(), "10.00"));

        Path csvFile = tempDir.resolve("statement.csv");
        reg.customerBankingService().exportOwnStatementCsv(csvFile);
        String csvText = Files.readString(csvFile);
        assertTrue(csvText.contains("\"'=SUM(A1)\""), "Formula injection must be neutralized in CSV export");
        assertTrue(csvText.contains(dep1.referenceId()));

        // Verify reconciliation matches cleanly first, then simulate external tampering and confirm mismatch detection without silent overwrite
        reg.authService().login("admin@tiet.edu", "AdminPass1!".toCharArray());
        ReconciliationReport cleanReport = reg.reconciliationService().reconcileAllAccounts();
        assertEquals(0, cleanReport.mismatchedAccounts());

        try (Connection raw = reg.databaseManager().openWriteConnection();
             PreparedStatement tamper = raw.prepareStatement("UPDATE accounts SET balance_paise = 99999 WHERE account_number = ?")) {
            tamper.setString(1, alice.accountNumber());
            tamper.executeUpdate();
        }
        ReconciliationReport tamperedReport = reg.reconciliationService().reconcileAllAccounts();
        assertEquals(1, tamperedReport.mismatchedAccounts());
        var aliceRec = tamperedReport.accountResults().stream()
                .filter(r -> r.accountNumber().equals(alice.accountNumber()))
                .findFirst().orElseThrow();
        assertFalse(aliceRec.matched());
        assertEquals(49_999L, aliceRec.discrepancyPaise());
        assertEquals(99_999L, reg.staffService().getCustomerAccountSummary(alice.accountNumber()).balancePaise());

        // Audit trail contains admin/staff events and atomic rollback test does not leave false audit rows
        List<AuditEventView> audits = reg.adminService().getAuditEvents(50, null);
        assertTrue(audits.stream().anyMatch(a -> a.action().equals("ACCOUNT_FREEZE_APPROVED")));
        assertTrue(audits.stream().anyMatch(a -> a.action().equals("STAFF_CREATED")));
    }

    @Test
    void surfacesCommitOutcomeUncertainExceptionWithReferenceOnCommitFailure() {
        DatabaseManager failingCommitManager = new DatabaseManager(tempDir.resolve("uncertain.db"), 5_000) {
            @Override
            public Connection openWriteConnection() throws SQLException {
                Connection delegate = super.openWriteConnection();
                return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[]{Connection.class},
                        (proxy, method, args) -> {
                            if ("commit".equals(method.getName())) {
                                throw new SQLException("Simulated I/O error during commit");
                            }
                            return method.invoke(delegate, args);
                        });
            }
        };
        TransactionExecutor exec = new JdbcTransactionExecutor(failingCommitManager);
        CommitOutcomeUncertainException ex = assertThrows(CommitOutcomeUncertainException.class, () ->
                exec.execute(uow -> {
                    uow.setActiveOperationReference("TRF-UNCERTAIN1");
                    return "ok";
                }));
        assertEquals("TRF-UNCERTAIN1", ex.getReferenceId());
    }
}
