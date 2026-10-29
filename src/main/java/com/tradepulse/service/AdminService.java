package com.tradepulse.service;

import com.tradepulse.database.DatabaseManager;
import com.tradepulse.model.FeedbackCategory;
import com.tradepulse.model.FeedbackItem;
import com.tradepulse.model.FeedbackStatus;
import com.tradepulse.model.Role;
import com.tradepulse.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminService {
    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private final DatabaseManager dbManager;

    public AdminService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void submitFeedback(int userId, int rating, FeedbackCategory category, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars.");
        }
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment cannot be empty.");
        }

        String sql = "INSERT INTO feedback (user_id, rating, category, comment, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, rating);
            stmt.setString(3, category.name());
            stmt.setString(4, comment.trim());
            stmt.setString(5, FeedbackStatus.PENDING.name());
            stmt.executeUpdate();
            log.info("Feedback submitted by user {}: rating {}", userId, rating);
        } catch (SQLException e) {
            log.error("Failed to submit feedback", e);
            throw new RuntimeException("Database error saving feedback", e);
        }
    }

    public List<FeedbackItem> getAllFeedback(Role userRole) {
        verifyAdmin(userRole);
        List<FeedbackItem> list = new ArrayList<>();
        String sql = "SELECT f.id, f.user_id, u.username, f.rating, f.category, f.comment, f.status, f.created_at " +
                     "FROM feedback f " +
                     "JOIN users u ON u.id = f.user_id " +
                     "ORDER BY f.created_at DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("created_at");
                list.add(new FeedbackItem(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getInt("rating"),
                        FeedbackCategory.valueOf(rs.getString("category")),
                        rs.getString("comment"),
                        FeedbackStatus.valueOf(rs.getString("status")),
                        ts != null ? ts.toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            log.error("Failed to retrieve feedback items", e);
        }
        return list;
    }

    public void updateFeedbackStatus(int feedbackId, FeedbackStatus status, Role userRole) {
        verifyAdmin(userRole);
        String sql = "UPDATE feedback SET status = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, feedbackId);
            stmt.executeUpdate();
            log.info("Updated feedback #{} status to {}", feedbackId, status);
        } catch (SQLException e) {
            log.error("Failed to update feedback status", e);
            throw new RuntimeException("Database error updating feedback", e);
        }
    }

    public Map<String, Object> getFeedbackMetrics(Role userRole) {
        verifyAdmin(userRole);
        Map<String, Object> metrics = new HashMap<>();
        String sql = "SELECT COUNT(*) AS total_count, AVG(rating) AS avg_rating FROM feedback";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                metrics.put("totalCount", rs.getInt("total_count"));
                metrics.put("avgRating", Math.round(rs.getDouble("avg_rating") * 10.0) / 10.0);
            }
        } catch (SQLException e) {
            log.error("Failed to calculate feedback metrics", e);
        }

        // Ratings breakdown 1 to 5
        Map<Integer, Integer> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0);

        String distSql = "SELECT rating, COUNT(*) AS cnt FROM feedback GROUP BY rating";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(distSql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                distribution.put(rs.getInt("rating"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            log.error("Failed to calculate rating breakdown", e);
        }
        metrics.put("distribution", distribution);

        return metrics;
    }

    public List<User> getAllUsers(Role userRole) {
        verifyAdmin(userRole);
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, email, password_hash, display_name, role, created_at, is_active FROM users ORDER BY id ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("created_at");
                list.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        "", // Hide password hash for safety
                        rs.getString("display_name"),
                        Role.valueOf(rs.getString("role")),
                        ts != null ? ts.toLocalDateTime() : null,
                        rs.getInt("is_active") == 1
                ));
            }
        } catch (SQLException e) {
            log.error("Failed to fetch all users", e);
        }
        return list;
    }

    public void toggleUserActive(int targetUserId, boolean active, Role userRole) {
        verifyAdmin(userRole);
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, active ? 1 : 0);
            stmt.setInt(2, targetUserId);
            stmt.executeUpdate();
            log.info("Admin updated user #{} active state to {}", targetUserId, active);
        } catch (SQLException e) {
            log.error("Failed to toggle user active status", e);
            throw new RuntimeException("Database error toggling user status", e);
        }
    }

    private void verifyAdmin(Role role) {
        if (role != Role.ADMIN) {
            throw new SecurityException("Access Denied: Administrative privileges required.");
        }
    }
}
