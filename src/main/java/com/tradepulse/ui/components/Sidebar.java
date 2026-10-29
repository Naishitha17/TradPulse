package com.tradepulse.ui.components;

import com.tradepulse.model.Role;
import com.tradepulse.model.User;
import com.tradepulse.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Sidebar extends VBox {
    public enum NavItem {
        DASHBOARD("📊", "Dashboard"),
        MARKET("📈", "Market Watch"),
        PORTFOLIO("💼", "Portfolio"),
        ORDERS("📜", "Orders & Trades"),
        SOCIAL("👥", "Social Community"),
        NEWS("📰", "Financial News"),
        ALERTS("🔔", "Price Alerts"),
        FEEDBACK("💬", "Feedback"),
        SETTINGS("⚙️", "Settings"),
        ADMIN("🛡️", "Admin Panel");

        final String icon;
        final String label;

        NavItem(String icon, String label) {
            this.icon = icon;
            this.label = label;
        }
    }

    private final AuthService authService;
    private final Consumer<NavItem> onNavSelected;
    private final Runnable onLogout;
    private final Map<NavItem, Button> navButtons = new HashMap<>();
    private NavItem activeItem = NavItem.DASHBOARD;
    private boolean isCollapsed = false;

    private final VBox navContainer = new VBox(4);
    private final Label brandLabel = new Label("TradePulse");
    private final Label nexusBadge = new Label("NEXUS");
    private final VBox userCard = new VBox(2);
    private final Label userNameLabel = new Label("User");
    private final Label userRoleLabel = new Label("@trader");

    public Sidebar(AuthService authService, Consumer<NavItem> onNavSelected, Runnable onLogout) {
        this.authService = authService;
        this.onNavSelected = onNavSelected;
        this.onLogout = onLogout;

        getStyleClass().add("sidebar");
        setPrefWidth(220);
        setMinWidth(70);
        setMaxWidth(240);
        setPadding(new Insets(16, 12, 16, 12));
        setSpacing(14);

        buildUI();
    }

    private void buildUI() {
        // Brand Title Header
        HBox brandBox = new HBox(6);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(4, 8, 12, 8));

        brandLabel.getStyleClass().add("brand-title");
        nexusBadge.getStyleClass().add("brand-badge");

        brandBox.getChildren().addAll(brandLabel, nexusBadge);

        // Navigation Items
        for (NavItem item : NavItem.values()) {
            Button btn = createNavButton(item);
            navButtons.put(item, btn);
            navContainer.getChildren().add(btn);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // User Profile Card & Logout
        userCard.setStyle("-fx-background-color: #111827; -fx-padding: 10 12 10 12; -fx-background-radius: 8px; " +
                "-fx-border-color: #1e293b; -fx-border-radius: 8px;");

        userNameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        userRoleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        HBox userRow = new HBox(8);
        userRow.setAlignment(Pos.CENTER_LEFT);

        Label avatarIcon = new Label("👤");
        avatarIcon.setStyle("-fx-font-size: 14px; -fx-background-color: #1e293b; -fx-padding: 4 6 4 6; -fx-background-radius: 20px;");

        VBox userText = new VBox(1);
        userText.getChildren().addAll(userNameLabel, userRoleLabel);

        userRow.getChildren().addAll(avatarIcon, userText);

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("btn-secondary");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            if (onLogout != null) onLogout.run();
        });

        userCard.getChildren().addAll(userRow, new Separator(), logoutBtn);

        getChildren().addAll(brandBox, navContainer, spacer, userCard);

        updateActiveButton(NavItem.DASHBOARD);
        refreshUser();
    }

    private Button createNavButton(NavItem item) {
        Button btn = new Button(item.icon + "  " + item.label);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            updateActiveButton(item);
            if (onNavSelected != null) onNavSelected.accept(item);
        });
        return btn;
    }

    public void updateActiveButton(NavItem item) {
        this.activeItem = item;
        for (Map.Entry<NavItem, Button> entry : navButtons.entrySet()) {
            Button b = entry.getValue();
            b.getStyleClass().remove("nav-button-active");
            if (entry.getKey() == item) {
                b.getStyleClass().add("nav-button-active");
            }
        }
    }

    public void refreshUser() {
        User u = authService.getCurrentUser();
        if (u != null) {
            userNameLabel.setText(u.getDisplayName());
            userRoleLabel.setText("@" + u.getUsername() + (u.isAdmin() ? " [Admin]" : ""));
            // Hide or show Admin item based on Role
            Button adminBtn = navButtons.get(NavItem.ADMIN);
            if (adminBtn != null) {
                adminBtn.setVisible(u.isAdmin());
                adminBtn.setManaged(u.isAdmin());
            }
        }
    }
}
