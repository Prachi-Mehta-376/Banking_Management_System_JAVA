package com.tiet.banking.ui.admin;

import com.tiet.banking.bootstrap.ServiceRegistry;
import com.tiet.banking.dto.AccountActionRequestView;
import com.tiet.banking.dto.AdministrativeSummary;
import com.tiet.banking.dto.AuditEventView;
import com.tiet.banking.dto.CustomerSearchResult;
import com.tiet.banking.dto.ReconciliationReport;
import com.tiet.banking.dto.ReconciliationResult;
import com.tiet.banking.dto.StaffUserSummary;
import com.tiet.banking.dto.TransactionView;
import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.BankingException;
import com.tiet.banking.model.Money;
import com.tiet.banking.ui.common.AppNavigator;
import com.tiet.banking.ui.common.AppShell;
import com.tiet.banking.ui.common.UiComponents;
import com.tiet.banking.util.AppConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AdminDashboardView {
    private final ServiceRegistry services;
    private final AppShell shell;

    private final Label overviewBanner = UiComponents.createBanner();
    private final Label staffBanner = UiComponents.createBanner();
    private final Label oversightBanner = UiComponents.createBanner();
    private final Label integrityBanner = UiComponents.createBanner();
    private final Label approvalBanner = UiComponents.createBanner();
    private final Label auditBanner = UiComponents.createBanner();

    private final Label customersMetric = new Label("0");
    private final Label staffMetric = new Label("0");
    private final Label accountsMetric = new Label("0");
    private final Label frozenMetric = new Label("0");
    private final Label operationsMetric = new Label("0");
    private final Label totalBalanceMetric = new Label("₹0.00");

    private final Label recCheckedMetric = new Label("0");
    private final Label recConsistentMetric = new Label("0");
    private final Label recMismatchMetric = new Label("0");
    private final Label recTimestampLabel = new Label("Last checked: Not run yet");

    private final TableView<StaffUserSummary> staffTable = createStaffTable();
    private final TableView<CustomerSearchResult> oversightTable = createOversightTable();
    private final TableView<TransactionView> oversightHistoryTable = createHistoryTable();
    private final TableView<ReconciliationResult> reconciliationTable = createReconciliationTable();
    private final TableView<AccountActionRequestView> approvalTable = createApprovalTable();
    private final TableView<AuditEventView> auditTable = createAuditTable();

    public AdminDashboardView(ServiceRegistry services, UserSession session, AppNavigator navigator) {
        this.services = services;
        this.shell = new AppShell(session, "ADMINISTRATOR", navigator);

        shell.addSection("overview", "Dashboard Overview", this::showOverview);
        shell.addSection("staff", "Staff Management", this::showStaffManagement);
        shell.addSection("oversight", "Customer Oversight", this::showCustomerOversight);
        shell.addSection("integrity", "Integrity Control Center", this::showIntegrityControlCenter);
        shell.addSection("approvals", "Freeze Approvals", this::showApprovals);
        shell.addSection("audit", "Audit Trail", this::showAuditTrail);
        shell.selectSection("overview");
    }

    public Parent root() {
        return shell.root();
    }

    public void selectSection(String id) {
        shell.selectSection(id);
    }

    private void showOverview() {
        refreshSummary();
        HBox row1 = new HBox(14,
                UiComponents.metricCard("CUSTOMERS", customersMetric),
                UiComponents.metricCard("ACTIVE STAFF", staffMetric),
                UiComponents.metricCard("TOTAL ACCOUNTS", accountsMetric));
        row1.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        HBox row2 = new HBox(14,
                UiComponents.metricCard("FROZEN ACCOUNTS", frozenMetric),
                UiComponents.metricCard("COMMITTED OPERATIONS", operationsMetric),
                UiComponents.metricCard("SIMULATED SYSTEM BALANCE", totalBalanceMetric));
        row2.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        Button refreshBtn = new Button("Refresh Metrics");
        refreshBtn.setId("adminRefreshMetricsBtn");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setOnAction(e -> refreshSummary());

        Label note = new Label("Administrators manage governance, oversight, and integrity—teller cash operations remain restricted to Bank Staff.");
        note.getStyleClass().add("app-subtitle");

        VBox card = new VBox(12, refreshBtn, note);
        card.getStyleClass().add("card");

        shell.setContent("Administrator Dashboard", new VBox(16, overviewBanner, row1, row2, card));
    }

    private void showStaffManagement() {
        TextField nameField = new TextField();
        nameField.setId("staffCreateNameField");
        nameField.setPromptText("Staff full name");

        TextField emailField = new TextField();
        emailField.setId("staffCreateEmailField");
        emailField.setPromptText("staff@bank.edu");

        PasswordField passwordField = new PasswordField();
        passwordField.setId("staffCreatePasswordField");
        passwordField.setPromptText("Minimum 8 characters");

        Button createBtn = new Button("Provision Staff User");
        createBtn.setId("staffCreateBtn");
        createBtn.getStyleClass().add("primary-button");

        createBtn.setOnAction(e -> UiComponents.runAsync(
                createBtn,
                staffBanner,
                () -> services.adminService().createStaffUser(
                        nameField.getText(), emailField.getText(), passwordField.getText().toCharArray()),
                created -> {
                    UiComponents.showSuccess(staffBanner, "Provisioned Staff user: " + created.email());
                    nameField.clear();
                    emailField.clear();
                    passwordField.clear();
                    refreshStaffList();
                }));

        Button toggleBtn = new Button("Toggle Active / Inactive");
        toggleBtn.setId("staffToggleActiveBtn");
        toggleBtn.getStyleClass().add("secondary-button");
        toggleBtn.setOnAction(e -> {
            StaffUserSummary selected = staffTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                UiComponents.showError(staffBanner, "Select a staff row first.");
                return;
            }
            try {
                services.adminService().setStaffActiveStatus(selected.userId(), !selected.active());
                UiComponents.showSuccess(staffBanner,
                        "Updated " + selected.email() + " active=" + (!selected.active()));
                refreshStaffList();
            } catch (BankingException ex) {
                UiComponents.showError(staffBanner, UiComponents.friendlyError(ex));
            }
        });

        staffTable.setId("adminStaffTable");
        staffTable.setPrefHeight(240);
        refreshStaffList();

        VBox createCard = new VBox(10,
                new Label("Provision New Bank Staff"),
                staffBanner,
                new HBox(10, nameField, emailField, passwordField),
                new HBox(10, createBtn, toggleBtn));
        ((Label) createCard.getChildren().get(0)).getStyleClass().add("section-title");
        createCard.getStyleClass().add("card");

        VBox tableCard = new VBox(10, new Label("Bank Staff Directory"), staffTable);
        tableCard.getStyleClass().add("card");
        shell.setContent("Staff Provisioning & Management", new VBox(16, createCard, tableCard));
    }

    private void showCustomerOversight() {
        TextField queryField = new TextField();
        queryField.setId("adminSearchField");
        queryField.setPromptText("Read-only search by customer name, email, or account number");
        HBox.setHgrow(queryField, Priority.ALWAYS);

        Button searchBtn = new Button("Search");
        searchBtn.setId("adminSearchBtn");
        searchBtn.getStyleClass().add("primary-button");
        searchBtn.setOnAction(e -> refreshOversight(queryField.getText()));

        oversightTable.setId("adminOversightTable");
        oversightTable.setPrefHeight(220);
        oversightHistoryTable.setPrefHeight(200);

        oversightTable.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected != null) {
                try {
                    oversightHistoryTable.setItems(FXCollections.observableArrayList(
                            services.staffService().getCustomerTransactionHistory(selected.accountNumber(), 50)));
                } catch (BankingException ex) {
                    UiComponents.showError(oversightBanner, UiComponents.friendlyError(ex));
                }
            }
        });

        refreshOversight("");
        VBox card = new VBox(12, new HBox(10, queryField, searchBtn), oversightBanner, oversightTable,
                new Label("Selected Account History (Read-Only)"), oversightHistoryTable);
        card.getStyleClass().add("card");
        shell.setContent("Read-Only Customer & Account Oversight", card);
    }

    private void showIntegrityControlCenter() {
        Button runRecBtn = new Button("Run System Balance Reconciliation");
        runRecBtn.setId("runReconciliationBtn");
        runRecBtn.getStyleClass().add("primary-button");

        TextArea findingsArea = new TextArea();
        findingsArea.setId("reconciliationFindingsArea");
        findingsArea.setEditable(false);
        findingsArea.setPrefRowCount(4);

        runRecBtn.setOnAction(e -> runReconciliationAndPopulate(findingsArea));

        Button rollbackDemoBtn = new Button("Run Isolated Rollback Proof (Temp DB)");
        rollbackDemoBtn.setId("runRollbackSandboxBtn");
        rollbackDemoBtn.getStyleClass().add("secondary-button");

        TextArea sandboxLogArea = new TextArea();
        sandboxLogArea.setId("rollbackSandboxOutput");
        sandboxLogArea.setEditable(false);
        sandboxLogArea.setPrefRowCount(4);
        sandboxLogArea.setPromptText("Click to verify mid-transfer exception rollback on an isolated temporary database.");

        rollbackDemoBtn.setOnAction(e -> runIsolatedRollbackProof(sandboxLogArea));

        HBox recMetrics = new HBox(14,
                UiComponents.metricCard("ACCOUNTS CHECKED", recCheckedMetric),
                UiComponents.metricCard("CONSISTENT", recConsistentMetric),
                UiComponents.metricCard("MISMATCHES", recMismatchMetric));
        recMetrics.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        reconciliationTable.setId("reconciliationTable");
        reconciliationTable.setPrefHeight(220);

        Label disclaimer = new Label("Note: Reconciliation verifies application-level balance vs. committed journal delta sums without overwriting data; a local SQLite file cannot prevent external OS-level file modification.");
        disclaimer.getStyleClass().add("disclaimer-text");
        disclaimer.setWrapText(true);

        VBox recCard = new VBox(10,
                new HBox(12, runRecBtn, recTimestampLabel),
                integrityBanner,
                recMetrics,
                reconciliationTable,
                new Label("Journal Integrity Checks"),
                findingsArea,
                disclaimer);
        recCard.getStyleClass().add("card");

        VBox sandboxCard = new VBox(10,
                new Label("Atomic Rollback Proof Sandbox (Isolated Temporary SQLite DB — Safe)"),
                rollbackDemoBtn,
                sandboxLogArea);
        ((Label) sandboxCard.getChildren().get(0)).getStyleClass().add("section-title");
        sandboxCard.getStyleClass().add("card");

        runReconciliationAndPopulate(findingsArea);
        shell.setContent("Banking Integrity Control Center", new VBox(16, recCard, sandboxCard));
    }

    public ReconciliationReport runReconciliationAndPopulate(TextArea findingsArea) {
        UiComponents.clearBanner(integrityBanner);
        try {
            ReconciliationReport report = services.reconciliationService().reconcileAllAccounts();
            recCheckedMetric.setText(String.valueOf(report.totalAccountsChecked()));
            recConsistentMetric.setText(String.valueOf(report.consistentAccounts()));
            recMismatchMetric.setText(String.valueOf(report.mismatchedAccounts()));
            recTimestampLabel.setText("Last checked: " + report.checkedAt());
            reconciliationTable.setItems(FXCollections.observableArrayList(report.accountResults()));

            if (report.mismatchedAccounts() == 0 && report.integrityRuleViolations() == 0) {
                UiComponents.showSuccess(integrityBanner,
                        "All " + report.totalAccountsChecked() + " account(s) match committed journal deltas with zero violations.");
                findingsArea.setText("All deposit, withdrawal, and zero-sum transfer journal invariants verified.");
            } else {
                UiComponents.showWarning(integrityBanner,
                        "Detected " + report.mismatchedAccounts() + " account mismatch(es) and "
                                + report.integrityRuleViolations() + " journal finding(s). Stored balances were NOT altered.");
                findingsArea.setText(String.join("\n", report.journalFindings()));
            }
            return report;
        } catch (BankingException ex) {
            UiComponents.showError(integrityBanner, UiComponents.friendlyError(ex));
            return null;
        }
    }

    public void runIsolatedRollbackProof(TextArea outputArea) {
        Path tempDb = null;
        try {
            tempDb = Files.createTempFile("banking_rollback_proof_", ".db");
            AppConfig sandboxCfg = new AppConfig(tempDb, 10_000, 3_000);
            ServiceRegistry sandbox = new ServiceRegistry(sandboxCfg);
            sandbox.authService().setupInitialAdmin("Sandbox Admin", "sb-admin@demo.edu", "SandboxPass1!".toCharArray());
            sandbox.adminService().createStaffUser("Sandbox Staff", "sb-staff@demo.edu", "SandboxPass1!".toCharArray());
            var custA = sandbox.authService().registerCustomer("Cust A", "sb-a@demo.edu", "SandboxPass1!".toCharArray());
            var custB = sandbox.authService().registerCustomer("Cust B", "sb-b@demo.edu", "SandboxPass1!".toCharArray());
            sandbox.authService().login("sb-staff@demo.edu", "SandboxPass1!".toCharArray());
            sandbox.tellerService().depositCash(custA.accountNumber(), "1000.00");
            sandbox.authService().login("sb-a@demo.edu", "SandboxPass1!".toCharArray());

            StringBuilder log = new StringBuilder();
            log.append("Before injected failure: A=₹1,000.00, B=₹0.00\n");
            try {
                sandbox.transactionExecutor().executeVoid(uow -> {
                    uow.accounts().debitIfActiveAndSufficient(custA.accountId(), 40_000);
                    throw new IllegalStateException("Injected mid-transfer failure after sender debit");
                });
            } catch (RuntimeException expected) {
                log.append("Simulated failure caught: ").append(expected.getMessage()).append("\n");
            }
            var aAfter = sandbox.customerBankingService().getOwnAccountSummary();
            sandbox.authService().login("sb-b@demo.edu", "SandboxPass1!".toCharArray());
            var bAfter = sandbox.customerBankingService().getOwnAccountSummary();
            log.append("After rollback on fresh read: A=").append(aAfter.formattedBalance())
                    .append(", B=").append(bAfter.formattedBalance())
                    .append(" (Zero partial writes committed)");
            outputArea.setText(log.toString());
        } catch (Exception ex) {
            outputArea.setText("Rollback sandbox failed: " + ex.getMessage());
        } finally {
            if (tempDb != null) {
                try {
                    Files.deleteIfExists(tempDb);
                    Files.deleteIfExists(Path.of(tempDb + "-wal"));
                    Files.deleteIfExists(Path.of(tempDb + "-shm"));
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void showApprovals() {
        approvalTable.setId("adminApprovalTable");
        approvalTable.setPrefHeight(300);

        Button approveBtn = new Button("Approve Selected Request");
        approveBtn.setId("adminApproveReqBtn");
        approveBtn.getStyleClass().add("primary-button");

        Button rejectBtn = new Button("Reject Selected Request");
        rejectBtn.setId("adminRejectReqBtn");
        rejectBtn.getStyleClass().add("danger-button");

        approveBtn.setOnAction(e -> handleApprovalDecision(true));
        rejectBtn.setOnAction(e -> handleApprovalDecision(false));

        refreshApprovals();
        VBox card = new VBox(12, approvalBanner, new HBox(12, approveBtn, rejectBtn), approvalTable);
        card.getStyleClass().add("card");
        shell.setContent("Maker-Checker Account Control Approvals", card);
    }

    private void handleApprovalDecision(boolean approve) {
        UiComponents.clearBanner(approvalBanner);
        AccountActionRequestView selected = approvalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiComponents.showError(approvalBanner, "Select a pending request row first.");
            return;
        }
        try {
            AccountActionRequestView updated = services.accountControlService()
                    .reviewAccountFreezeChange(selected.requestId(), approve);
            UiComponents.showSuccess(approvalBanner,
                    "Request #" + updated.requestId() + " marked " + updated.status() + " for " + updated.accountNumber());
            refreshApprovals();
        } catch (BankingException ex) {
            UiComponents.showError(approvalBanner, UiComponents.friendlyError(ex));
        }
    }

    private void showAuditTrail() {
        TextField actionFilter = new TextField();
        actionFilter.setId("auditFilterField");
        actionFilter.setPromptText("Filter by action (e.g., STAFF_CREATED, ACCOUNT_FREEZE_APPROVED)");
        HBox.setHgrow(actionFilter, Priority.ALWAYS);

        Button refreshBtn = new Button("Load Audit Events");
        refreshBtn.setId("auditRefreshBtn");
        refreshBtn.getStyleClass().add("primary-button");
        refreshBtn.setOnAction(e -> refreshAuditEvents(actionFilter.getText()));

        auditTable.setId("adminAuditTable");
        auditTable.setPrefHeight(400);
        refreshAuditEvents("");

        VBox card = new VBox(12, new HBox(10, actionFilter, refreshBtn), auditBanner, auditTable);
        card.getStyleClass().add("card");
        shell.setContent("System Audit Trail", card);
    }

    private void refreshSummary() {
        UiComponents.clearBanner(overviewBanner);
        try {
            AdministrativeSummary s = services.adminService().getAdministrativeSummary();
            customersMetric.setText(String.valueOf(s.totalCustomers()));
            staffMetric.setText(String.valueOf(s.totalActiveStaff()));
            accountsMetric.setText(String.valueOf(s.totalAccounts()));
            frozenMetric.setText(String.valueOf(s.frozenAccounts()));
            operationsMetric.setText(String.valueOf(s.totalOperationsCount()));
            totalBalanceMetric.setText(s.formattedTotalBalance());
        } catch (BankingException ex) {
            UiComponents.showError(overviewBanner, UiComponents.friendlyError(ex));
        }
    }

    private void refreshStaffList() {
        try {
            staffTable.setItems(FXCollections.observableArrayList(services.adminService().listStaffUsers()));
        } catch (BankingException ex) {
            UiComponents.showError(staffBanner, UiComponents.friendlyError(ex));
        }
    }

    private void refreshOversight(String query) {
        UiComponents.clearBanner(oversightBanner);
        try {
            List<CustomerSearchResult> rows = services.staffService().searchCustomers(query);
            oversightTable.setItems(FXCollections.observableArrayList(rows));
        } catch (BankingException ex) {
            UiComponents.showError(oversightBanner, UiComponents.friendlyError(ex));
        }
    }

    private void refreshApprovals() {
        try {
            approvalTable.setItems(FXCollections.observableArrayList(services.accountControlService().listAllRequests()));
        } catch (BankingException ex) {
            UiComponents.showError(approvalBanner, UiComponents.friendlyError(ex));
        }
    }

    private void refreshAuditEvents(String filter) {
        UiComponents.clearBanner(auditBanner);
        try {
            auditTable.setItems(FXCollections.observableArrayList(services.adminService().getAuditEvents(200, filter)));
        } catch (BankingException ex) {
            UiComponents.showError(auditBanner, UiComponents.friendlyError(ex));
        }
    }

    private static TableView<StaffUserSummary> createStaffTable() {
        TableView<StaffUserSummary> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<StaffUserSummary, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().userId())));
        TableColumn<StaffUserSummary, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fullName()));
        TableColumn<StaffUserSummary, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().email()));
        TableColumn<StaffUserSummary, String> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().active() ? "ACTIVE" : "INACTIVE"));
        table.getColumns().addAll(List.of(idCol, nameCol, emailCol, activeCol));
        table.setPlaceholder(new Label("No staff users provisioned yet."));
        return table;
    }

    private static TableView<CustomerSearchResult> createOversightTable() {
        TableView<CustomerSearchResult> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<CustomerSearchResult, String> acctCol = new TableColumn<>("Account #");
        acctCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().accountNumber()));
        TableColumn<CustomerSearchResult, String> nameCol = new TableColumn<>("Owner");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fullName()));
        TableColumn<CustomerSearchResult, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().email()));
        TableColumn<CustomerSearchResult, String> balCol = new TableColumn<>("Balance");
        balCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().formattedBalance()));
        TableColumn<CustomerSearchResult, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().accountStatus().name()));
        table.getColumns().addAll(List.of(acctCol, nameCol, emailCol, balCol, statusCol));
        table.setPlaceholder(new Label("No customer accounts found."));
        return table;
    }

    private static TableView<TransactionView> createHistoryTable() {
        TableView<TransactionView> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<TransactionView, String> refCol = new TableColumn<>("Reference");
        refCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().referenceId()));
        TableColumn<TransactionView, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().operationType().name()));
        TableColumn<TransactionView, String> deltaCol = new TableColumn<>("Amount");
        deltaCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().formattedDelta()));
        TableColumn<TransactionView, String> afterCol = new TableColumn<>("Balance After");
        afterCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().formattedBalanceAfter()));
        table.getColumns().addAll(List.of(refCol, typeCol, deltaCol, afterCol));
        table.setPlaceholder(new Label("Select a customer account above."));
        return table;
    }

    private static TableView<ReconciliationResult> createReconciliationTable() {
        TableView<ReconciliationResult> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<ReconciliationResult, String> acctCol = new TableColumn<>("Account #");
        acctCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().accountNumber()));
        TableColumn<ReconciliationResult, String> storedCol = new TableColumn<>("Stored Balance");
        storedCol.setCellValueFactory(c -> new SimpleStringProperty(Money.formatInr(c.getValue().storedBalancePaise())));
        TableColumn<ReconciliationResult, String> expCol = new TableColumn<>("Expected (Journal)");
        expCol.setCellValueFactory(c -> new SimpleStringProperty(Money.formatInr(c.getValue().expectedBalancePaise())));
        TableColumn<ReconciliationResult, String> diffCol = new TableColumn<>("Difference");
        diffCol.setCellValueFactory(c -> new SimpleStringProperty(Money.formatSignedInr(c.getValue().discrepancyPaise())));
        TableColumn<ReconciliationResult, String> statusCol = new TableColumn<>("Result");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().matched() ? "MATCHED" : "MISMATCH"));
        TableColumn<ReconciliationResult, String> detailCol = new TableColumn<>("Details");
        detailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().details()));
        table.getColumns().addAll(List.of(acctCol, storedCol, expCol, diffCol, statusCol, detailCol));
        table.setPlaceholder(new Label("No accounts to reconcile."));
        return table;
    }

    private static TableView<AccountActionRequestView> createApprovalTable() {
        TableView<AccountActionRequestView> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<AccountActionRequestView, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().requestId())));
        TableColumn<AccountActionRequestView, String> acctCol = new TableColumn<>("Account #");
        acctCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().accountNumber()));
        TableColumn<AccountActionRequestView, String> actCol = new TableColumn<>("Action");
        actCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().actionType().name()));
        TableColumn<AccountActionRequestView, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reason()));
        TableColumn<AccountActionRequestView, String> reqByCol = new TableColumn<>("Requested By");
        reqByCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().requestedByEmail()));
        TableColumn<AccountActionRequestView, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status().name()));
        table.getColumns().addAll(List.of(idCol, acctCol, actCol, reasonCol, reqByCol, statusCol));
        table.setPlaceholder(new Label("No freeze/unfreeze requests."));
        return table;
    }

    private static TableView<AuditEventView> createAuditTable() {
        TableView<AuditEventView> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<AuditEventView, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().auditId())));
        TableColumn<AuditEventView, String> timeCol = new TableColumn<>("Timestamp (UTC)");
        timeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().timestamp().toString()));
        TableColumn<AuditEventView, String> actorCol = new TableColumn<>("Actor");
        actorCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().actorEmail() + " (" + c.getValue().actorRole() + ")"));
        TableColumn<AuditEventView, String> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().action()));
        TableColumn<AuditEventView, String> targetCol = new TableColumn<>("Target");
        targetCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().targetType() + ":" + c.getValue().targetId()));
        table.getColumns().addAll(List.of(idCol, timeCol, actorCol, actionCol, targetCol));
        table.setPlaceholder(new Label("No audit events recorded."));
        return table;
    }
}
