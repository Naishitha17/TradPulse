package com.tradepulse.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ToastNotification {
    public enum Type {
        SUCCESS("#10b981", "✓"),
        ERROR("#ef4444", "✕"),
        INFO("#06b6d4", "ℹ"),
        WARNING("#f59e0b", "⚠");

        final String color;
        final String icon;

        Type(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    public static void show(Node anchorNode, String title, String message, Type type) {
        if (anchorNode == null) return;

        Platform.runLater(() -> {
            Pane targetPane = null;
            if (anchorNode.getScene() != null && anchorNode.getScene().getRoot() instanceof Pane) {
                targetPane = (Pane) anchorNode.getScene().getRoot();
            } else if (anchorNode instanceof Pane) {
                targetPane = (Pane) anchorNode;
            } else if (anchorNode.getParent() instanceof Pane) {
                targetPane = (Pane) anchorNode.getParent();
            }

            if (targetPane == null) return;

            final Pane host = targetPane;
            VBox toast = new VBox(4);
            toast.setStyle(String.format("-fx-background-color: #111827; -fx-border-color: %s; -fx-border-width: 1 1 1 4; " +
                    "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-padding: 12 16 12 16; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);", type.color));
            toast.setMaxWidth(340);
            toast.setMouseTransparent(true);

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);

            Label iconLbl = new Label(type.icon);
            iconLbl.setStyle(String.format("-fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 14px;", type.color));

            Label titleLbl = new Label(title);
            titleLbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 13px;");

            header.getChildren().addAll(iconLbl, titleLbl);

            Label msgLbl = new Label(message);
            msgLbl.setWrapText(true);
            msgLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

            toast.getChildren().addAll(header, msgLbl);

            if (host instanceof StackPane) {
                StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
                StackPane.setMargin(toast, new Insets(0, 24, 24, 0));
            } else {
                toast.layoutXProperty().bind(host.widthProperty().subtract(toast.widthProperty()).subtract(24));
                toast.layoutYProperty().bind(host.heightProperty().subtract(toast.heightProperty()).subtract(24));
            }

            host.getChildren().add(toast);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toast);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            PauseTransition pause = new PauseTransition(Duration.seconds(3.5));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> host.getChildren().remove(toast));

            SequentialTransition seq = new SequentialTransition(fadeIn, pause, fadeOut);
            seq.play();
        });
    }
}
