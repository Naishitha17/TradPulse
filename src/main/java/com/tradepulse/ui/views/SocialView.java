package com.tradepulse.ui.views;

import com.tradepulse.model.Conversation;
import com.tradepulse.model.Message;
import com.tradepulse.model.TraderRanking;
import com.tradepulse.model.User;
import com.tradepulse.service.AuthService;
import com.tradepulse.service.ChatService;
import com.tradepulse.service.LeaderboardService;
import com.tradepulse.ui.components.ToastNotification;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class SocialView extends HBox {
    private final AuthService authService;
    private final LeaderboardService leaderboardService;
    private final ChatService chatService;

    // Left Panel Containers
    private boolean showingTraders = true;
    private final Button tradersTabBtn = new Button("Community Traders");
    private final Button convTabBtn = new Button("Private Conversations");
    private final VBox traderListContainer = new VBox(10);
    private final VBox convListContainer = new VBox(8);

    // Right Chat Panel Containers
    private final VBox chatPanel = new VBox(12);
    private final Label activeChatUserLabel = new Label("Select a trader to start chatting");
    private final Label activeChatHandleLabel = new Label("");
    private final VBox messageBubbleContainer = new VBox(10);
    private final ScrollPane messageScrollPane = new ScrollPane(messageBubbleContainer);
    private final TextField messageInput = new TextField();
    private final Button sendBtn = new Button("Send");

    private Conversation currentConversation;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public SocialView(AuthService authService, LeaderboardService leaderboardService, ChatService chatService) {
        this.authService = authService;
        this.leaderboardService = leaderboardService;
        this.chatService = chatService;

        setSpacing(20);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: transparent;");

        buildUI();
        refresh();
    }

    private void buildUI() {
        // LEFT COLUMN (Traders & Conversations)
        VBox leftColumn = new VBox(14);
        leftColumn.getStyleClass().add("panel-card");
        leftColumn.setPrefWidth(380);
        leftColumn.setMinWidth(320);

        HBox tabBox = new HBox(8);
        tradersTabBtn.getStyleClass().add("btn-primary");
        convTabBtn.getStyleClass().add("btn-secondary");
        HBox.setHgrow(tradersTabBtn, Priority.ALWAYS);
        HBox.setHgrow(convTabBtn, Priority.ALWAYS);
        tradersTabBtn.setMaxWidth(Double.MAX_VALUE);
        convTabBtn.setMaxWidth(Double.MAX_VALUE);

        tradersTabBtn.setOnAction(e -> {
            showingTraders = true;
            tradersTabBtn.getStyleClass().setAll("btn-primary");
            convTabBtn.getStyleClass().setAll("btn-secondary");
            traderListContainer.setVisible(true);
            traderListContainer.setManaged(true);
            convListContainer.setVisible(false);
            convListContainer.setManaged(false);
        });

        convTabBtn.setOnAction(e -> {
            showingTraders = false;
            convTabBtn.getStyleClass().setAll("btn-primary");
            tradersTabBtn.getStyleClass().setAll("btn-secondary");
            traderListContainer.setVisible(false);
            traderListContainer.setManaged(false);
            convListContainer.setVisible(true);
            convListContainer.setManaged(true);
            refreshConversations();
        });

        tabBox.getChildren().addAll(tradersTabBtn, convTabBtn);

        ScrollPane listScroll = new ScrollPane();
        listScroll.setFitToWidth(true);
        listScroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        VBox listWrapper = new VBox(10);
        listWrapper.getChildren().addAll(traderListContainer, convListContainer);
        convListContainer.setVisible(false);
        convListContainer.setManaged(false);
        listScroll.setContent(listWrapper);

        leftColumn.getChildren().addAll(tabBox, listScroll);

        // RIGHT COLUMN (Chat Area)
        chatPanel.getStyleClass().add("panel-card");
        HBox.setHgrow(chatPanel, Priority.ALWAYS);

        // Chat Header
        HBox chatHeader = new HBox(12);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setStyle("-fx-padding: 0 0 10 0; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;");

        Label avatarPlaceholder = new Label("💬");
        avatarPlaceholder.setStyle("-fx-font-size: 20px;");

        VBox headerText = new VBox(2);
        activeChatUserLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        activeChatHandleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        headerText.getChildren().addAll(activeChatUserLabel, activeChatHandleLabel);

        chatHeader.getChildren().addAll(avatarPlaceholder, headerText);

        // Message Thread Scroll Area
        messageScrollPane.setFitToWidth(true);
        messageScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(messageScrollPane, Priority.ALWAYS);

        // Message Input Field & Button
        HBox inputRow = new HBox(10);
        inputRow.setAlignment(Pos.CENTER);
        messageInput.setPromptText("Type a private message...");
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        messageInput.setOnAction(e -> handleSendMessage());

        sendBtn.getStyleClass().add("btn-primary");
        sendBtn.setOnAction(e -> handleSendMessage());

        inputRow.getChildren().addAll(messageInput, sendBtn);

        chatPanel.getChildren().addAll(chatHeader, messageScrollPane, inputRow);

        getChildren().addAll(leftColumn, chatPanel);
    }

    public void refresh() {
        refreshTraders();
        refreshConversations();
        if (currentConversation != null) {
            loadMessages(currentConversation.getId());
        }
    }

    private void refreshTraders() {
        traderListContainer.getChildren().clear();
        User currentUser = authService.getCurrentUser();
        List<TraderRanking> traders = leaderboardService.getAllPublicTraders();

        for (TraderRanking t : traders) {
            if (currentUser != null && t.getUserId() == currentUser.getId()) continue; // Don't show self

            VBox card = new VBox(6);
            card.setStyle("-fx-background-color: #151d2d; -fx-padding: 12; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");

            HBox top = new HBox(8);
            top.setAlignment(Pos.CENTER_LEFT);

            Label avatar = new Label("👤");
            avatar.setStyle("-fx-background-color: #1f2937; -fx-padding: 6; -fx-background-radius: 20px;");

            VBox names = new VBox(1);
            Label dName = new Label(t.getDisplayName());
            dName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
            Label handle = new Label("@" + t.getUsername());
            handle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            names.getChildren().addAll(dName, handle);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label retBadge = new Label(String.format("%s%.1f%% Return", t.getReturnPercent() >= 0 ? "+" : "", t.getReturnPercent()));
            retBadge.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 3 8 3 8; -fx-background-radius: 4;",
                    t.getReturnPercent() >= 0 ? "#10b981" : "#ef4444",
                    t.getReturnPercent() >= 0 ? "rgba(16, 185, 129, 0.15)" : "rgba(239, 68, 68, 0.15)"));

            top.getChildren().addAll(avatar, names, sp, retBadge);

            Label bio = new Label(t.getBio());
            bio.setWrapText(true);
            bio.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");

            HBox bottom = new HBox(8);
            bottom.setAlignment(Pos.CENTER_LEFT);

            Label stats = new Label(String.format("Win Rate: %.1f%%  •  Trades: %d", t.getWinRate(), t.getTotalTrades()));
            stats.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

            Region sp2 = new Region();
            HBox.setHgrow(sp2, Priority.ALWAYS);

            Button msgBtn = new Button("Message");
            msgBtn.getStyleClass().add("btn-secondary");
            msgBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 10 3 10;");
            msgBtn.setOnAction(e -> openChatWithTrader(t));

            bottom.getChildren().addAll(stats, sp2, msgBtn);

            card.getChildren().addAll(top, bio, bottom);
            traderListContainer.getChildren().add(card);
        }
    }

    private void refreshConversations() {
        convListContainer.getChildren().clear();
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) return;

        List<Conversation> list = chatService.getUserConversations(currentUser.getId());
        if (list.isEmpty()) {
            Label empty = new Label("No active chats yet. Message a trader from the Community tab!");
            empty.setWrapText(true);
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            convListContainer.getChildren().add(empty);
            return;
        }

        for (Conversation c : list) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 10, 8, 10));
            row.setStyle("-fx-background-color: #151d2d; -fx-background-radius: 6; -fx-cursor: hand;");
            row.setOnMouseClicked(e -> {
                this.currentConversation = c;
                activeChatUserLabel.setText(c.getOtherDisplayName());
                activeChatHandleLabel.setText("@" + c.getOtherUsername());
                loadMessages(c.getId());
            });

            VBox info = new VBox(2);
            Label name = new Label(c.getOtherDisplayName());
            name.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

            String last = c.getLastMessage() != null ? c.getLastMessage() : "No messages yet";
            Label msg = new Label(last);
            msg.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            info.getChildren().addAll(name, msg);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            row.getChildren().addAll(info, sp);
            if (c.getUnreadCount() > 0) {
                Label badge = new Label(String.valueOf(c.getUnreadCount()));
                badge.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: #0b0f19; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 10;");
                row.getChildren().add(badge);
            }

            convListContainer.getChildren().add(row);
        }
    }

    public void openChatWithTrader(TraderRanking trader) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) return;

        Conversation c = chatService.getOrCreateConversation(currentUser.getId(), trader.getUserId());
        this.currentConversation = c;
        activeChatUserLabel.setText(trader.getDisplayName());
        activeChatHandleLabel.setText("@" + trader.getUsername());

        loadMessages(c.getId());
    }

    private void loadMessages(int conversationId) {
        messageBubbleContainer.getChildren().clear();
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) return;

        List<Message> messages = chatService.getMessages(conversationId, currentUser.getId());
        for (Message m : messages) {
            boolean isMine = m.getSenderId() == currentUser.getId();

            HBox bubbleRow = new HBox();
            bubbleRow.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox bubble = new VBox(2);
            bubble.setMaxWidth(420);
            bubble.setPadding(new Insets(8, 12, 8, 12));
            bubble.setStyle(isMine
                    ? "-fx-background-color: #06b6d4; -fx-background-radius: 12 12 2 12;"
                    : "-fx-background-color: #1f2937; -fx-background-radius: 12 12 12 2; -fx-border-color: #2e3b4e; -fx-border-radius: 12 12 12 2;");

            Label content = new Label(m.getContent());
            content.setWrapText(true);
            content.setStyle(isMine
                    ? "-fx-text-fill: #0b0f19; -fx-font-weight: 500; -fx-font-size: 13px;"
                    : "-fx-text-fill: #f8fafc; -fx-font-size: 13px;");

            String timeStr = m.getCreatedAt() != null ? m.getCreatedAt().format(TIME_FMT) : "";
            Label time = new Label(timeStr);
            time.setStyle(isMine ? "-fx-font-size: 9px; -fx-text-fill: rgba(11, 15, 25, 0.7);" : "-fx-font-size: 9px; -fx-text-fill: #64748b;");

            bubble.getChildren().addAll(content, time);
            bubbleRow.getChildren().add(bubble);
            messageBubbleContainer.getChildren().add(bubbleRow);
        }

        // Auto-scroll to bottom of chat
        Platform.runLater(() -> messageScrollPane.setVvalue(1.0));
    }

    private void handleSendMessage() {
        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty() || currentConversation == null) return;

        User currentUser = authService.getCurrentUser();
        if (currentUser == null) return;

        try {
            chatService.sendMessage(currentConversation.getId(), currentUser.getId(), text);
            messageInput.clear();
            loadMessages(currentConversation.getId());
        } catch (Exception e) {
            ToastNotification.show(this, "Message Failed", e.getMessage(), ToastNotification.Type.ERROR);
        }
    }
}
