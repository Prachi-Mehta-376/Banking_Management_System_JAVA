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

public class CustomerRegistrationView {
    private final StackPane root = new StackPane();

    public CustomerRegistrationView(AuthService authService, AppNavigator navigator) {
        root.getStyleClass().add("auth-root");

        VBox card = new VBox(12);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(440);

        Label title = new Label("Customer Account Registration");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Creates one synthetic customer profile and an active ₹0.00 account.");
        subtitle.getStyleClass().add("app-subtitle");
        subtitle.setWrapText(true);

        Label errorBanner = UiComponents.createBanner();

        TextField nameField = new TextField();
        nameField.setId("regNameField");
        nameField.setPromptText("Full name (synthetic demo)");

        TextField emailField = new TextField();
        emailField.setId("regEmailField");
        emailField.setPromptText("customer@example.com");

        PasswordField passwordField = new PasswordField();
        passwordField.setId("regPasswordField");
        passwordField.setPromptText("Minimum 8 characters");

        Button registerButton = new Button("Register & Open Account");
        registerButton.setId("registerSubmitButton");
        registerButton.getStyleClass().add("primary-button");
        registerButton.setMaxWidth(Double.MAX_VALUE);

        Button backButton = new Button("Back to Sign In");
        backButton.setId("backToLoginButton");
        backButton.getStyleClass().add("secondary-button");
        backButton.setMaxWidth(Double.MAX_VALUE);

        registerButton.setOnAction(e -> {
            UiComponents.clearBanner(errorBanner);
            try {
                UserSession session = authService.registerCustomer(
                        nameField.getText(), emailField.getText(), passwordField.getText().toCharArray());
                navigator.showDashboardForSession(session);
            } catch (BankingException ex) {
                UiComponents.showError(errorBanner, UiComponents.friendlyError(ex));
            }
        });

        backButton.setOnAction(e -> navigator.showLogin());

        card.getChildren().addAll(
                title, subtitle, errorBanner,
                new Label("Full Name"), nameField,
                new Label("Email"), emailField,
                new Label("Password"), passwordField,
                registerButton, backButton);

        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().add(card);
    }

    public Parent root() {
        return root;
    }
}
