package com.tiet.banking.bootstrap;

import com.tiet.banking.ui.common.AppNavigator;
import com.tiet.banking.util.AppConfig;
import com.tiet.banking.util.AppLogger;
import javafx.application.Application;
import javafx.stage.Stage;

public class BankingApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        AppLogger.configure();
        ServiceRegistry services = new ServiceRegistry(AppConfig.loadDefault());
        AppNavigator navigator = new AppNavigator(primaryStage, services);
        navigator.showInitialScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
