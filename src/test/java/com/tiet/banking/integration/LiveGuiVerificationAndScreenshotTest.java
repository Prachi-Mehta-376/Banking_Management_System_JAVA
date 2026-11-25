package com.tiet.banking.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tiet.banking.bootstrap.ServiceRegistry;
import com.tiet.banking.ui.admin.AdminDashboardView;
import com.tiet.banking.ui.common.AppNavigator;
import com.tiet.banking.ui.customer.CustomerDashboardView;
import com.tiet.banking.ui.staff.StaffDashboardView;
import com.tiet.banking.util.AppConfig;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("gui")
class LiveGuiVerificationAndScreenshotTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void startJavaFxToolkit() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
    }

    @Test
    void exercisesCompleteGuiWorkflowAcrossRolesAndCapturesGenuineScreenshots() throws Exception {
        Path dbFile = tempDir.resolve("gui_e2e.db");
        String configuredScreenshotDir = System.getProperty("banking.screenshots.dir");
        Path screenshotDir = (configuredScreenshotDir != null && !configuredScreenshotDir.isBlank())
                ? Path.of(configuredScreenshotDir)
                : tempDir.resolve("screenshots");
        Files.createDirectories(screenshotDir);

        AppConfig config = new AppConfig(dbFile, 10_000, 5_000);
        ServiceRegistry services = new ServiceRegistry(config);

        AtomicReference<Stage> stageRef = new AtomicReference<>();
        AtomicReference<AppNavigator> navRef = new AtomicReference<>();

        runOnFxAndWait(() -> {
            Stage stage = new Stage();
            AppNavigator navigator = new AppNavigator(stage, services);
            navigator.showInitialScreen();
            stage.show();
            stageRef.set(stage);
            navRef.set(navigator);
            return null;
        });

        try {
            // 1. First-run Administrator Setup screen
            captureSnapshot(navRef.get().scene(), screenshotDir.resolve("01_first_run_admin_setup.png"));
            runOnFxAndWait(() -> {
                Scene scene = navRef.get().scene();
                lookup(scene, "#adminNameField", TextField.class).setText("System Admin");
                lookup(scene, "#adminEmailField", TextField.class).setText("admin@bank.edu");
                lookup(scene, "#adminPasswordField", PasswordField.class).setText("AdminPass123!");
                lookup(scene, "#adminConfirmPasswordField", PasswordField.class).setText("AdminPass123!");
                lookup(scene, "#createAdminButton", Button.class).fire();
                return null;
            });

            // 2. Admin provisions Bank Staff via live UI
            runOnFxAndWait(() -> {
                Scene scene = navRef.get().scene();
                lookup(scene, "#nav_staff", Button.class).fire();
                lookup(scene, "#staffCreateNameField", TextField.class).setText("Priya Teller");
                lookup(scene, "#staffCreateEmailField", TextField.class).setText("staff@bank.edu");
                lookup(scene, "#staffCreatePasswordField", PasswordField.class).setText("StaffPass123!");
                lookup(scene, "#staffCreateBtn", Button.class).fire();
                return null;
            });
            waitUntilDisabledCleared(navRef.get().scene(), "#staffCreateBtn");

            // 3. Logout to Login screen -> capture screenshot -> navigate to Customer Registration
            runOnFxAndWait(() -> {
                lookup(navRef.get().scene(), "#logoutButton", Button.class).fire();
                return null;
            });
            captureSnapshot(navRef.get().scene(), screenshotDir.resolve("02_login_screen.png"));

            // Register Customer B first, then Customer A (so A ends logged in)
            runOnFxAndWait(() -> {
                lookup(navRef.get().scene(), "#goToRegisterButton", Button.class).fire();
                Scene scene = navRef.get().scene();
                lookup(scene, "#regNameField", TextField.class).setText("Rohan Verma");
                lookup(scene, "#regEmailField", TextField.class).setText("rohan@demo.edu");
                lookup(scene, "#regPasswordField", PasswordField.class).setText("CustomerPass1!");
                lookup(scene, "#registerSubmitButton", Button.class).fire();
                return null;
            });
            runOnFxAndWait(() -> {
                lookup(navRef.get().scene(), "#logoutButton", Button.class).fire();
                lookup(navRef.get().scene(), "#goToRegisterButton", Button.class).fire();
                return null;
            });
            runOnFxAndWait(() -> {
                Scene scene = navRef.get().scene();
                lookup(scene, "#regNameField", TextField.class).setText("Ananya Sharma");
                lookup(scene, "#regEmailField", TextField.class).setText("ananya@demo.edu");
                lookup(scene, "#regPasswordField", PasswordField.class).setText("CustomerPass1!");
                return null;
            });
            captureSnapshot(navRef.get().scene(), screenshotDir.resolve("03_customer_registration.png"));
            runOnFxAndWait(() -> {
                lookup(navRef.get().scene(), "#registerSubmitButton", Button.class).fire();
                return null;
            });

            // 4. Staff logs in -> searches customers -> deposits & withdraws simulated cash
            runOnFxAndWait(() -> {
                lookup(navRef.get().scene(), "#logoutButton", Button.class).fire();
                Scene scene = navRef.get().scene();
                lookup(scene, "#loginEmailField", TextField.class).setText("staff@bank.edu");
                lookup(scene, "#loginPasswordField", PasswordField.class).setText("StaffPass123!");
                lookup(scene, "#loginButton", Button.class).fire();
                return null;
            });
            runOnFxAndWait(() -> {
                Scene scene = navRef.get().scene();
                lookup(scene, "#nav_cash", Button.class).fire();
                lookup(scene, "#tellerAccountField", TextField.class).setText("AC000002");
                lookup(scene, "#tellerAmountField", TextField.class).setText("2500.00");
                lookup(scene, "#tellerDepositBtn", Button.class).fire();
                return null;
            });
            waitUntilDisabledCleared(navRef.get().scene(), "#tellerDepositBtn");

            runOnFxAndWait(() -> {
                Scene scene = navRef.get().scene();
                lookup(scene, "#tellerAmountField", TextField.class).setText("500.00");
                lookup(scene, "#tellerWithdrawBtn", Button.class).fire();
                return null;
            });
            waitUntilDisabledCleared(navRef.get().scene(), "#tellerWithdrawBtn");
            captureSnapshot(navRef.get().scene(), screenshotDir.resolve("04_staff_dashboard_cash_ops.png"));

            // Staff submits freeze request for AC000001 (to demonstrate maker-checker in Admin UI)
            runOnFxAndWait(() -> {
                Scene scene = navRef.get().scene();
                lookup(scene, "#nav_controls", Button.class).fire();
                lookup(scene, "#freezeReqAccountField", TextField.class).setText("AC000001");
                lookup(scene, "#freezeReqReasonField", TextField.class).setText("Routine compliance verification demo");
                lookup(scene, "#freezeReqSubmitBtn", Button.class).fire();
                return null;
            });
            waitUntilDisabledCleared(navRef.get().scene(), "#freezeReqSubmitBtn");

            // 5. Customer A logs in -> views dashboard -> attempts invalid transfer -> sends valid transfer -> views history & exports CSV
            runOnFxAndWait(() -> {
                lookup(navRef.get().scene(), "#logoutButton", Button.class).fire();
                Scene scene = navRef.get().scene();
                lookup(scene, "#loginEmailField", TextField.class).setText("ananya@demo.edu");
                lookup(scene, "#loginPasswordField", PasswordField.class).setText("CustomerPass1!");
                lookup(scene, "#loginButton", Button.class).fire();
                return null;
            });
            captureSnapshot(navRef.get().scene(), screenshotDir.resolve("05_customer_dashboard_overview.png"));

            // Invalid transfer (insufficient funds)
            runOnFxAndWait(() -> {
                Scene scene = navRef.get().scene();
                lookup(scene, "#nav_transfer", Button.class).fire();
                lookup(scene, "#transferDestField", TextField.class).setText("AC000001");
                lookup(scene, "#transferAmountField", TextField.class).setText("9999.00");
                lookup(scene, "#transferSubmitBtn", Button.class).fire();
                return null;
            });
            waitUntilDisabledCleared(navRef.get().scene(), "#transferSubmitBtn");
            captureSnapshot(navRef.get().scene(), screenshotDir.resolve("06_customer_validation_error.png"));

            // Valid transfer to AC000001
            runOnFxAndWait(() -> {
                Scene scene = navRef.get().scene();
                lookup(scene, "#transferDestField", TextField.class).setText("AC000001");
                lookup(scene, "#transferAmountField", TextField.class).setText("750.00");
                lookup(scene, "#transferSubmitBtn", Button.class).fire();
                return null;
            });
            waitUntilDisabledCleared(navRef.get().scene(), "#transferSubmitBtn");
            captureSnapshot(navRef.get().scene(), screenshotDir.resolve("07_customer_transfer_receipt.png"));

            // History + CSV export
            Path exportedCsv = tempDir.resolve("ananya_statement.csv");
            runOnFxAndWait(() -> {
                Scene scene = navRef.get().scene();
                lookup(scene, "#nav_history", Button.class).fire();
                TableView<?> table = lookup(scene, "#customerHistoryTable", TableView.class);
                assertEquals(3, table.getItems().size());
                services.customerBankingService().exportOwnStatementCsv(exportedCsv);
                return null;
            });
            assertTrue(Files.exists(exportedCsv));
            captureSnapshot(navRef.get().scene(), screenshotDir.resolve("08_customer_transaction_history.png"));

            // 6. Administrator logs in -> views metrics -> runs Integrity Control Center & Rollback Sandbox -> approves request -> inspects Audit Trail
            runOnFxAndWait(() -> {
                lookup(navRef.get().scene(), "#logoutButton", Button.class).fire();
                Scene scene = navRef.get().scene();
                lookup(scene, "#loginEmailField", TextField.class).setText("admin@bank.edu");
                lookup(scene, "#loginPasswordField", PasswordField.class).setText("AdminPass123!");
                lookup(scene, "#loginButton", Button.class).fire();
                return null;
            });
            captureSnapshot(navRef.get().scene(), screenshotDir.resolve("09_admin_dashboard_overview.png"));

            runOnFxAndWait(() -> {
                Scene scene = navRef.get().scene();
                lookup(scene, "#nav_integrity", Button.class).fire();
                lookup(scene, "#runRollbackSandboxBtn", Button.class).fire();
                TextArea sandboxOut = lookup(scene, "#rollbackSandboxOutput", TextArea.class);
                assertTrue(sandboxOut.getText().contains("Zero partial writes committed"));
                return null;
            });
            captureSnapshot(navRef.get().scene(), screenshotDir.resolve("10_admin_integrity_control_center.png"));

            runOnFxAndWait(() -> {
                Scene scene = navRef.get().scene();
                lookup(scene, "#nav_approvals", Button.class).fire();
                TableView<?> table = lookup(scene, "#adminApprovalTable", TableView.class);
                assertFalse(table.getItems().isEmpty());
                table.getSelectionModel().select(0);
                lookup(scene, "#adminApproveReqBtn", Button.class).fire();
                return null;
            });
            captureSnapshot(navRef.get().scene(), screenshotDir.resolve("11_admin_maker_checker_approvals.png"));

            runOnFxAndWait(() -> {
                Scene scene = navRef.get().scene();
                lookup(scene, "#nav_audit", Button.class).fire();
                TableView<?> auditTable = lookup(scene, "#adminAuditTable", TableView.class);
                assertTrue(auditTable.getItems().size() >= 3);
                return null;
            });
            captureSnapshot(navRef.get().scene(), screenshotDir.resolve("12_admin_audit_trail.png"));

            // 7. Simulate restart: fresh ServiceRegistry + Stage verifying persisted state
            ServiceRegistry restarted = new ServiceRegistry(config);
            runOnFxAndWait(() -> {
                assertFalse(restarted.authService().isInitialAdminSetupRequired());
                var session = restarted.authService().login("ananya@demo.edu", "CustomerPass1!".toCharArray());
                AppNavigator nav2 = new AppNavigator(stageRef.get(), restarted);
                nav2.showDashboardForSession(session);
                assertEquals(125_000L, restarted.customerBankingService().getOwnAccountSummary().balancePaise());
                return null;
            });
        } finally {
            runOnFxAndWait(() -> {
                if (stageRef.get() != null) {
                    stageRef.get().close();
                }
                return null;
            });
        }
    }

    private void waitUntilDisabledCleared(Scene scene, String selector) throws Exception {
        for (int i = 0; i < 100; i++) {
            boolean done = runOnFxAndWait(() -> !lookup(scene, selector, Button.class).isDisabled());
            if (done) {
                return;
            }
            Thread.sleep(30);
        }
        throw new AssertionError("Timed out waiting for async UI action on " + selector);
    }

    private void captureSnapshot(Scene scene, Path outputFile) throws Exception {
        WritableImage image = runOnFxAndWait(() -> {
            Parent root = scene.getRoot();
            root.applyCss();
            root.layout();
            return scene.snapshot(null);
        });
        BufferedImage buffered = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(buffered, "png", outputFile.toFile());
        assertTrue(Files.size(outputFile) > 5_000, "Screenshot should be non-trivial: " + outputFile);
    }

    private <T extends Node> T lookup(Scene scene, String selector, Class<T> type) {
        Node node = scene.lookup(selector);
        if (node == null) {
            throw new AssertionError("UI node not found: " + selector);
        }
        return type.cast(node);
    }

    private <T> T runOnFxAndWait(Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.call());
            } catch (Throwable ex) {
                error.set(ex);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(15, TimeUnit.SECONDS), "Timed out waiting for JavaFX thread");
        if (error.get() != null) {
            if (error.get() instanceof Exception ex) {
                throw ex;
            }
            throw new RuntimeException(error.get());
        }
        return result.get();
    }
}
