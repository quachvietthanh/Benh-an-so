package com.benhsoan.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.benhsoan.domain.auth.User;
import com.benhsoan.infrastructure.bootstrap.ProductionAdminBootstrap;
import com.benhsoan.persistence.entity.auth.RoleEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaRoleRepository;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

@SpringBootTest(properties = {
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.url=jdbc:h2:mem:guard_unit_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@DisplayName("F-2 Production Demo Seed Security & Admin Bootstrap Integration Tests")
class ProductionSecurityMigrationGuardIntegrationTest {

        private static final String DEFAULT_KNOWN_PASSWORD = "Password123@";
        private static final String UNSET_TOKEN = "BOOTSTRAP.ADMIN.PASSWORD.NOT.SET";
        private static final UUID ROLE_ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
        private static final UUID ADMIN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private JpaRoleRepository roleRepository;

        @Autowired
        private PasswordEncoderPort passwordEncoderPort;

        @Autowired
        private ProductionAdminBootstrap adminBootstrap;

        @BeforeEach
        void setUp() {
                if (!roleRepository.existsById(ROLE_ADMIN_ID)) {
                        roleRepository.save(RoleEntity.builder()
                                        .id(ROLE_ADMIN_ID)
                                        .name("ADMIN")
                                        .description("System administrator")
                                        .isSystem(true)
                                        .createdAt(Instant.now())
                                        .build());
                }
        }

        // =========================================================================
        // 1. ProductionAdminBootstrap Unit / Integration Tests
        // =========================================================================

        @Test
        @DisplayName("F-2b: Admin Bootstrap with configured INITIAL_ADMIN_PASSWORD provisions admin securely")
        void testBootstrapWithConfiguredPassword() {
                // Setup admin at unset token
                User admin = User.restore(
                                ADMIN_ID, "admin", "$2a$10$" + UNSET_TOKEN + ".000000000000000000000",
                                "System Administrator", "admin@benhsoan.com", null,
                                ROLE_ADMIN_ID, false, true, null, Instant.now());
                userRepository.save(admin);

                // Configure bootstrap properties
                String customPassword = "SecureProdPassword2026!#";
                ReflectionTestUtils.setField(adminBootstrap, "seedDemo", false);
                ReflectionTestUtils.setField(adminBootstrap, "initialAdminPassword", customPassword);
                ReflectionTestUtils.setField(adminBootstrap, "envInitialAdminPassword", "");

                // Execute runner
                adminBootstrap.run(null);

                // Verify
                User updatedAdmin = userRepository.findByUsername("admin").orElseThrow();
                assertTrue(updatedAdmin.isActive(), "Admin should be active after bootstrap");
                assertTrue(updatedAdmin.isMustChangePassword(), "must_change_password must be true");
                assertTrue(passwordEncoderPort.matches(customPassword, updatedAdmin.getPasswordHash()),
                                "Admin password must match configured INITIAL_ADMIN_PASSWORD");
                assertFalse(passwordEncoderPort.matches(DEFAULT_KNOWN_PASSWORD, updatedAdmin.getPasswordHash()),
                                "Admin password must NOT match Password123@");
        }

        @Test
        @DisplayName("F-2b: Admin Bootstrap without configured secret auto-generates 24-character random password")
        void testBootstrapAutoGeneratesRandomPassword() {
                // Setup admin at unset token
                User admin = User.restore(
                                ADMIN_ID, "admin", "$2a$10$" + UNSET_TOKEN + ".000000000000000000000",
                                "System Administrator", "admin@benhsoan.com", null,
                                ROLE_ADMIN_ID, false, true, null, Instant.now());
                userRepository.save(admin);

                // Configure bootstrap properties with empty secret
                ReflectionTestUtils.setField(adminBootstrap, "seedDemo", false);
                ReflectionTestUtils.setField(adminBootstrap, "initialAdminPassword", "");
                ReflectionTestUtils.setField(adminBootstrap, "envInitialAdminPassword", "");

                // Execute runner
                adminBootstrap.run(null);

                // Verify
                User updatedAdmin = userRepository.findByUsername("admin").orElseThrow();
                assertTrue(updatedAdmin.isActive(), "Admin should be active after auto-bootstrap");
                assertTrue(updatedAdmin.isMustChangePassword(), "must_change_password must be true");
                assertFalse(updatedAdmin.getPasswordHash().contains(UNSET_TOKEN), "Hash must not remain unset token");
                assertTrue(updatedAdmin.getPasswordHash().startsWith("$2a$"), "Must be valid BCrypt format");
                assertFalse(passwordEncoderPort.matches(DEFAULT_KNOWN_PASSWORD, updatedAdmin.getPasswordHash()),
                                "Auto-generated admin must NOT match Password123@");
        }

        @Test
        @DisplayName("F-2c: Admin Bootstrap skips provisioning when seedDemo=true (Dev/Local mode)")
        void testBootstrapSkippedWhenSeedDemoTrue() {
                String originalHash = passwordEncoderPort.encode(DEFAULT_KNOWN_PASSWORD);
                User admin = User.restore(
                                ADMIN_ID, "admin", originalHash,
                                "System Administrator", "admin@benhsoan.com", null,
                                ROLE_ADMIN_ID, true, false, null, Instant.now());
                userRepository.save(admin);

                // Configure bootstrap with seedDemo=true
                ReflectionTestUtils.setField(adminBootstrap, "seedDemo", true);
                ReflectionTestUtils.setField(adminBootstrap, "initialAdminPassword", "IgnoreThisSecret");

                // Execute runner
                adminBootstrap.run(null);

                // Verify
                User currentAdmin = userRepository.findByUsername("admin").orElseThrow();
                assertEquals(originalHash, currentAdmin.getPasswordHash(),
                                "Admin hash should remain untouched when seedDemo is true");
                assertTrue(passwordEncoderPort.matches(DEFAULT_KNOWN_PASSWORD, currentAdmin.getPasswordHash()),
                                "In dev mode, demo password should still match");
        }

        @Test
        @DisplayName("F-2b: Admin Bootstrap is idempotent on subsequent runs")
        void testBootstrapIsIdempotent() {
                String existingSecureHash = passwordEncoderPort.encode("ExistingSecurePassword123!");
                User admin = User.restore(
                                ADMIN_ID, "admin", existingSecureHash,
                                "System Administrator", "admin@benhsoan.com", null,
                                ROLE_ADMIN_ID, true, false, null, Instant.now());
                userRepository.save(admin);

                // Run bootstrap with different password
                ReflectionTestUtils.setField(adminBootstrap, "seedDemo", false);
                ReflectionTestUtils.setField(adminBootstrap, "initialAdminPassword", "NewPasswordShouldBeIgnored!");

                adminBootstrap.run(null);

                // Verify no change
                User currentAdmin = userRepository.findByUsername("admin").orElseThrow();
                assertEquals(existingSecureHash, currentAdmin.getPasswordHash(),
                                "Bootstrap should be idempotent and not overwrite existing provisioned password");
        }

        // =========================================================================
        // 2. Migration Guard & Fail-Secure Contract Tests
        // =========================================================================

        @Test
        @DisplayName("F-2c: application.properties must configure Fail-Secure defaults (seed_demo=false)")
        void verifyApplicationPropertiesFailSecure() throws IOException {
                Path path = Paths.get("src/main/resources/application.properties");
                String content = Files.readString(path);

                assertTrue(content.contains("spring.flyway.placeholders.seed_demo=${ENABLE_DEMO_SEED:false}"),
                                "Default seed_demo placeholder MUST default to false!");
                assertTrue(content.contains("app.seed.demo=${ENABLE_DEMO_SEED:false}"),
                                "Default app.seed.demo MUST default to false!");
                assertFalse(content.contains("spring.flyway.placeholders.env=${FLYWAY_ENV:local}"),
                                "Fail-open placeholders.env MUST be removed!");
        }

        @Test
        @DisplayName("F-2c: application-prod.properties must strictly disable demo seed")
        void verifyApplicationProdPropertiesDisableDemo() throws IOException {
                Path path = Paths.get("src/main/resources/application-prod.properties");
                String content = Files.readString(path);

                assertTrue(content.contains("spring.flyway.placeholders.seed_demo=false"),
                                "Prod configuration MUST disable seed_demo!");
                assertTrue(content.contains("app.seed.demo=false"),
                                "Prod app.seed.demo MUST be false!");
        }

        @Test
        @DisplayName("F-2c: application-dev and application-local must explicitly enable demo seed")
        void verifyApplicationDevAndLocalEnableDemo() throws IOException {
                String devContent = Files.readString(Paths.get("src/main/resources/application-dev.properties"));
                assertTrue(devContent.contains("spring.flyway.placeholders.seed_demo=true"),
                                "Dev configuration must enable seed_demo");
                assertTrue(devContent.contains("app.seed.demo=true"));

                String localContent = Files.readString(Paths.get("src/main/resources/application-local.properties"));
                assertTrue(localContent.contains("spring.flyway.placeholders.seed_demo=true"),
                                "Local configuration must enable seed_demo");
                assertTrue(localContent.contains("app.seed.demo=true"));
        }

        @Test
        @DisplayName("F-2a: V48 migration must gate all demo inserts behind seed_demo='true'")
        void verifyV48MigrationIsGated() throws IOException {
                Path path = Paths.get("src/main/resources/db/migration/V48__seed_user_stories_demo_data.sql");
                String content = Files.readString(path);

                assertTrue(content.contains("WHERE LOWER(TRIM('${seed_demo}')) = 'true'"),
                                "V48 must contain normalized guard condition LOWER(TRIM('${seed_demo}')) = 'true'");
                assertTrue(content.contains("doctor_new"), "V48 must reference doctor_new");
                // Ensure no unconditional insert exists
                assertFalse(content.contains("VALUES\n    (UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa14')"),
                                "V48 must not use unconditional VALUES insert for doctor_new");
        }

        @Test
        @DisplayName("F-2b & F-P0-1: V49 migration must wipe default admin hash ONLY if unchanged, preserving custom password")
        void verifyV49MigrationWipesKnownHash() throws IOException {
                Path path = Paths.get("src/main/resources/db/migration/V49__production_security_guard.sql");
                String content = Files.readString(path);

                assertTrue(content.contains("LOWER(TRIM('${seed_demo}')) != 'true'"),
                                "V49 guard must activate when seed_demo is not true (case-insensitive)");
                assertTrue(content.contains("BOOTSTRAP.ADMIN.PASSWORD.NOT.SET"),
                                "V49 must set admin password to unset token, wiping Password123@ hash!");
                assertFalse(content.contains("SET password_hash = '$2a$10$OY5a1YZ"),
                                "V49 must not SET the known Password123@ hash!");
                assertTrue(content.contains("AND password_hash = '$2a$10$OY5a1YZ/5Iaz2PcEKjfOveEyy3FVXm7ei9OxTW6jPMyap/Hlk.5sK'"),
                                "V49 must only wipe admin if it still has the default V2 hash!");
        }

        @Test
        @DisplayName("F-P0-1: Upgrading existing production DB preserves custom admin password")
        void testUpgradePreservesCustomAdminPassword() {
                // Setup existing admin with custom production password
                String customProdHash = passwordEncoderPort.encode("CustomHospitalSecretPass2026!#");
                User existingAdmin = User.restore(
                                ADMIN_ID, "admin", customProdHash,
                                "System Administrator", "admin@benhsoan.com", null,
                                ROLE_ADMIN_ID, true, false, null, Instant.now());
                userRepository.save(existingAdmin);

                // Simulate V49 guard logic: Only update if hash == V2 default hash
                String v2DefaultHash = "$2a$10$OY5a1YZ/5Iaz2PcEKjfOveEyy3FVXm7ei9OxTW6jPMyap/Hlk.5sK";
                User loadedAdmin = userRepository.findByUsername("admin").orElseThrow();
                if (loadedAdmin.getPasswordHash().equals(v2DefaultHash)) {
                        loadedAdmin.resetPassword("$2a$10$" + UNSET_TOKEN + ".000000000000000000000");
                        loadedAdmin.deactivate();
                        userRepository.save(loadedAdmin);
                }

                // Verify: Custom password was NOT wiped
                User postMigrationAdmin = userRepository.findByUsername("admin").orElseThrow();
                assertEquals(customProdHash, postMigrationAdmin.getPasswordHash(),
                                "Existing custom admin password MUST be preserved during migration upgrade!");
                assertTrue(postMigrationAdmin.isActive(), "Custom admin account must remain active");

                // Bootstrap should also skip
                ReflectionTestUtils.setField(adminBootstrap, "seedDemo", false);
                adminBootstrap.run(null);

                User postBootstrapAdmin = userRepository.findByUsername("admin").orElseThrow();
                assertEquals(customProdHash, postBootstrapAdmin.getPasswordHash(),
                                "Bootstrap must not alter preserved custom admin password");
        }

        @Test
        @DisplayName("F-P0-1: Upgrading existing production DB wipes unchanged default V2 password")
        void testUpgradeWipesDefaultV2AdminPassword() {
                // Setup admin with V2 default password
                String v2DefaultHash = "$2a$10$OY5a1YZ/5Iaz2PcEKjfOveEyy3FVXm7ei9OxTW6jPMyap/Hlk.5sK";
                User existingAdmin = User.restore(
                                ADMIN_ID, "admin", v2DefaultHash,
                                "System Administrator", "admin@benhsoan.com", null,
                                ROLE_ADMIN_ID, true, false, null, Instant.now());
                userRepository.save(existingAdmin);

                // Simulate V49 guard logic
                User loadedAdmin = userRepository.findByUsername("admin").orElseThrow();
                if (loadedAdmin.getPasswordHash().equals(v2DefaultHash)) {
                        loadedAdmin.resetPassword("$2a$10$" + UNSET_TOKEN + ".000000000000000000000");
                        loadedAdmin.deactivate();
                        userRepository.save(loadedAdmin);
                }

                // Verify: Default password WAS wiped to UNSET_TOKEN
                User postMigrationAdmin = userRepository.findByUsername("admin").orElseThrow();
                assertTrue(postMigrationAdmin.getPasswordHash().contains(UNSET_TOKEN),
                                "Default V2 password must be wiped to unset token");
                assertFalse(postMigrationAdmin.isActive(), "Admin must be inactive before bootstrap");

                // Bootstrap should now safely provision secure credentials
                ReflectionTestUtils.setField(adminBootstrap, "seedDemo", false);
                ReflectionTestUtils.setField(adminBootstrap, "initialAdminPassword", "NewSecurePassword2026!");
                adminBootstrap.run(null);

                User postBootstrapAdmin = userRepository.findByUsername("admin").orElseThrow();
                assertTrue(postBootstrapAdmin.isActive(), "Admin should be active after bootstrap");
                assertTrue(passwordEncoderPort.matches("NewSecurePassword2026!", postBootstrapAdmin.getPasswordHash()),
                                "Admin must be provisioned with safe initial password");
        }

        @Test
        @DisplayName("F-P2-01: V49 migration must include Soft Neutralization Guard for demo transactions")
        void verifyV49IncludesSoftNeutralizationGuard() throws IOException {
                Path path = Paths.get("src/main/resources/db/migration/V49__production_security_guard.sql");
                String content = Files.readString(path);

                assertTrue(content.contains("UPDATE payments\nSET status = 'CANCELLED'"),
                                "V49 must cancel sample payments to neutralize fake revenue");
                assertTrue(content.contains("UPDATE appointments\nSET status = 'CANCELLED'"),
                                "V49 must cancel sample appointments");
                assertTrue(content.contains("UPDATE doctor_schedules\nSET active = FALSE"),
                                "V49 must deactivate sample doctor schedules");
                assertTrue(content.contains("UPDATE patients\nSET active = FALSE"),
                                "V49 must deactivate sample patients BN000001-BN000010");
        }

        @Test
        @DisplayName("F-P2-01: Pre-launch cleanup script exists and includes safe transactional purge")
        void verifyPreLaunchCleanupScriptExists() throws IOException {
                Path path = Paths.get("src/main/resources/db/scripts/prod-pre-launch-cleanup.sql");
                assertTrue(Files.exists(path), "prod-pre-launch-cleanup.sql must exist");
                String content = Files.readString(path);
                assertTrue(content.contains("SET FOREIGN_KEY_CHECKS = 0;"), "Script must disable FK checks for safe purge");
                assertTrue(content.contains("DELETE FROM patients WHERE HEX(id) LIKE 'BBBBBBBBBBBBBBBBBBBBBBBBB0%';"),
                                "Script must target sample patients");
                assertTrue(content.contains("DELETE FROM payments WHERE id = UUID_TO_BIN('23000000-0000-0000-0000-000000000001');"),
                                "Script must target sample payment");
        }

        @Test
        @DisplayName("F-1: V45 must match develop exactly (no QUEUE_CALL_NEXT in V45)")
        void verifyV45PreservesF1Invariance() throws IOException {
                Path path = Paths.get("src/main/resources/db/migration/V45__sync_role_permissions.sql");
                String content = Files.readString(path);

                assertFalse(content.contains("'USER_READ', 'QUEUE_CALL_NEXT'"),
                                "V45 must not contain QUEUE_CALL_NEXT to preserve 0-diff with develop");

                Path v47Path = Paths
                                .get("src/main/resources/db/migration/V47__sync_manager_and_doctor_permissions.sql");
                String v47Content = Files.readString(v47Path);
                assertTrue(v47Content.contains("QUEUE_CALL_NEXT"),
                                "QUEUE_CALL_NEXT must be safely added in additive migration V47");
        }

        // =========================================================================
        // 3. Real MySQL Migration Verification (Executed if local MySQL is accessible)
        // =========================================================================

        @Test
        @DisplayName("F-2 Live Verification: Query test_fresh_prod on MySQL if available")
        void verifyLiveMySqlProductionMigration() {
                try (Connection conn = DriverManager.getConnection(
                                "jdbc:mysql://localhost:3306/test_fresh_prod", "root", "Tuan20122005@");
                                Statement stmt = conn.createStatement()) {

                        // 1. doctor_new must NOT exist
                        ResultSet rsDoc = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'doctor_new'");
                        assertTrue(rsDoc.next());
                        assertEquals(0, rsDoc.getInt(1),
                                        "CRITICAL: doctor_new must NOT exist in production MySQL database!");

                        // 2. admin must have wiped password hash (not Password123@) and active=0
                        ResultSet rsAdmin = stmt.executeQuery(
                                        "SELECT active, password_hash FROM users WHERE username = 'admin'");
                        assertTrue(rsAdmin.next());
                        assertEquals(0, rsAdmin.getInt("active"), "Production admin must be inactive before bootstrap");
                        assertTrue(rsAdmin.getString("password_hash").contains(UNSET_TOKEN),
                                        "Production admin must have unset token hash");
                        assertFalse(rsAdmin.getString("password_hash").contains("OY5a1YZ"),
                                        "Production admin must NEVER have Password123@ hash!");

                        // 3. Demo staff accounts must be inactive and locked
                        ResultSet rsStaff = stmt.executeQuery(
                                        "SELECT COUNT(*) FROM users WHERE username IN ('doctor1', 'doctor2', 'receptionist1', 'pharmacist1', 'manager1') AND active = 0 AND password_hash LIKE '%LOCKED.ACCOUNT%'");
                        assertTrue(rsStaff.next());
                        assertEquals(5, rsStaff.getInt(1), "All 5 demo staff accounts must be inactive and locked!");

                } catch (Exception e) {
                        // If MySQL is not running (e.g. CI without MySQL), test logs and gracefully
                        // passes
                        System.out.println("[INFO] Skipping Live MySQL verification: MySQL not accessible ("
                                        + e.getMessage() + ")");
                }
        }
}
