package com.tiet.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tiet.banking.bootstrap.ServiceRegistry;
import com.tiet.banking.dto.StaffUserSummary;
import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.AuthenticationException;
import com.tiet.banking.exception.AuthorizationException;
import com.tiet.banking.exception.ConflictException;
import com.tiet.banking.model.Role;
import com.tiet.banking.model.User;
import com.tiet.banking.util.AppConfig;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthAndRbacTest {

    @TempDir
    Path tempDir;

    @Test
    void enforcesOneTimeAdminBootstrapAndBlocksRepeatEvenIfAdminDeactivated() {
        ServiceRegistry reg = new ServiceRegistry(new AppConfig(tempDir.resolve("auth.db"), 10_000, 5_000));
        assertTrue(reg.authService().isInitialAdminSetupRequired());

        UserSession admin = reg.authService().setupInitialAdmin("Root Admin", "admin@tiet.edu", "AdminPass1!".toCharArray());
        assertEquals(Role.ADMIN, admin.role());
        assertFalse(reg.authService().isInitialAdminSetupRequired());

        assertThrows(ConflictException.class, () ->
                reg.authService().setupInitialAdmin("Second Admin", "admin2@tiet.edu", "AdminPass1!".toCharArray()));

        // Even if the admin row were deactivated, bootstrap must never re-open.
        reg.transactionExecutor().executeVoid(uow -> uow.users().updateActiveStatus(admin.userId(), false));
        assertFalse(reg.authService().isInitialAdminSetupRequired());
        assertThrows(ConflictException.class, () ->
                reg.authService().setupInitialAdmin("Rogue Admin", "rogue@tiet.edu", "AdminPass1!".toCharArray()));
    }

    @Test
    void hashesPasswordsWithRandomSaltsRejectsDuplicatesAndEnforcesRoleBoundaries() {
        ServiceRegistry reg = new ServiceRegistry(new AppConfig(tempDir.resolve("rbac.db"), 10_000, 5_000));
        reg.authService().setupInitialAdmin("Admin", "admin@tiet.edu", "AdminPass1!".toCharArray());
        StaffUserSummary staff = reg.adminService().createStaffUser("Teller", "staff@tiet.edu", "StaffPass1!".toCharArray());

        // Admin cannot perform teller cash deposits/withdrawals, yet has read-only search access.
        assertThrows(AuthorizationException.class, () -> reg.tellerService().depositCash("AC000001", "100.00"));
        assertThrows(AuthorizationException.class, () -> reg.tellerService().withdrawCash("AC000001", "50.00"));
        assertTrue(reg.staffService().searchCustomers("").isEmpty());

        UserSession alice = reg.authService().registerCustomer("Alice", "alice@tiet.edu", "SharedPass1!".toCharArray());
        assertEquals(0L, reg.customerBankingService().getOwnAccountSummary().balancePaise());
        assertThrows(ConflictException.class, () ->
                reg.authService().registerCustomer("Alice Dup", "ALICE@tiet.edu", "SharedPass1!".toCharArray()));

        UserSession bob = reg.authService().registerCustomer("Bob", "bob@tiet.edu", "SharedPass1!".toCharArray());

        // Verify distinct random salts and non-plaintext storage.
        User aliceRow = reg.transactionExecutor().executeReadOnly(uow -> uow.users().findById(alice.userId())).orElseThrow();
        User bobRow = reg.transactionExecutor().executeReadOnly(uow -> uow.users().findById(bob.userId())).orElseThrow();
        assertNotEquals("SharedPass1!", aliceRow.passwordHash());
        assertNotEquals(aliceRow.passwordSalt(), bobRow.passwordSalt());
        assertNotEquals(aliceRow.passwordHash(), bobRow.passwordHash());

        // Customer cannot call staff, teller, or admin endpoints.
        assertThrows(AuthorizationException.class, () -> reg.staffService().searchCustomers("Alice"));
        assertThrows(AuthorizationException.class, () -> reg.staffService().getCustomerAccountSummary(alice.accountNumber()));
        assertThrows(AuthorizationException.class, () -> reg.tellerService().depositCash(bob.accountNumber(), "100.00"));
        assertThrows(AuthorizationException.class, () -> reg.adminService().listStaffUsers());

        // Bad password rejected; deactivated staff rejected.
        reg.authService().logout();
        assertThrows(AuthenticationException.class, () -> reg.authService().login("bob@tiet.edu", "WrongPass1!".toCharArray()));

        reg.authService().login("admin@tiet.edu", "AdminPass1!".toCharArray());
        reg.adminService().setStaffActiveStatus(staff.userId(), false);
        reg.authService().logout();
        assertThrows(AuthenticationException.class, () -> reg.authService().login("staff@tiet.edu", "StaffPass1!".toCharArray()));
    }
}
