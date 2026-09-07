package com.benhsoan.domain;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.auth.User;

@DisplayName("User Domain Tests")
class UserTest {

    private static final UUID ROLE_ID = UUID.randomUUID();

    private User createDefaultUser() {
        return User.create(
                "testuser",
                "passwordHash123",
                "Test User",
                "test@example.com",
                "0123456789",
                ROLE_ID
        );
    }

    @Test
    @DisplayName("New user should be active by default")
    void newUserShouldBeActive() {
        User user = createDefaultUser();
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("shouldDeactivateUser")
    void shouldDeactivateUser() {
        User user = createDefaultUser();
        user.deactivate();
        assertFalse(user.isActive());
    }

    @Test
    @DisplayName("Deactivated user should become active after activate()")
    void shouldActivateUser() {
        User user = createDefaultUser();
        user.deactivate();
        assertFalse(user.isActive());

        user.activate();
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("activate() on already active user should remain active")
    void activateOnActiveUser() {
        User user = createDefaultUser();
        assertTrue(user.isActive());

        user.activate();
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("deactivate() on already inactive user should remain inactive")
    void deactivateOnInactiveUser() {
        User user = createDefaultUser();
        user.deactivate();
        assertFalse(user.isActive());

        user.deactivate();
        assertFalse(user.isActive());
    }

    @Test
    @DisplayName("changePassword should update password hash and clear mustChangePassword")
    void changePassword() {
        User user = createDefaultUser();
        user.resetPassword("tempHash123");
        assertTrue(user.isMustChangePassword());

        user.changePassword("newHash456");
        assertFalse(user.isMustChangePassword());
    }

    @Test
    @DisplayName("resetPassword should set mustChangePassword to true")
    void resetPassword() {
        User user = createDefaultUser();
        assertFalse(user.isMustChangePassword());

        user.resetPassword("temporaryHash789");
        assertTrue(user.isMustChangePassword());
    }

    @Test
    @DisplayName("updateProfile should update fullName, email and phone")
    void updateProfile() {
        User user = createDefaultUser();
        user.updateProfile("Updated Name", "updated@example.com", "0987654321");

        assertEquals("Updated Name", user.getFullName());
        assertEquals("updated@example.com", user.getEmail());
        assertEquals("0987654321", user.getPhone());
    }

    @Test
    @DisplayName("updateLastLogin should set lastLoginAt")
    void updateLastLogin() {
        User user = createDefaultUser();
        assertNull(user.getLastLoginAt());

        Instant loginTime = Instant.now();
        user.updateLastLogin(loginTime);

        assertNotNull(user.getLastLoginAt());
        assertEquals(loginTime, user.getLastLoginAt());
    }

    @Test
    @DisplayName("Restored user should have correct state")
    void restoredUserHasCorrectState() {
        UUID id = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant lastLogin = Instant.now().minusSeconds(86400);

        User user = User.restore(
                id, "restored", "hash", "Restored User",
                "restored@test.com", "0111111111",
                roleId, false, lastLogin, now
        );

        assertEquals(id, user.getId());
        assertEquals("restored", user.getUsername());
        assertEquals("Restored User", user.getFullName());
        assertFalse(user.isActive());
        assertEquals(lastLogin, user.getLastLoginAt());

        // Verify can activate restored user
        user.activate();
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("User create should generate random UUID id")
    void createGeneratesRandomId() {
        User user1 = createDefaultUser();
        User user2 = createDefaultUser();
        assertNotNull(user1.getId());
        assertNotNull(user2.getId());
        assertFalse(user1.getId().equals(user2.getId()));
    }
}
