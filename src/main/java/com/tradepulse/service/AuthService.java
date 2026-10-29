package com.tradepulse.service;

import com.tradepulse.database.DatabaseManager;
import com.tradepulse.model.Role;
import com.tradepulse.model.User;
import com.tradepulse.model.UserProfile;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final DatabaseManager dbManager;
    private User currentUser;
    private UserProfile currentProfile;

    public AuthService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            this.currentProfile = getUserProfile(user.getId());
        } else {
            this.currentProfile = null;
        }
    }

    public synchronized UserProfile getCurrentProfile() {
        if (currentProfile == null && currentUser != null) {
            currentProfile = getUserProfile(currentUser.getId());
        }
        return currentProfile;
    }

    public synchronized void refreshCurrentProfile() {
        if (currentUser != null) {
            this.currentProfile = getUserProfile(currentUser.getId());
        }
    }

    public synchronized void logout() {
        this.currentUser = null;
        this.currentProfile = null;
        log.info("User logged out successfully.");
    }

    public User register(String username, String email, String password, String displayName) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters.");
        }
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = username;
        }

        username = username.trim().toLowerCase();
        email = email.trim().toLowerCase();

        String checkSql = "SELECT id FROM users WHERE username = ? OR email = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, username);
            checkStmt.setString(2, email);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    throw new IllegalArgumentException("Username or email is already registered.");
                }
            }
        } catch (SQLException e) {
            log.error("Database error checking user existence", e);
            throw new RuntimeException("Database error during registration check", e);
        }

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(10));
        String insertUserSql = "INSERT INTO users (username, email, password_hash, display_name, role) VALUES (?, ?, ?, ?, ?)";
        String insertProfileSql = "INSERT INTO profiles (user_id, bio, avatar_seed, virtual_balance, initial_balance, currency, theme) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement uStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                    uStmt.setString(1, username);
                    uStmt.setString(2, email);
                    uStmt.setString(3, hash);
                    uStmt.setString(4, displayName);
                    uStmt.setString(5, Role.USER.name());
                    uStmt.executeUpdate();

                    try (ResultSet keys = uStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            userId = keys.getInt(1);
                        } else {
                            throw new SQLException("Failed to retrieve generated user ID.");
                        }
                    }
                }

                try (PreparedStatement pStmt = conn.prepareStatement(insertProfileSql)) {
                    pStmt.setInt(1, userId);
                    pStmt.setString(2, "Active trader exploring markets on TradePulse Nexus.");
                    pStmt.setString(3, "avatar_" + (1 + (userId % 8)));
                    pStmt.setDouble(4, 100000.00);
                    pStmt.setDouble(5, 100000.00);
                    pStmt.setString(6, "INR");
                    pStmt.setString(7, "DARK");
                    pStmt.executeUpdate();
                }

                // Initial welcome notification
                String notifSql = "INSERT INTO notifications (user_id, title, message, type) VALUES (?, ?, ?, ?)";
                try (PreparedStatement nStmt = conn.prepareStatement(notifSql)) {
                    nStmt.setInt(1, userId);
                    nStmt.setString(2, "Welcome to TradePulse Nexus!");
                    nStmt.setString(3, "Your account has been credited with ₹1,00,000 in virtual funds. Explore the Market Watch to begin paper trading.");
                    nStmt.setString(4, "SYSTEM");
                    nStmt.executeUpdate();
                }

                conn.commit();
                log.info("Registered new user: {}", username);

                User newUser = new User(userId, username, email, hash, displayName, Role.USER, null, true);
                setCurrentUser(newUser);
                return newUser;
            } catch (Exception e) {
                conn.rollback();
                log.error("Failed to complete user registration transaction", e);
                throw new RuntimeException("Registration failed: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Database connection failure during registration", e);
            throw new RuntimeException("Failed to connect to database for registration", e);
        }
    }

    public User login(String identifier, String password) {
        if (identifier == null || identifier.trim().isEmpty() || password == null) {
            throw new IllegalArgumentException("Username/email and password must not be empty.");
        }
        identifier = identifier.trim().toLowerCase();

        String sql = "SELECT id, username, email, password_hash, display_name, role, created_at, is_active FROM users WHERE username = ? OR email = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, identifier);
            stmt.setString(2, identifier);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean active = rs.getInt("is_active") == 1;
                    if (!active) {
                        throw new IllegalStateException("Your account has been deactivated. Please contact an administrator.");
                    }

                    String hash = rs.getString("password_hash");
                    if (BCrypt.checkpw(password, hash)) {
                        Timestamp ts = rs.getTimestamp("created_at");
                        User user = new User(
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("email"),
                                hash,
                                rs.getString("display_name"),
                                Role.valueOf(rs.getString("role")),
                                ts != null ? ts.toLocalDateTime() : null,
                                active
                        );
                        setCurrentUser(user);
                        log.info("User {} logged in successfully.", user.getUsername());
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Database error during login", e);
            throw new RuntimeException("Database error during login", e);
        }

        throw new IllegalArgumentException("Invalid username/email or password.");
    }

    public UserProfile getUserProfile(int userId) {
        String sql = "SELECT * FROM profiles WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserProfile(
                            rs.getInt("user_id"),
                            rs.getString("bio"),
                            rs.getString("avatar_seed"),
                            BigDecimal.valueOf(rs.getDouble("virtual_balance")).setScale(2, RoundingMode.HALF_UP),
                            BigDecimal.valueOf(rs.getDouble("initial_balance")).setScale(2, RoundingMode.HALF_UP),
                            rs.getString("currency"),
                            rs.getString("theme"),
                            rs.getInt("is_public") == 1,
                            rs.getInt("show_trades_on_leaderboard") == 1,
                            rs.getInt("notify_trades") == 1,
                            rs.getInt("notify_price_alerts") == 1,
                            rs.getInt("notify_messages") == 1
                    );
                }
            }
        } catch (SQLException e) {
            log.error("Failed to fetch user profile for user {}", userId, e);
        }
        return null;
    }

    public void updateProfile(UserProfile profile) {
        if (profile == null) return;
        String sql = "UPDATE profiles SET bio = ?, avatar_seed = ?, currency = ?, theme = ?, is_public = ?, " +
                     "show_trades_on_leaderboard = ?, notify_trades = ?, notify_price_alerts = ?, notify_messages = ? " +
                     "WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, profile.getBio());
            stmt.setString(2, profile.getAvatarSeed());
            stmt.setString(3, profile.getCurrency());
            stmt.setString(4, profile.getTheme());
            stmt.setInt(5, profile.isPublic() ? 1 : 0);
            stmt.setInt(6, profile.isShowTradesOnLeaderboard() ? 1 : 0);
            stmt.setInt(7, profile.isNotifyTrades() ? 1 : 0);
            stmt.setInt(8, profile.isNotifyPriceAlerts() ? 1 : 0);
            stmt.setInt(9, profile.isNotifyMessages() ? 1 : 0);
            stmt.setInt(10, profile.getUserId());
            stmt.executeUpdate();

            if (currentUser != null && currentUser.getId() == profile.getUserId()) {
                this.currentProfile = profile;
            }
        } catch (SQLException e) {
            log.error("Failed to update user profile", e);
            throw new RuntimeException("Failed to update profile", e);
        }
    }

    public void updateDisplayName(int userId, String newDisplayName) {
        if (newDisplayName == null || newDisplayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be empty.");
        }
        String sql = "UPDATE users SET display_name = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newDisplayName.trim());
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            if (currentUser != null && currentUser.getId() == userId) {
                currentUser.setDisplayName(newDisplayName.trim());
            }
        } catch (SQLException e) {
            log.error("Failed to update display name", e);
            throw new RuntimeException("Database error updating display name", e);
        }
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        String fetchSql = "SELECT password_hash FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement fetchStmt = conn.prepareStatement(fetchSql)) {
            fetchStmt.setInt(1, userId);
            try (ResultSet rs = fetchStmt.executeQuery()) {
                if (rs.next()) {
                    String existingHash = rs.getString("password_hash");
                    if (!BCrypt.checkpw(oldPassword, existingHash)) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            String updateSql = "UPDATE users SET password_hash = ? WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
                updateStmt.setString(1, newHash);
                updateStmt.setInt(2, userId);
                updateStmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            log.error("Error changing password", e);
            throw new RuntimeException("Database error changing password", e);
        }
    }

    public User getUserById(int userId) {
        String sql = "SELECT id, username, email, password_hash, display_name, role, created_at, is_active FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getString("display_name"),
                            Role.valueOf(rs.getString("role")),
                            ts != null ? ts.toLocalDateTime() : null,
                            rs.getInt("is_active") == 1
                    );
                }
            }
        } catch (SQLException e) {
            log.error("Error retrieving user by id", e);
        }
        return null;
    }
}
