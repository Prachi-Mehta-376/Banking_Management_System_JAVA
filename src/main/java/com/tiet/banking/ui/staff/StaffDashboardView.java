package com.tiet.banking.ui.staff;

import com.tiet.banking.bootstrap.ServiceRegistry;
import com.tiet.banking.dto.AccountActionRequestView;
import com.tiet.banking.dto.AccountSummary;
import com.tiet.banking.dto.CustomerSearchResult;
import com.tiet.banking.dto.TransactionView;
import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.BankingException;
import com.tiet.banking.model.AccountActionType;
import com.tiet.banking.ui.common.AppNavigator;
import com.tiet.banking.ui.common.AppShell;
import com.tiet.banking.ui.common.UiComponents;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class StaffDashboardView {
    private final ServiceRegistry services;
    private final AppShell shell;

    private final Label searchBanner = UiComponents.createBanner();
    private final Label cashBanner = UiComponents.createBanner();
    private final Label controlBanner = UiComponents.createBanner();

    private final TableView<CustomerSearchResult> customerTable = createCustomerTable();
    private final TableView<TransactionView> selectedHistoryTable = createTransactionTable();
    private final TableView<AccountActionRequestView> requestsTable = createRequestTable();
    private final Label selectedAccountDetail = new Label("Select a customer account to inspect history.");

    public StaffDashboardView(ServiceRegistry services, UserSession session, AppNavigator navigator) {
        this.services = services;
        this.shell = new AppShell(session, "BANK STAFF", navigator);

        shell.addSection("customers", "Customer Search", this::showCustomerSearch);
        shell.addSection("cash", "Cash Deposit & Withdrawal", this::showCashOperations);
        shell.addSection("controls", "Freeze / Unfreeze Requests", this::showAccountControlRequests);
        shell.selectSection("customers");
    }

    public Parent root() {
        return shell.root();
    }

    public void selectSection(String id) {
        shell.selectSection(id);
    }

    private void showCustomerSearch() {
        TextField queryField = new TextField();
        queryField.setId("staffSearchField");
        queryField.setPromptText("Search by name, email, or account number (e.g., AC000001)");
        HBox.setHgrow(queryField, Priority.ALWAYS);

        Button searchBtn = new Button("Search");
        searchBtn.setId("staffSearchBtn");
        searchBtn.getStyleClass().add("primary-button");
        searchBtn.setOnAction(e -> runCustomerSearch(queryField.getText()));

        customerTable.setId("staffCustomerTable");
        customerTable.setPrefHeight(230);
        customerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                loadSelectedAccountHistory(selected.accountNumber());
            }
        });

        selectedHistoryTable.setId("staffSelectedHistoryTable");
        selectedHistoryTable.setPrefHeight(220);

        VBox searchCard = new VBox(12,
                new HBox(10, queryField, searchBtn),
                searchBanner,
                customerTable);
        searchCard.getStyleClass().add("card");

        VBox detailCard = new VBox(10, selectedAccountDetail, selectedHistoryTable);
        detailCard.getStyleClass().add("card");

        runCustomerSearch("");
        shell.setContent("Customer & Account Search", new VBox(16, searchCard, detailCard));
    }

    private void showCashOperations() {
        TextField accountField = new TextField();
        accountField.setId("tellerAccountField");
        accountField.setPromptText("Account Number (e.g., AC000001)");

        TextField amountField = new TextField();
        amountField.setId("tellerAmountField");
        amountField.setPromptText("Amount (INR), e.g., 500.00");

        Button depositBtn = new Button("Simulated Cash Deposit");
        depositBtn.setId("tellerDepositBtn");
        depositBtn.getStyleClass().add("primary-button");

        Button withdrawBtn = new Button("Simulated Cash Withdrawal");
        withdrawBtn.setId("tellerWithdrawBtn");
        withdrawBtn.getStyleClass().add("secondary-button");

        depositBtn.setOnAction(e -> UiComponents.runAsync(
                depositBtn,
                cashBanner,
                () -> services.tellerService().depositCash(accountField.getText(), amountField.getText()),
                receipt -> {
                    UiComponents.showSuccess(cashBanner, "Deposit Recorded — " + UiComponents.formatReceiptSummary(receipt));
                    amountField.clear();
                    loadSelectedAccountHistory(receipt.accountNumber());
                }));

        withdrawBtn.setOnAction(e -> UiComponents.runAsync(
                withdrawBtn,
                cashBanner,
                () -> services.tellerService().withdrawCash(accountField.getText(), amountField.getText()),
                receipt -> {
                    UiComponents.showSuccess(cashBanner, "Withdrawal Recorded — " + UiComponents.formatReceiptSummary(receipt));
                    amountField.clear();
                    loadSelectedAccountHistory(receipt.accountNumber());
                }));

        HBox buttons = new HBox(12, depositBtn, withdrawBtn);
        VBox formCard = new VBox(12,
                new Label("Simulated Over-The-Counter Cash Operations"),
                cashBanner,
                new Label("Customer Account Number"), accountField,
                new Label("Amount (INR)"), amountField,
                buttons);
        ((Label) formCard.getChildren().get(0)).getStyleClass().add("section-title");
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(560);

        VBox historyCard = new VBox(10, selectedAccountDetail, selectedHistoryTable);
        historyCard.getStyleClass().add("card");
        selectedHistoryTable.setPrefHeight(240);

        shell.setContent("Teller Cash Deposit & Withdrawal", new VBox(16, formCard, historyCard));
    }

    private void showAccountControlRequests() {
        TextField accountField = new TextField();
        accountField.setId("freezeReqAccountField");
        accountField.setPromptText("Account Number (e.g., AC000001)");

        ComboBox<AccountActionType> actionBox = new ComboBox<>(FXCollections.observableArrayList(AccountActionType.values()));
        actionBox.setId("freezeReqActionBox");
        actionBox.setValue(AccountActionType.FREEZE);

        TextField reasonField = new TextField();
        reasonField.setId("freezeReqReasonField");
        reasonField.setPromptText("Justification for freeze/unfreeze request");

        Button submitBtn = new Button("Submit Request for Admin Review");
        submitBtn.setId("freezeReqSubmitBtn");
        submitBtn.getStyleClass().add("primary-button");

        submitBtn.setOnAction(e -> UiComponents.runAsync(
                submitBtn,
                controlBanner,
                () -> services.accountControlService().requestAccountFreezeChange(
                        accountField.getText(), actionBox.getValue(), reasonField.getText()),
                created -> {
                    UiComponents.showSuccess(controlBanner,
                            "Request #" + created.requestId() + " (" + created.actionType() + ") submitted for " + created.accountNumber());
                    accountField.clear();
                    reasonField.clear();
                    refreshRequestsTable();
                }));

        VBox formCard = new VBox(10,
                new Label("Submit Freeze / Unfreeze Request (Maker)"),
                controlBanner,
                new Label("Account Number"), accountField,
                new Label("Requested Action"), actionBox,
                new Label("Reason"), reasonField,
                submitBtn);
        ((Label) formCard.getChildren().get(0)).getStyleClass().add("section-title");
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(560);

        requestsTable.setId("staffRequestsTable");
        requestsTable.setPrefHeight(250);
        refreshRequestsTable();

        VBox listCard = new VBox(10, new Label("Account Action Requests"), requestsTable);
        listCard.getStyleClass().add("card");
        shell.setContent("Account Freeze / Unfreeze Requests", new VBox(16, formCard, listCard));
    }

    private void runCustomerSearch(String query) {
        UiComponents.clearBanner(searchBanner);
        try {
            List<CustomerSearchResult> rows = services.staffService().searchCustomers(query);
            customerTable.setItems(FXCollections.observableArrayList(rows));
            if (!rows.isEmpty()) {
                loadSelectedAccountHistory(rows.get(0).accountNumber());
            }
        } catch (BankingException ex) {
            UiComponents.showError(searchBanner, UiComponents.friendlyError(ex));
        }
    }

    private void loadSelectedAccountHistory(String accountNumber) {
        try {
            AccountSummary summary = services.staffService().getCustomerAccountSummary(accountNumber);
            selectedAccountDetail.setText("Account " + summary.accountNumber() + " | Owner: " + summary.ownerName()
                    + " | Balance: " + summary.formattedBalance() + " | Status: " + summary.status());
            List<TransactionView> tx = services.staffService().getCustomerTransactionHistory(accountNumber, 50);
            selectedHistoryTable.setItems(FXCollections.observableArrayList(tx));
        } catch (BankingException ex) {
            selectedAccountDetail.setText("Unable to load account details: " + ex.getMessage());
        }
    }

    private void refreshRequestsTable() {
        try {
            requestsTable.setItems(FXCollections.observableArrayList(services.accountControlService().listAllRequests()));
        } catch (BankingException ex) {
            UiComponents.showError(controlBanner, UiComponents.friendlyError(ex));
        }
    }

    private static TableView<CustomerSearchResult> createCustomerTable() {
        TableView<CustomerSearchResult> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<CustomerSearchResult, String> acctCol = new TableColumn<>("Account #");
        acctCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().accountNumber()));

        TableColumn<CustomerSearchResult, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fullName()));

        TableColumn<CustomerSearchResult, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().email()));

        TableColumn<CustomerSearchResult, String> balCol = new TableColumn<>("Balance");
        balCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().formattedBalance()));

        TableColumn<CustomerSearchResult, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().accountStatus().name()));

        table.getColumns().addAll(List.of(acctCol, nameCol, emailCol, balCol, statusCol));
        table.setPlaceholder(new Label("No matching customer accounts."));
        return table;
    }

    private static TableView<TransactionView> createTransactionTable() {
        TableView<TransactionView> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TransactionView, String> refCol = new TableColumn<>("Reference");
        refCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().referenceId()));
        TableColumn<TransactionView, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().operationType().name()));
        TableColumn<TransactionView, String> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().formattedDelta()));
        TableColumn<TransactionView, String> afterCol = new TableColumn<>("Balance After");
        afterCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().formattedBalanceAfter()));
        TableColumn<TransactionView, String> timeCol = new TableColumn<>("Timestamp (UTC)");
        timeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().timestamp().toString()));

        table.getColumns().addAll(List.of(refCol, typeCol, amtCol, afterCol, timeCol));
        table.setPlaceholder(new Label("No transactions yet."));
        return table;
    }

    private static TableView<AccountActionRequestView> createRequestTable() {
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
        TableColumn<AccountActionRequestView, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status().name()));

        table.getColumns().addAll(List.of(idCol, acctCol, actCol, reasonCol, statusCol));
        table.setPlaceholder(new Label("No account action requests submitted."));
        return table;
    }
}
