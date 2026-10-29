package com.tradepulse;

import com.tradepulse.database.DatabaseManager;
import com.tradepulse.model.FeedbackCategory;
import com.tradepulse.model.FeedbackItem;
import com.tradepulse.model.FeedbackStatus;
import com.tradepulse.model.Role;
import com.tradepulse.model.User;
import com.tradepulse.service.AdminService;
import com.tradepulse.service.AuthService;
import com.tradepulse.service.ChatService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AdminAndSecurityTest {
    private static DatabaseManager dbManager;
    private static AuthService authService;
    private static AdminService adminService;
    private static ChatService chatService;

    @BeforeAll
    public static void setUp() {
        dbManager = TestDbHelper.createTestDatabase();
        authService = new AuthService(dbManager);
        adminService = new AdminService(dbManager);
        chatService = new ChatService(dbManager);
    }

    @AfterAll
    public static void tearDown() {
        if (dbManager != null) {
            dbManager.shutdown();
        }
    }

    @Test
    public void testNonAdminCannotAccessAdminFunctions() {
        // Regular user role
        Role userRole = Role.USER;

        assertThrows(SecurityException.class, () -> {
            adminService.getAllFeedback(userRole);
        });

        assertThrows(SecurityException.class, () -> {
            adminService.updateFeedbackStatus(1, FeedbackStatus.REVIEWED, userRole);
        });

        assertThrows(SecurityException.class, () -> {
            adminService.getAllUsers(userRole);
        });
    }

    @Test
    public void testAdminCanReviewFeedback() {
        Role adminRole = Role.ADMIN;
        String username = "fdbk_user_" + System.currentTimeMillis();
        User user = authService.register(username, username + "@test.com", "Pass@123", "Feedback User");

        // User submits feedback
        adminService.submitFeedback(user.getId(), 5, FeedbackCategory.SUGGESTION, "Great desktop platform!");

        // Admin retrieves feedback
        List<FeedbackItem> feedbackList = adminService.getAllFeedback(adminRole);
        assertFalse(feedbackList.isEmpty());

        FeedbackItem latest = feedbackList.get(0);
        adminService.updateFeedbackStatus(latest.getId(), FeedbackStatus.REVIEWED, adminRole);

        List<FeedbackItem> updatedList = adminService.getAllFeedback(adminRole);
        assertEquals(FeedbackStatus.REVIEWED, updatedList.get(0).getStatus());
    }

    @Test
    public void testPrivateChatUnauthorizedAccessBlocked() {
        User alice = authService.register("alice_" + System.currentTimeMillis(), "alice@test.com", "Pass@123", "Alice");
        User bob = authService.register("bob_" + System.currentTimeMillis(), "bob@test.com", "Pass@123", "Bob");
        User eve = authService.register("eve_" + System.currentTimeMillis(), "eve@test.com", "Pass@123", "Eve");

        var conversation = chatService.getOrCreateConversation(alice.getId(), bob.getId());
        assertNotNull(conversation);

        // Alice sends message to Bob
        chatService.sendMessage(conversation.getId(), alice.getId(), "Hey Bob, check out NVDA today!");

        // Eve tries to view the messages between Alice and Bob
        assertThrows(SecurityException.class, () -> {
            chatService.getMessages(conversation.getId(), eve.getId());
        });

        // Eve tries to inject a message into Alice and Bob's chat
        assertThrows(SecurityException.class, () -> {
            chatService.sendMessage(conversation.getId(), eve.getId(), "Intrusion attempt");
        });

        // Authorized Bob views the messages
        var messages = chatService.getMessages(conversation.getId(), bob.getId());
        assertEquals(1, messages.size());
        assertEquals("Hey Bob, check out NVDA today!", messages.get(0).getContent());
    }
}
