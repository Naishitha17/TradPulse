package com.tradepulse;

import com.tradepulse.database.DatabaseManager;
import com.tradepulse.model.Role;
import com.tradepulse.model.User;
import com.tradepulse.model.UserProfile;
import com.tradepulse.service.AuthService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {
    private static DatabaseManager dbManager;
    private static AuthService authService;

    @BeforeAll
    public static void setUp() {
        dbManager = TestDbHelper.createTestDatabase();
        authService = new AuthService(dbManager);
    }

    @AfterAll
    public static void tearDown() {
        if (dbManager != null) {
            dbManager.shutdown();
        }
    }

    @Test
    public void testUserRegistrationAndLoginSuccess() {
        String username = "tester_" + System.currentTimeMillis();
        String email = username + "@test.com";
        String password = "StrongPassword@123";
        String displayName = "Test Trader";

        User registered = authService.register(username, email, password, displayName);
        assertNotNull(registered);
        assertTrue(registered.getId() > 0);
        assertEquals(username, registered.getUsername());
        assertEquals(Role.USER, registered.getRole());

        // Test login with username
        User loggedIn = authService.login(username, password);
        assertNotNull(loggedIn);
        assertEquals(registered.getId(), loggedIn.getId());

        // Test login with email
        User loggedInEmail = authService.login(email, password);
        assertNotNull(loggedInEmail);
        assertEquals(registered.getId(), loggedInEmail.getId());

        // Verify initial profile
        UserProfile profile = authService.getUserProfile(registered.getId());
        assertNotNull(profile);
        assertEquals(new BigDecimal("100000.00"), profile.getVirtualBalance());
    }

    @Test
    public void testDuplicateRegistrationThrowsException() {
        String username = "dupuser_" + System.currentTimeMillis();
        String email = username + "@dup.com";
        authService.register(username, email, "Pass@123", "Dup User");

        // Duplicate username
        assertThrows(IllegalArgumentException.class, () -> {
            authService.register(username, "other@email.com", "Pass@123", "Other");
        });

        // Duplicate email
        assertThrows(IllegalArgumentException.class, () -> {
            authService.register("othername", email, "Pass@123", "Other");
        });
    }

    @Test
    public void testLoginWithInvalidPassword() {
        String username = "badpass_" + System.currentTimeMillis();
        authService.register(username, username + "@test.com", "ValidPass123", "Bad Pass User");

        assertThrows(IllegalArgumentException.class, () -> {
            authService.login(username, "WrongPassword");
        });
    }

    @Test
    public void testChangePassword() {
        String username = "changepw_" + System.currentTimeMillis();
        User u = authService.register(username, username + "@test.com", "OldPassword123", "User PW");

        boolean changed = authService.changePassword(u.getId(), "OldPassword123", "NewPassword456");
        assertTrue(changed);

        // Login with new password works
        User u2 = authService.login(username, "NewPassword456");
        assertNotNull(u2);

        // Login with old password fails
        assertThrows(IllegalArgumentException.class, () -> {
            authService.login(username, "OldPassword123");
        });
    }
}
