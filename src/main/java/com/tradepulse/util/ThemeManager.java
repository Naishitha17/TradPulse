package com.tradepulse.util;

import com.tradepulse.service.ServiceRegistry;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThemeManager {
    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);
    private static String currentTheme = "DARK";

    public static String getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDark() {
        return "DARK".equalsIgnoreCase(currentTheme);
    }

    public static void setTheme(String theme, Scene scene) {
        if (theme == null || (!theme.equalsIgnoreCase("DARK") && !theme.equalsIgnoreCase("LIGHT"))) {
            theme = "DARK";
        }
        currentTheme = theme.toUpperCase();

        if (scene != null) {
            applyTheme(scene);
        }

        // Persist to user profile if logged in
        try {
            var auth = ServiceRegistry.getInstance().getAuthService();
            var profile = auth.getCurrentProfile();
            if (profile != null) {
                profile.setTheme(currentTheme);
                auth.updateProfile(profile);
            }
        } catch (Exception e) {
            log.debug("Could not persist theme preference: {}", e.getMessage());
        }
    }

    public static void toggleTheme(Scene scene) {
        setTheme(isDark() ? "LIGHT" : "DARK", scene);
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String cssFile = isDark() ? "/com/tradepulse/css/dark.css" : "/com/tradepulse/css/light.css";
        var url = ThemeManager.class.getResource(cssFile);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        } else {
            log.warn("Stylesheet {} could not be loaded from classpath.", cssFile);
        }
    }
}
