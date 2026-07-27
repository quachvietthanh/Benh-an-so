package com.benhsoan.persistence.adapterRepository;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.benhsoan.infrastructure.authSecurity.CurrentUserPrincipal;

import com.benhsoan.application.ucservice.user.ActivateUserService;
import com.benhsoan.application.ucservice.user.DeactivateUserService;
import com.benhsoan.domain.auth.constant.RoleConstants;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.outbound.repository.crudRepository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;

import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
@DisplayName("User Repository & Service Integration Tests")
class UserRepositoryAdapterTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ActivateUserService activateUserService;

    @Autowired
    private DeactivateUserService deactivateUserService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

        user = User.create(
                "test_admin",
                "123456",
                "Administrator",
                "test_admin@gmail.com",
                "0123456789",
                adminRole.getId()
        );

        user = userRepository.save(user);
        userId = user.getId();

        // Authenticate as the created admin user so that
        // DeactivateUserService / ActivateUserService
        // can read the current user via CurrentUserPort.
        var principal = new CurrentUserPrincipal(userId, user.getUsername());
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should save and find user by username")
    void shouldSaveAndFindUserByUsername() {

        Optional<User> result = userRepository.findByUsername("test_admin");

        assertTrue(result.isPresent());
        assertEquals("test_admin", result.get().getUsername());
    }

    @Test
    @DisplayName("Should find user by id")
    void shouldFindUserById() {

        Optional<User> result = userRepository.findById(user.getId());

        assertTrue(result.isPresent());
        assertEquals(user.getId(), result.get().getId());
    }

    @Test
    @DisplayName("Should exists username")
    void shouldExistsUsername() {

        assertTrue(userRepository.existsByUsername("test_admin"));
    }

    @Test
    @DisplayName("Should exists email")
    void shouldExistsEmail() {

        assertTrue(userRepository.existsByEmail("test_admin@gmail.com"));
    }

    @Test
    @DisplayName("Should find only active doctors by role id")
    void shouldFindOnlyActiveDoctorsByRoleId() {

        User activeDoctor = userRepository.save(User.create(
                "doctor_active",
                "123456",
                "Doctor Active",
                "doctor_active@gmail.com",
                "0900000001",
                RoleConstants.DOCTOR
        ));

        User inactiveDoctor = User.create(
                "doctor_inactive",
                "123456",
                "Doctor Inactive",
                "doctor_inactive@gmail.com",
                "0900000002",
                RoleConstants.DOCTOR
        );
        inactiveDoctor.deactivate();
        userRepository.save(inactiveDoctor);

        var doctors = userRepository.findAllActiveByRoleId(RoleConstants.DOCTOR);

        assertTrue(doctors.stream().anyMatch(user -> user.getId().equals(activeDoctor.getId())));
        assertTrue(doctors.stream().noneMatch(user -> user.getUsername().equals("doctor_inactive")));
        assertTrue(doctors.stream().allMatch(User::isActive));
    }

    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {

        userRepository.deleteById(user.getId());

        assertFalse(
                userRepository.findById(user.getId()).isPresent()
        );
    }

    // ============================
    // Account Lock/Unlock Tests
    // ============================

    @Test
    @DisplayName("DeactivateUserService should deactivate an active user")
    void deactivateUserServiceShouldLockAccount() {
        UserResult result = deactivateUserService.deactivate(userId);

        assertNotNull(result);
        assertEquals("test_admin", result.username());

        // Verify in DB
        User deactivated = userRepository.findById(userId).orElseThrow();
        assertFalse(deactivated.isActive());
    }

    @Test
    @DisplayName("Deactivated user cannot be active")
    void deactivatedUserIsNotActive() {
        deactivateUserService.deactivate(userId);

        User deactivated = userRepository.findById(userId).orElseThrow();
        assertFalse(deactivated.isActive());
    }

    @Test
    @DisplayName("ActivateUserService should activate a deactivated user")
    void activateUserServiceShouldUnlockAccount() {
        // First deactivate
        deactivateUserService.deactivate(userId);
        assertFalse(
                userRepository.findById(userId).orElseThrow().isActive()
        );

        // Then activate
        UserResult result = activateUserService.activate(userId);

        assertNotNull(result);
        assertEquals("test_admin", result.username());

        // Verify in DB
        User activated = userRepository.findById(userId).orElseThrow();
        assertTrue(activated.isActive());
    }

    @Test
    @DisplayName("ActivateUserService should throw UserNotFoundException for non-existent user")
    void activateNonExistentUserThrowsException() {
        UUID fakeId = UUID.randomUUID();

        assertThrows(UserNotFoundException.class, () -> {
            activateUserService.activate(fakeId);
        });
    }

    @Test
    @DisplayName("DeactivateUserService should throw UserNotFoundException for non-existent user")
    void deactivateNonExistentUserThrowsException() {
        UUID fakeId = UUID.randomUUID();

        assertThrows(UserNotFoundException.class, () -> {
            deactivateUserService.deactivate(fakeId);
        });
    }

    @Test
    @DisplayName("Activate then deactivate cycle should work correctly")
    void activateDeactivateCycle() {
        // Start: active (default)
        assertTrue(userRepository.findById(userId).orElseThrow().isActive());

        // Deactivate
        deactivateUserService.deactivate(userId);
        assertFalse(userRepository.findById(userId).orElseThrow().isActive());

        // Reactivate
        activateUserService.activate(userId);
        assertTrue(userRepository.findById(userId).orElseThrow().isActive());

        // Deactivate again
        deactivateUserService.deactivate(userId);
        assertFalse(userRepository.findById(userId).orElseThrow().isActive());
    }
}
