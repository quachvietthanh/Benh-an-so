package com.benhsoan.persistence.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;

/**
 * Executes the actual V96 migration against H2 to prove the scheduled-backup
 * DDL is H2-compatible: the singleton schedule table, the failure_reason column
 * and the widened backup_type check constraint.
 */
class ScheduledBackupMigrationH2Test {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void v96MigrationRunsAndWidensBackupTypeCheckOnH2() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:v96;DB_CLOSE_DELAY=-1;MODE=LEGACY", "sa", "")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE users (id BINARY(16) NOT NULL, PRIMARY KEY (id))");
            seedUser(stmt, USER_ID);
            stmt.execute("""
                    CREATE TABLE backup_records (
                        id BINARY(16) NOT NULL,
                        backup_code VARCHAR(30) NOT NULL,
                        file_name VARCHAR(255) NULL,
                        file_size BIGINT NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        backup_type VARCHAR(30) NOT NULL,
                        description VARCHAR(255) NULL,
                        created_by BINARY(16) NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        restored_at TIMESTAMP NULL,
                        restored_by BINARY(16) NULL,
                        CONSTRAINT pk_backup_records PRIMARY KEY (id),
                        CONSTRAINT fk_backup_records_created_by FOREIGN KEY (created_by) REFERENCES users(id),
                        CONSTRAINT chk_backup_records_type CHECK (backup_type IN ('FULL', 'MANUAL'))
                    )
                    """);

            String ddl = readResource("db/migration/V96__add_scheduled_backup_schema.sql");
            RunScript.execute(conn, new StringReader(ddl));

            // Singleton schedule is seeded, disabled by default.
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT enabled, backup_time FROM backup_schedules WHERE id = 1")) {
                rs.next();
                assertEquals(Boolean.FALSE, rs.getBoolean(1));
                assertEquals("02:00:00", rs.getString(2));
            }

            // failure_reason column exists.
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT failure_reason FROM backup_records WHERE 1 = 0")) {
                assertNotNull(rs);
            }

            // SCHEDULED type is now allowed by the widened check constraint.
            insertBackup(conn, "SCHEDULED");

            // An unknown type is still rejected.
            assertThrows(SQLException.class, () -> insertBackup(conn, "BOGUS"));
        }
    }

    private void insertBackup(Connection conn, String backupType) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO backup_records "
                        + "(id, backup_code, file_size, status, backup_type, created_by, created_at) "
                        + "VALUES (?, ?, 0, 'SUCCESS', ?, ?, ?)")) {
            ps.setBytes(1, uuidBytes(UUID.randomUUID()));
            ps.setString(2, "BKP-" + UUID.randomUUID().toString().substring(0, 20));
            ps.setString(3, backupType);
            ps.setBytes(4, uuidBytes(USER_ID));
            ps.setTimestamp(5, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        }
    }

    private void seedUser(Statement stmt, UUID id) throws SQLException {
        try (PreparedStatement ps = stmt.getConnection().prepareStatement(
                "INSERT INTO users (id) VALUES (?)")) {
            ps.setBytes(1, uuidBytes(id));
            ps.executeUpdate();
        }
    }

    private String readResource(String path) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "Migration resource not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private byte[] uuidBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        ByteBuffer.wrap(bytes).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits());
        return bytes;
    }
}
