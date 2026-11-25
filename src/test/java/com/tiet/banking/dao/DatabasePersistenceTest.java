package com.tiet.banking.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tiet.banking.bootstrap.ServiceRegistry;
import com.tiet.banking.dto.TransactionReceipt;
import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.PersistenceException;
import com.tiet.banking.model.BankAccount;
import com.tiet.banking.model.Role;
import com.tiet.banking.model.User;
import com.tiet.banking.util.AppConfig;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabasePersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    void enforcesForeignKeysAndNonNegativeBalanceCheckConstraints() throws Exception {
        Path dbFile = tempDir.resolve("constraints.db");
        ServiceRegistry registry = new ServiceRegistry(new AppConfig(dbFile, 10_000, 5_000));

        // Direct SQL violating FK or negative CHECK constraint must be rejected by SQLite.
        try (Connection conn = registry.databaseManager().openWriteConnection()) {
            try (PreparedStatement fkPs = conn.prepareStatement(
                    "INSERT INTO accounts (user_id, account_number, balance_paise, status, created_at) VALUES (9999, 'AC999999', 0, 'ACTIVE', ?)")) {
                fkPs.setString(1, Instant.now().toString());
                assertThrows(SQLException.class, fkPs::executeUpdate);
            }

            registry.authService().registerCustomer("Demo User", "demo@tiet.edu", "SecurePass1!".toCharArray());
            try (PreparedStatement negPs = conn.prepareStatement(
                    "UPDATE accounts SET balance_paise = -100 WHERE account_number = 'AC000001'")) {
                assertThrows(SQLException.class, negPs::executeUpdate);
            }

            // Deletion of referenced user must be blocked by ON DELETE RESTRICT.
            try (PreparedStatement delPs = conn.prepareStatement("DELETE FROM users WHERE email = 'demo@tiet.edu'")) {
                assertThrows(SQLException.class, delPs::executeUpdate);
            }
        }
    }

    @Test
    void generatesSequentialAccountNumbersAndPersistsAcrossRestart() {
        Path dbFile = tempDir.resolve("restart.db");
        AppConfig cfg = new AppConfig(dbFile, 10_000, 5_000);

        ServiceRegistry firstRun = new ServiceRegistry(cfg);
        firstRun.authService().setupInitialAdmin("Admin One", "admin@tiet.edu", "AdminPass1!".toCharArray());
        firstRun.adminService().createStaffUser("Staff One", "staff@tiet.edu", "StaffPass1!".toCharArray());
        UserSession cust1 = firstRun.authService().registerCustomer("Alice", "alice@tiet.edu", "CustPass1!".toCharArray());
        UserSession cust2 = firstRun.authService().registerCustomer("Bob", "bob@tiet.edu", "CustPass1!".toCharArray());

        assertEquals("AC000001", cust1.accountNumber());
        assertEquals("AC000002", cust2.accountNumber());

        firstRun.authService().login("staff@tiet.edu", "StaffPass1!".toCharArray());
        firstRun.tellerService().depositCash("AC000001", "750.50");
        firstRun.authService().login("alice@tiet.edu", "CustPass1!".toCharArray());
        TransactionReceipt trf = firstRun.customerBankingService().transferFunds("AC000002", "250.25");

        // Reinitialize a completely new ServiceRegistry pointing to the same SQLite file on disk.
        ServiceRegistry secondRun = new ServiceRegistry(cfg);
        secondRun.authService().login("alice@tiet.edu", "CustPass1!".toCharArray());
        assertEquals(50_025L, secondRun.customerBankingService().getOwnAccountSummary().balancePaise());
        assertEquals(2, secondRun.customerBankingService().getOwnTransactionHistory(20).size());
        assertEquals(trf.referenceId(), secondRun.customerBankingService().getOwnTransactionHistory(20).get(0).referenceId());

        secondRun.authService().login("bob@tiet.edu", "CustPass1!".toCharArray());
        assertEquals(25_025L, secondRun.customerBankingService().getOwnAccountSummary().balancePaise());
    }

    @Test
    void concurrentWithdrawalsSerializeSafelyWithoutNegativeBalance() throws Exception {
        Path dbFile = tempDir.resolve("concurrency.db");
        AppConfig cfg = new AppConfig(dbFile, 10_000, 5_000);
        ServiceRegistry registry = new ServiceRegistry(cfg);

        registry.authService().setupInitialAdmin("Admin", "admin@tiet.edu", "AdminPass1!".toCharArray());
        registry.adminService().createStaffUser("Staff 1", "staff1@tiet.edu", "StaffPass1!".toCharArray());
        registry.adminService().createStaffUser("Staff 2", "staff2@tiet.edu", "StaffPass1!".toCharArray());
        registry.authService().registerCustomer("Concurrent User", "conc@tiet.edu", "CustPass1!".toCharArray());

        registry.authService().login("staff1@tiet.edu", "StaffPass1!".toCharArray());
        registry.tellerService().depositCash("AC000001", "100.00");

        // Each thread uses its own ServiceRegistry & SessionManager against the shared SQLite file.
        ServiceRegistry worker1 = new ServiceRegistry(cfg);
        ServiceRegistry worker2 = new ServiceRegistry(cfg);
        worker1.authService().login("staff1@tiet.edu", "StaffPass1!".toCharArray());
        worker2.authService().login("staff2@tiet.edu", "StaffPass1!".toCharArray());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        List<Callable<Boolean>> tasks = List.of(
                () -> attemptWithdraw(worker1, "AC000001", "80.00", ready, start),
                () -> attemptWithdraw(worker2, "AC000001", "80.00", ready, start));

        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            futures.add(pool.submit(task));
        }
        ready.await();
        start.countDown();

        int successes = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successes++;
            }
        }
        pool.shutdown();

        assertEquals(1, successes, "Exactly one concurrent withdrawal should succeed");
        registry.authService().login("conc@tiet.edu", "CustPass1!".toCharArray());
        assertEquals(2_000L, registry.customerBankingService().getOwnAccountSummary().balancePaise());
        boolean verified = registry.transactionExecutor().executeReadOnly(uow -> {
            User u = uow.users().findByRole(Role.CUSTOMER).get(0);
            BankAccount acct = uow.accounts().findByUserId(u.id()).orElseThrow();
            return acct.balancePaise() == 2_000L;
        });
        assertTrue(verified);
    }

    private boolean attemptWithdraw(
            ServiceRegistry reg, String acct, String amount, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            reg.tellerService().withdrawCash(acct, amount);
            return true;
        } catch (RuntimeException ex) {
            if (ex instanceof PersistenceException) {
                throw ex;
            }
            return false;
        }
    }
}
