package com.tiet.banking.ui.common;

import com.tiet.banking.bootstrap.ServiceRegistry;
import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.BankingException;
import com.tiet.banking.model.Role;
import com.tiet.banking.ui.admin.AdminDashboardView;
import com.tiet.banking.ui.auth.CustomerRegistrationView;
import com.tiet.banking.ui.auth.FirstRunAdminView;
import com.tiet.banking.ui.auth.LoginView;
import com.tiet.banking.ui.customer.CustomerDashboardView;
import com.tiet.banking.ui.staff.StaffDashboardView;
import java.net.URL;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppNavigator {
    private final Stage stage;
    private final ServiceRegistry services;
    private final Scene scene;

    public AppNavigator(Stage stage, ServiceRegistry services) {
        this.stage = stage;
        this.services = services;
        this.scene = new Scene(new javafx.scene.layout.StackPane(), 1160, 740);
        URL cssUrl = AppNavigator.class.getResource("/styles/banking.css");
        if (cssUrl != null) {
            this.scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setTitle("TIET Banking Management System — Educational Simulation");
        stage.setScene(scene);
    }

    public Scene scene() {
        return scene;
    }

    public Stage stage() {
        return stage;
    }

    public void showInitialScreen() {
        try {
            if (services.authService().isInitialAdminSetupRequired()) {
                showFirstRunAdminSetup();
            } else {
                showLogin();
            }
        } catch (BankingException ex) {
            LoginView view = new LoginView(services.authService(), this);
            view.showError(ex.getMessage());
            setRoot(view.root());
        }
    }

    public void showFirstRunAdminSetup() {
        setRoot(new FirstRunAdminView(services.authService(), this).root());
    }

    public void showLogin() {
        setRoot(new LoginView(services.authService(), this).root());
    }

    public void showCustomerRegistration() {
        setRoot(new CustomerRegistrationView(services.authService(), this).root());
    }

    public void showDashboardForSession(UserSession session) {
        if (session.role() == Role.CUSTOMER) {
            setRoot(new CustomerDashboardView(services, session, this).root());
        } else if (session.role() == Role.STAFF) {
            setRoot(new StaffDashboardView(services, session, this).root());
        } else {
            setRoot(new AdminDashboardView(services, session, this).root());
        }
    }

    public void logoutAndReturnToLogin() {
        services.authService().logout();
        showLogin();
    }

    private void setRoot(Parent root) {
        scene.setRoot(root);
    }
}
