package com.tiet.banking.ui.auth;

import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.BankingException;
import com.tiet.banking.service.AuthService;
import com.tiet.banking.ui.common.AppNavigator;
import com.tiet.banking.ui.common.UiComponents;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class FirstRunAdminView {
    private final StackPane root = new StackPane();

    public FirstRunAdminView(AuthService authService, AppNavigator navigator) {
        root.getStyleClass().add("auth-root");

        VBox card = new VBox(12);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(440);

        Label title = new Label("Initial Administrator Setup");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("First-run security bootstrap. No default admin password exists.");
        subtitle.getStyleClass().add("app-subtitle");
        subtitle.setWrapText(true);

        Label errorBanner = UiComponents.createBanner();

        TextField nameField = new TextField();
        nameField.setId("adminNameField");
        nameField.setPromptText("Administrator full name");

        TextField emailField = new TextField();
        emailField.setId("adminEmailField");
        emailField.setPromptText("admin@bank.edu");

        PasswordField passwordField = new PasswordField();
        passwordField.setId("adminPasswordField");
        passwordField.setPromptText("Minimum 8 characters");

        PasswordField confirmField = new PasswordField();
        confirmField.setId("adminConfirmPasswordField");
        confirmField.setPromptText("Re-enter password");

        Button createButton = new Button("Create Initial Administrator");
        createButton.setId("createAdminButton");
        createButton.getStyleClass().add("primary-button");
        createButton.setMaxWidth(Double.MAX_VALUE);

        createButton.setOnAction(e -> {
            UiComponents.clearBanner(errorBanner);
            String pwd = passwordField.getText();
            if (!pwd.equals(confirmField.getText())) {
                UiComponents.showError(errorBanner, "Passwords do not match.");
                return;
            }
            try {
                UserSession session = authService.setupInitialAdmin(
                        nameField.getText(), emailField.getText(), pwd.toCharArray());
                navigator.showDashboardForSession(session);
            } catch (BankingException ex) {
                UiComponents.showError(errorBanner, UiComponents.friendlyError(ex));
            }
        });

        Label disclaimer = new Label("Educational simulation (UCS553). Local SQLite storage.");
        disclaimer.getStyleClass().add("disclaimer-text");

        card.getChildren().addAll(
                title, subtitle, errorBanner,
                new Label("Full Name"), nameField,
                new Label("Email"), emailField,
                new Label("Password"), passwordField,
                new Label("Confirm Password"), confirmField,
                createButton, disclaimer);

        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().add(card);
    }

    public Parent root() {
        return root;
    }
}
