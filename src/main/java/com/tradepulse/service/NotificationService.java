package com.tradepulse.service;

import com.tradepulse.database.DatabaseManager;
import com.tradepulse.model.Notification;
import com.tradepulse.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final DatabaseManager dbManager;
    private final List<Consumer<Notification>> notificationListeners = new ArrayList<>();

    public NotificationService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void addNotificationListener(Consumer<Notification> listener) {
        this.notificationListeners.add(listener);
    }

    public Notification createNotification(int userId, String title, String message, String type) {
        String sql = "INSERT INTO notifications (user_id, title, message, type) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setString(2, title);
            stmt.setString(3, message);
            stmt.setString(4, type);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    Notification notif = new Notification(id, userId, title, message, NotificationType.valueOf(type), false, null);
                    for (Consumer<Notification> l : notificationListeners) {
                        try {
                            l.accept(notif);
                        } catch (Exception ex) {
                            log.debug("Listener error: {}", ex.getMessage());
                        }
                    }
                    return notif;
                }
            }
        } catch (SQLException e) {
            log.error("Failed to insert notification", e);
        }
        return null;
    }

    public List<Notification> getUserNotifications(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT id, user_id, title, message, type, is_read, created_at FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    list.add(new Notification(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("title"),
                            rs.getString("message"),
                            NotificationType.valueOf(rs.getString("type")),
                            rs.getInt("is_read") == 1,
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query notifications for user {}", userId, e);
        }
        return list;
    }

    public int getUnreadCount(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get unread notification count", e);
        }
        return 0;
    }

    public void markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, notificationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to mark notification as read", e);
        }
    }

    public void markAllAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to mark all notifications as read", e);
        }
    }
}
