package com.tiet.banking.ui.customer;

import com.tiet.banking.bootstrap.ServiceRegistry;
import com.tiet.banking.dto.AccountSummary;
import com.tiet.banking.dto.TransactionReceipt;
import com.tiet.banking.dto.TransactionView;
import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.BankingException;
import com.tiet.banking.ui.common.AppNavigator;
import com.tiet.banking.ui.common.AppShell;
import com.tiet.banking.ui.common.UiComponents;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class CustomerDashboardView {
    private final ServiceRegistry services;
    private final AppNavigator navigator;
    private final AppShell shell;

    private final Label accountNumberValue = new Label("-");
    private final Label balanceValue = new Label("₹0.00");
    private final Label statusBadge = new Label("ACTIVE");
    private final Label overviewBanner = UiComponents.createBanner();
    private final Label transferBanner = UiComponents.createBanner();
    private final Label historyBanner = UiComponents.createBanner();

    private final TableView<TransactionView> recentTable = createTransactionTable();
    private final TableView<TransactionView> fullHistoryTable = createTransactionTable();

    public CustomerDashboardView(ServiceRegistry services, UserSession session, AppNavigator navigator) {
        this.services = services;
        this.navigator = navigator;
        this.shell = new AppShell(session, "CUSTOMER", navigator);

        shell.addSection("overview", "Account Overview", this::showOverview);
        shell.addSection("transfer", "Transfer Funds", this::showTransfer);
        shell.addSection("history", "Transaction History", this::showHistory);
        shell.selectSection("overview");
    }

    public Parent root() {
        return shell.root();
    }

    public void selectSection(String id) {
        shell.selectSection(id);
    }

    private void showOverview() {
        refreshOverviewData();
        VBox statusCard = new VBox(6, new Label("ACCOUNT STATUS"), statusBadge);
        statusCard.getStyleClass().add("metric-card");
        statusCard.setAlignment(Pos.CENTER_LEFT);
        statusCard.setMinWidth(160);
        ((Label) statusCard.getChildren().get(0)).getStyleClass().add("metric-label");

        HBox metrics = new HBox(16,
                UiComponents.metricCard("ACCOUNT NUMBER", accountNumberValue),
                UiComponents.metricCard("AVAILABLE BALANCE", balanceValue),
                statusCard);
        metrics.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        Button refreshButton = new Button("Refresh Account");
        refreshButton.setId("customerRefreshBtn");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(e -> refreshOverviewData());

        Label recentLabel = new Label("Recent Transactions");
        recentLabel.getStyleClass().add("section-title");
        recentTable.setId("customerRecentTable");
        recentTable.setPrefHeight(280);

        VBox tableCard = new VBox(12, new HBox(12, recentLabel, refreshButton), recentTable);
        tableCard.getStyleClass().add("card");

        VBox page = new VBox(16, overviewBanner, metrics, tableCard);
        shell.setContent("Customer Account Overview", page);
    }

    private void showTransfer() {
        refreshOverviewData();
        TextField destField = new TextField();
        destField.setId("transferDestField");
        destField.setPromptText("e.g., AC000002");

        TextField amountField = new TextField();
        amountField.setId("transferAmountField");
        amountField.setPromptText("e.g., 250.00");

        Button submitButton = new Button("Send Transfer");
        submitButton.setId("transferSubmitBtn");
        submitButton.getStyleClass().add("primary-button");

        Label balanceHint = new Label("Available Balance: " + balanceValue.getText() + " | From: " + accountNumberValue.getText());
        balanceHint.getStyleClass().add("app-subtitle");

        submitButton.setOnAction(e -> UiComponents.runAsync(
                submitButton,
                transferBanner,
                () -> services.customerBankingService().transferFunds(destField.getText(), amountField.getText()),
                receipt -> {
                    UiComponents.showSuccess(transferBanner, "Transfer Completed — " + UiComponents.formatReceiptSummary(receipt));
                    destField.clear();
                    amountField.clear();
                    refreshOverviewData();
                    balanceHint.setText("Available Balance: " + balanceValue.getText() + " | From: " + accountNumberValue.getText());
                }));

        VBox formCard = new VBox(12,
                new Label("Internal Customer Transfer"),
                balanceHint,
                transferBanner,
                new Label("Recipient Account Number"), destField,
                new Label("Amount (INR)"), amountField,
                submitButton);
        ((Label) formCard.getChildren().get(0)).getStyleClass().add("section-title");
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(520);

        shell.setContent("Transfer Funds", new VBox(16, formCard));
    }

    private void showHistory() {
        refreshHistoryData();
        Button refreshButton = new Button("Refresh");
        refreshButton.setId("historyRefreshBtn");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(e -> refreshHistoryData());

        Button exportButton = new Button("Export CSV Statement");
        exportButton.setId("exportCsvBtn");
        exportButton.getStyleClass().add("primary-button");
        exportButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Account Statement (CSV)");
            chooser.setInitialFileName("statement_" + accountNumberValue.getText() + ".csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File chosen = chooser.showSaveDialog(navigator.stage());
            if (chosen != null) {
                exportStatementTo(chosen.toPath());
            }
        });

        HBox actions = new HBox(12, refreshButton, exportButton);
        fullHistoryTable.setId("customerHistoryTable");
        fullHistoryTable.setPrefHeight(420);

        VBox card = new VBox(12, actions, historyBanner, fullHistoryTable);
        card.getStyleClass().add("card");
        shell.setContent("Transaction History & Statement Export", card);
    }

    public Path exportStatementTo(Path targetPath) {
        UiComponents.clearBanner(historyBanner);
        try {
            Path saved = services.customerBankingService().exportOwnStatementCsv(targetPath);
            UiComponents.showSuccess(historyBanner, "CSV statement exported to: " + saved.toAbsolutePath());
            return saved;
        } catch (BankingException ex) {
            UiComponents.showError(historyBanner, UiComponents.friendlyError(ex));
            return null;
        }
    }

    public void refreshOverviewData() {
        UiComponents.clearBanner(overviewBanner);
        try {
            AccountSummary summary = services.customerBankingService().getOwnAccountSummary();
            accountNumberValue.setText(summary.accountNumber());
            balanceValue.setText(summary.formattedBalance());
            UiComponents.applyStatusStyle(statusBadge, summary.status());
            List<TransactionView> recent = services.customerBankingService().getOwnTransactionHistory(10);
            recentTable.setItems(FXCollections.observableArrayList(recent));
        } catch (BankingException ex) {
            UiComponents.showError(overviewBanner, UiComponents.friendlyError(ex));
        }
    }

    private void refreshHistoryData() {
        UiComponents.clearBanner(historyBanner);
        try {
            AccountSummary summary = services.customerBankingService().getOwnAccountSummary();
            accountNumberValue.setText(summary.accountNumber());
            balanceValue.setText(summary.formattedBalance());
            List<TransactionView> rows = services.customerBankingService().getOwnTransactionHistory(200);
            fullHistoryTable.setItems(FXCollections.observableArrayList(rows));
        } catch (BankingException ex) {
            UiComponents.showError(historyBanner, UiComponents.friendlyError(ex));
        }
    }

    private static TableView<TransactionView> createTransactionTable() {
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

        TableColumn<TransactionView, String> actorCol = new TableColumn<>("Initiated By");
        actorCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().performedByRole()));

        TableColumn<TransactionView, String> timeCol = new TableColumn<>("Timestamp (UTC)");
        timeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().timestamp().toString()));

        table.getColumns().addAll(List.of(refCol, typeCol, deltaCol, afterCol, actorCol, timeCol));
        table.setPlaceholder(new Label("No transactions recorded yet."));
        return table;
    }
}
