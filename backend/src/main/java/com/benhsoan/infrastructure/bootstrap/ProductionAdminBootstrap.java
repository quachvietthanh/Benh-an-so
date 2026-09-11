package com.benhsoan.infrastructure.bootstrap;

import java.security.SecureRandom;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Production Security Bootstrap for Initial Administrator Account (OWASP A07:2021).
 * Ensures no default/known credential (e.g. Password123@) exists in production.
 * Supports credentials supplied via environment variables (INITIAL_ADMIN_PASSWORD)
 * or auto-generates a one-time cryptographically secure random temporary password.
 * Concurrency-safe across multi-pod clusters via pessimistic row locking (findByIdForUpdate).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ProductionAdminBootstrap implements ApplicationRunner {

    private static final String UNSET_TOKEN = "BOOTSTRAP.ADMIN.PASSWORD.NOT.SET";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
    private static final int GENERATED_PASSWORD_LENGTH = 24;

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final EntityManager entityManager;

    @Value("${app.seed.demo:false}")
    private boolean seedDemo;

    @Value("${app.initial-admin.password:}")
    private String initialAdminPassword;

    @Value("${INITIAL_ADMIN_PASSWORD:}")
    private String envInitialAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (seedDemo) {
            log.info("[BOOTSTRAP] Demo seed is enabled (app.seed.demo=true). Skipping ProductionAdminBootstrap.");
            return;
        }

        Optional<User> adminLookup = userRepository.findByUsername("admin");
        if (adminLookup.isEmpty()) {
            log.warn("[BOOTSTRAP] Admin user not found in database. Skipping admin bootstrap.");
            return;
        }

        // Fast check before acquiring lock
        if (!isAdminPasswordUnset(adminLookup.get().getPasswordHash())) {
            log.info("[BOOTSTRAP] Admin account already provisioned with secure credentials.");
            return;
        }

        // Clear JPA L1 cache to ensure fresh database state is loaded from MySQL after acquiring the lock
        entityManager.clear();

        // Pessimistic lock via findByIdForUpdate serializes concurrent multi-pod startups
        Optional<User> lockedAdminOpt = userRepository.findByIdForUpdate(adminLookup.get().getId());
        if (lockedAdminOpt.isEmpty()) {
            log.warn("[BOOTSTRAP] Admin user could not be locked. Skipping admin bootstrap.");
            return;
        }

        User admin = lockedAdminOpt.get();

        // Double-check inside lock to ensure another cluster node hasn't already provisioned
        if (!isAdminPasswordUnset(admin.getPasswordHash())) {
            log.info("[BOOTSTRAP] Admin account was already provisioned by another cluster node.");
            return;
        }

        String resolvedPassword = resolveInitialPassword();
        boolean generated = false;

        if (resolvedPassword == null || resolvedPassword.isBlank()) {
            resolvedPassword = generateSecureRandomPassword();
            generated = true;
        }

        String encodedHash = passwordEncoderPort.encode(resolvedPassword);
        admin.resetPassword(encodedHash);
        admin.activate();
        userRepository.save(admin);

        if (generated) {
            log.warn("""
                    \n================================================================================
                    [SECURITY ALERT - INITIAL PRODUCTION ADMIN BOOTSTRAP]
                    No INITIAL_ADMIN_PASSWORD was configured in environment.
                    A cryptographically secure temporary password was generated and set for administrator:
                    Username: admin
                    Password: [REDACTED FOR SECURITY - OWASP CWE-532. CONFIGURE INITIAL_ADMIN_PASSWORD IN SECRET VAULT/ENV]
                    IMPORTANT: Provide INITIAL_ADMIN_PASSWORD via secure environment variables in production!
                    ================================================================================\
                    """);
        } else {
            log.info("[SECURITY] Production admin account initialized with configured INITIAL_ADMIN_PASSWORD.");
        }
    }

    private boolean isAdminPasswordUnset(String passwordHash) {
        return passwordHash != null && passwordHash.contains(UNSET_TOKEN);
    }

    private String resolveInitialPassword() {
        if (envInitialAdminPassword != null && !envInitialAdminPassword.isBlank()) {
            return envInitialAdminPassword.trim();
        }
        if (initialAdminPassword != null && !initialAdminPassword.isBlank()) {
            return initialAdminPassword.trim();
        }
        return null;
    }

    private String generateSecureRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
}
