package com.benhsoan.persistence.adapterRepository.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.benhsoan.domain.auth.PatientPasswordRecoveryToken;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.outbound.repository.auth.PatientPasswordRecoveryTokenRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import jakarta.transaction.Transactional;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:recovery_testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.defer-datasource-initialization=true"
})
@Transactional
@ActiveProfiles("test")
@DisplayName("Patient Password Recovery Token Repository Integration Tests")
class PatientPasswordRecoveryTokenRepositoryAdapterTest {

    @Autowired
    private PatientPasswordRecoveryTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User patientUser;
    private final String phone = "0901234567";

    @BeforeEach
    void setUp() {
        Role patientRole = roleRepository.findByName("PATIENT")
                .orElseGet(() -> roleRepository.save(Role.create("PATIENT", "Patient role", true, Set.of())));

        patientUser = userRepository.save(User.create(
                phone,
                "hashed_pass",
                "Patient Test",
                "patient@test.com",
                phone,
                patientRole.getId()
        ));
    }

    @Test
    @DisplayName("Should save and find latest recovery token by phone")
    void shouldSaveAndFindLatestToken() {
        Instant now = Instant.now();
        PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.create(
                patientUser.getId(),
                phone,
                "hashed_code_1",
                now.plusSeconds(300),
                now
        );

        PatientPasswordRecoveryToken saved = tokenRepository.save(token);
        assertNotNull(saved.getId());

        Optional<PatientPasswordRecoveryToken> found = tokenRepository.findLatestByPhone(phone);
        assertTrue(found.isPresent());
        assertEquals(phone, found.get().getPhone());
        assertEquals("hashed_code_1", found.get().getCodeHash());
        assertFalse(found.get().isUsed());
        assertFalse(found.get().isExpired(now));
    }

    @Test
    @DisplayName("Should find active token and ignore used ones")
    void shouldFindActiveTokenAndIgnoreUsed() {
        Instant now = Instant.now();

        // Older used token
        PatientPasswordRecoveryToken oldToken = PatientPasswordRecoveryToken.create(
                patientUser.getId(),
                phone,
                "old_code_hash",
                now.plusSeconds(300),
                now.minusSeconds(120)
        );
        oldToken.markUsed(now.minusSeconds(60));
        tokenRepository.save(oldToken);

        // Newer active token
        PatientPasswordRecoveryToken activeToken = PatientPasswordRecoveryToken.create(
                patientUser.getId(),
                phone,
                "active_code_hash",
                now.plusSeconds(300),
                now
        );
        tokenRepository.save(activeToken);

        Optional<PatientPasswordRecoveryToken> activeFound = tokenRepository.findLatestActiveByPhone(phone);
        assertTrue(activeFound.isPresent());
        assertEquals("active_code_hash", activeFound.get().getCodeHash());
        assertFalse(activeFound.get().isUsed());
    }

    @Test
    @DisplayName("Should find latest active token by userId")
    void shouldFindLatestActiveByUserId() {
        Instant now = Instant.now();
        PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.create(
                patientUser.getId(),
                phone,
                "user_code_hash",
                now.plusSeconds(300),
                now
        );
        tokenRepository.save(token);

        Optional<PatientPasswordRecoveryToken> found = tokenRepository.findLatestActiveByUserId(patientUser.getId());
        assertTrue(found.isPresent());
        assertEquals("user_code_hash", found.get().getCodeHash());
        assertEquals(patientUser.getId(), found.get().getUserId());
    }

    @Test
    @DisplayName("Should invalidate all active tokens by phone")
    void shouldInvalidateActiveTokensByPhone() {
        Instant now = Instant.now();

        PatientPasswordRecoveryToken token1 = PatientPasswordRecoveryToken.create(
                patientUser.getId(),
                phone,
                "token_1_hash",
                now.plusSeconds(300),
                now.minusSeconds(30)
        );
        tokenRepository.save(token1);

        PatientPasswordRecoveryToken token2 = PatientPasswordRecoveryToken.create(
                patientUser.getId(),
                phone,
                "token_2_hash",
                now.plusSeconds(300),
                now
        );
        tokenRepository.save(token2);

        // Invalidate active tokens for phone
        tokenRepository.invalidateActiveTokensByPhone(phone, now);

        Optional<PatientPasswordRecoveryToken> activeFound = tokenRepository.findLatestActiveByPhone(phone);
        assertFalse(activeFound.isPresent());
    }
}
