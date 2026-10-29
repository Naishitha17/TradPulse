package com.tradepulse.service;

import com.tradepulse.database.DatabaseManager;
import com.tradepulse.model.Conversation;
import com.tradepulse.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final DatabaseManager dbManager;

    public ChatService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<Conversation> getUserConversations(int userId) {
        List<Conversation> list = new ArrayList<>();
        String sql = "SELECT c.id, c.user1_id, c.user2_id, c.updated_at, " +
                     "u.username, u.display_name, p.avatar_seed, " +
                     "(SELECT content FROM messages WHERE conversation_id = c.id ORDER BY created_at DESC LIMIT 1) AS last_msg, " +
                     "(SELECT COUNT(*) FROM messages WHERE conversation_id = c.id AND sender_id != ? AND is_read = 0) AS unread_cnt " +
                     "FROM conversations c " +
                     "JOIN users u ON u.id = CASE WHEN c.user1_id = ? THEN c.user2_id ELSE c.user1_id END " +
                     "LEFT JOIN profiles p ON p.user_id = u.id " +
                     "WHERE c.user1_id = ? OR c.user2_id = ? " +
                     "ORDER BY c.updated_at DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("updated_at");
                    Conversation conv = new Conversation(
                            rs.getInt("id"),
                            rs.getInt("user1_id"),
                            rs.getInt("user2_id"),
                            ts != null ? ts.toLocalDateTime() : null
                    );
                    conv.setOtherUsername(rs.getString("username"));
                    conv.setOtherDisplayName(rs.getString("display_name"));
                    conv.setOtherAvatarSeed(rs.getString("avatar_seed"));
                    conv.setLastMessage(rs.getString("last_msg"));
                    conv.setUnreadCount(rs.getInt("unread_cnt"));
                    list.add(conv);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query user conversations for user {}", userId, e);
        }
        return list;
    }

    public Conversation getOrCreateConversation(int user1Id, int user2Id) {
        if (user1Id == user2Id) {
            throw new IllegalArgumentException("Cannot create conversation with yourself.");
        }

        int lowId = Math.min(user1Id, user2Id);
        int highId = Math.max(user1Id, user2Id);

        String selectSql = "SELECT id, user1_id, user2_id, updated_at FROM conversations WHERE user1_id = ? AND user2_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setInt(1, lowId);
            stmt.setInt(2, highId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("updated_at");
                    return new Conversation(rs.getInt("id"), lowId, highId, ts != null ? ts.toLocalDateTime() : null);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to check existing conversation", e);
        }

        String insertSql = "INSERT INTO conversations (user1_id, user2_id, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, lowId);
            stmt.setInt(2, highId);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Conversation(keys.getInt(1), lowId, highId, LocalDateTime.now());
                }
            }
        } catch (SQLException e) {
            log.error("Failed to create conversation", e);
        }
        return null;
    }

    public List<Message> getMessages(int conversationId, int requestingUserId) {
        // Enforce participant authorization
        verifyConversationAccess(conversationId, requestingUserId);

        List<Message> list = new ArrayList<>();
        String sql = "SELECT m.id, m.conversation_id, m.sender_id, u.display_name, m.content, m.is_read, m.created_at " +
                     "FROM messages m " +
                     "JOIN users u ON u.id = m.sender_id " +
                     "WHERE m.conversation_id = ? ORDER BY m.created_at ASC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    list.add(new Message(
                            rs.getInt("id"),
                            rs.getInt("conversation_id"),
                            rs.getInt("sender_id"),
                            rs.getString("display_name"),
                            rs.getString("content"),
                            rs.getInt("is_read") == 1,
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to fetch messages for conversation {}", conversationId, e);
        }

        // Mark unread messages as read
        markConversationRead(conversationId, requestingUserId);

        return list;
    }

    public Message sendMessage(int conversationId, int senderId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        verifyConversationAccess(conversationId, senderId);

        String msgSql = "INSERT INTO messages (conversation_id, sender_id, content) VALUES (?, ?, ?)";
        String updateConvSql = "UPDATE conversations SET updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int messageId;
                try (PreparedStatement stmt = conn.prepareStatement(msgSql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, conversationId);
                    stmt.setInt(2, senderId);
                    stmt.setString(3, content.trim());
                    stmt.executeUpdate();

                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            messageId = keys.getInt(1);
                        } else {
                            throw new SQLException("Failed to retrieve message ID.");
                        }
                    }
                }

                try (PreparedStatement uStmt = conn.prepareStatement(updateConvSql)) {
                    uStmt.setInt(1, conversationId);
                    uStmt.executeUpdate();
                }

                conn.commit();
                return new Message(messageId, conversationId, senderId, "You", content.trim(), false, LocalDateTime.now());
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to send message", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error sending message", e);
        }
    }

    public void markConversationRead(int conversationId, int currentUserId) {
        String sql = "UPDATE messages SET is_read = 1 WHERE conversation_id = ? AND sender_id != ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            stmt.setInt(2, currentUserId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to mark conversation messages read", e);
        }
    }

    private void verifyConversationAccess(int conversationId, int userId) {
        String sql = "SELECT 1 FROM conversations WHERE id = ? AND (user1_id = ? OR user2_id = ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SecurityException("Unauthorized: You do not have permission to access conversation #" + conversationId);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error checking authorization", e);
        }
    }
}
