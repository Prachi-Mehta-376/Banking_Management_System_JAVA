package com.tiet.banking.ui.common;

import com.tiet.banking.dto.TransactionReceipt;
import com.tiet.banking.exception.BankingException;
import com.tiet.banking.exception.CommitOutcomeUncertainException;
import com.tiet.banking.model.AccountStatus;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class UiComponents {
    private UiComponents() {
    }

    public static VBox metricCard(String title, Label valueLabel) {
        Label header = new Label(title);
        header.getStyleClass().add("metric-label");
        valueLabel.getStyleClass().add("metric-value");
        VBox box = new VBox(4, header, valueLabel);
        box.getStyleClass().add("metric-card");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinWidth(180);
        return box;
    }

    public static Label createBanner() {
        Label banner = new Label();
        banner.setWrapText(true);
        banner.setMaxWidth(Double.MAX_VALUE);
        banner.setVisible(false);
        banner.setManaged(false);
        return banner;
    }

    public static void showError(Label banner, String message) {
        setBanner(banner, message, "banner-error");
    }

    public static void showSuccess(Label banner, String message) {
        setBanner(banner, message, "banner-success");
    }

    public static void showWarning(Label banner, String message) {
        setBanner(banner, message, "banner-warning");
    }

    public static void clearBanner(Label banner) {
        banner.setText("");
        banner.setVisible(false);
        banner.setManaged(false);
    }

    private static void setBanner(Label banner, String message, String styleClass) {
        banner.getStyleClass().removeAll("banner-error", "banner-success", "banner-warning");
        banner.getStyleClass().add(styleClass);
        banner.setText(message);
        banner.setVisible(true);
        banner.setManaged(true);
    }

    public static void applyStatusStyle(Label badge, AccountStatus status) {
        badge.getStyleClass().removeAll("status-active", "status-frozen");
        if (status == AccountStatus.ACTIVE) {
            badge.setText("ACTIVE");
            badge.getStyleClass().add("status-active");
        } else {
            badge.setText("FROZEN");
            badge.getStyleClass().add("status-frozen");
        }
    }

    public static String formatReceiptSummary(TransactionReceipt receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ref: ").append(receipt.referenceId())
                .append(" | ").append(receipt.operationType())
                .append(" | Amount: ").append(receipt.formattedAmount())
                .append(" | Account: ").append(receipt.accountNumber());
        if (receipt.counterpartyAccountNumber() != null) {
            sb.append(" -> ").append(receipt.counterpartyAccountNumber());
        }
        sb.append(" | New Balance: ").append(receipt.formattedBalanceAfter());
        return sb.toString();
    }

    public static String friendlyError(Throwable error) {
        if (error instanceof CommitOutcomeUncertainException uncertain) {
            String ref = uncertain.getReferenceId() == null ? "" : " (Reference: " + uncertain.getReferenceId() + ")";
            return "The transaction outcome is uncertain" + ref
                    + ". Do not retry blindly—check your transaction history first.";
        }
        if (error instanceof BankingException) {
            return error.getMessage();
        }
        return "An unexpected error occurred while processing the request.";
    }

    public static <T> void runAsync(
            Button triggerButton,
            Label feedbackBanner,
            Supplier<T> work,
            Consumer<T> onSuccess) {
        triggerButton.setDisable(true);
        clearBanner(feedbackBanner);

        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };

        task.setOnSucceeded(evt -> {
            triggerButton.setDisable(false);
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(evt -> {
            triggerButton.setDisable(false);
            Throwable ex = task.getException();
            if (ex instanceof CommitOutcomeUncertainException) {
                showWarning(feedbackBanner, friendlyError(ex));
            } else {
                showError(feedbackBanner, friendlyError(ex));
            }
        });

        Thread worker = new Thread(task, "banking-ui-worker");
        worker.setDaemon(true);
        worker.start();
    }
}
