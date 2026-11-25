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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class LoginView {
    private final StackPane root = new StackPane();
    private final Label errorBanner = UiComponents.createBanner();

    public LoginView(AuthService authService, AppNavigator navigator) {
        root.getStyleClass().add("auth-root");

        VBox card = new VBox(12);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(420);

        Label title = new Label("Banking Management System");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Sign in as Customer, Bank Staff, or Administrator");
        subtitle.getStyleClass().add("app-subtitle");

        TextField emailField = new TextField();
        emailField.setId("loginEmailField");
        emailField.setPromptText("you@example.com");

        PasswordField passwordField = new PasswordField();
        passwordField.setId("loginPasswordField");
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Sign In");
        loginButton.setId("loginButton");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        Button registerButton = new Button("New Customer Registration");
        registerButton.setId("goToRegisterButton");
        registerButton.getStyleClass().add("secondary-button");
        registerButton.setMaxWidth(Double.MAX_VALUE);

        loginButton.setOnAction(e -> {
            UiComponents.clearBanner(errorBanner);
            try {
                UserSession session = authService.login(
                        emailField.getText(), passwordField.getText().toCharArray());
                navigator.showDashboardForSession(session);
            } catch (BankingException ex) {
                UiComponents.showError(errorBanner, UiComponents.friendlyError(ex));
            }
        });

        registerButton.setOnAction(e -> navigator.showCustomerRegistration());

        HBox footer = new HBox(new Label("UCS553 Enterprise Web Application — Synthetic Simulation"));
        footer.getStyleClass().add("disclaimer-text");
        footer.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
                title, subtitle, errorBanner,
                new Label("Email"), emailField,
                new Label("Password"), passwordField,
                loginButton, registerButton, footer);

        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().add(card);
    }

    public void showError(String message) {
        UiComponents.showError(errorBanner, message);
    }

    public Parent root() {
        return root;
    }
}
