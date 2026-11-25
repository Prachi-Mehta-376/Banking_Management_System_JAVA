package com.tiet.banking.ui.common;

import com.tiet.banking.dto.UserSession;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AppShell {
    private final BorderPane root = new BorderPane();
    private final VBox navItemsBox = new VBox(6);
    private final StackPane contentContainer = new StackPane();
    private final Label pageHeading = new Label();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final Map<String, Runnable> navHandlers = new LinkedHashMap<>();

    public AppShell(UserSession session, String roleTitle, AppNavigator navigator) {
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");

        Label brand = new Label("TIET CoreBank");
        brand.getStyleClass().add("sidebar-brand");
        Label roleLabel = new Label(roleTitle + " PORTAL");
        roleLabel.getStyleClass().add("sidebar-role");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutButton = new Button("Logout");
        logoutButton.setId("logoutButton");
        logoutButton.getStyleClass().add("nav-button");
        logoutButton.setOnAction(e -> navigator.logoutAndReturnToLogin());

        Label simNote = new Label("Educational Simulation\nSynthetic Funds Only");
        simNote.setStyle("-fx-text-fill: #8BA0B5; -fx-font-size: 11px;");

        sidebar.getChildren().addAll(brand, roleLabel, navItemsBox, spacer, logoutButton, simNote);

        HBox topbar = new HBox(12);
        topbar.getStyleClass().add("topbar");
        topbar.setAlignment(Pos.CENTER_LEFT);
        pageHeading.getStyleClass().add("section-title");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        Label userBadge = new Label(session.fullName() + " (" + session.email() + ")");
        userBadge.getStyleClass().add("app-subtitle");
        topbar.getChildren().addAll(pageHeading, topSpacer, userBadge);

        ScrollPane scrollPane = new ScrollPane(contentContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #F4F6F9; -fx-background: #F4F6F9;");
        contentContainer.setPadding(new Insets(22));

        BorderPane centerArea = new BorderPane();
        centerArea.setTop(topbar);
        centerArea.setCenter(scrollPane);

        root.setLeft(sidebar);
        root.setCenter(centerArea);
    }

    public void addSection(String id, String label, Runnable onSelect) {
        Button btn = new Button(label);
        btn.setId("nav_" + id);
        btn.getStyleClass().add("nav-button");
        btn.setOnAction(e -> selectSection(id));
        navButtons.put(id, btn);
        navHandlers.put(id, onSelect);
        navItemsBox.getChildren().add(btn);
    }

    public void selectSection(String id) {
        navButtons.forEach((key, btn) -> {
            btn.getStyleClass().remove("nav-button-active");
            if (key.equals(id)) {
                btn.getStyleClass().add("nav-button-active");
            }
        });
        Runnable handler = navHandlers.get(id);
        if (handler != null) {
            handler.run();
        }
    }

    public void setContent(String heading, Node content) {
        pageHeading.setText(heading);
        contentContainer.getChildren().setAll(content);
    }

    public BorderPane root() {
        return root;
    }
}
