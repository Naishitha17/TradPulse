package com.tradepulse;

import com.tradepulse.service.ServiceRegistry;
import com.tradepulse.ui.MainLayout;
import com.tradepulse.util.ThemeManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);
    private ServiceRegistry services;

    @Override
    public void init() throws Exception {
        log.info("Initializing TradePulse Nexus core services...");
        this.services = ServiceRegistry.getInstance();
    }

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting TradePulse Nexus JavaFX UI...");
        MainLayout mainLayout = new MainLayout(services);
        Scene scene = new Scene(mainLayout, 1320, 820);

        ThemeManager.applyTheme(scene);

        primaryStage.setTitle("TradePulse Nexus — Advanced Desktop Trading & Market Simulation Terminal");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(e -> {
            log.info("Application closing. Shutting down background services...");
            if (services != null) {
                services.shutdown();
            }
        });

        primaryStage.show();
        log.info("TradePulse Nexus UI presented successfully.");
    }

    @Override
    public void stop() throws Exception {
        if (services != null) {
            services.shutdown();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
