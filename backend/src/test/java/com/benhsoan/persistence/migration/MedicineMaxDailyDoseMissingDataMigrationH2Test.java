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
 * Executes the actual V93 migration against H2 to prove the missing max-daily-dose
 * flag DDL is H2-compatible and that the unique (medicine_id, missing_reason)
 * constraint enforces idempotent tracking (no duplicate flags per medicine+reason).
 */
class MedicineMaxDailyDoseMissingDataMigrationH2Test {

    @Test
    void v93MigrationRunsAndEnforcesIdempotencyOnH2() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:v93;DB_CLOSE_DELAY=-1;MODE=LEGACY", "sa", "")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE medicines (id BINARY(16) NOT NULL, PRIMARY KEY (id))");

            String ddl = readResource("db/migration/V93__create_medicine_max_daily_dose_missing_flags.sql");
            RunScript.execute(conn, new StringReader(ddl));

            UUID medicineId = UUID.randomUUID();
            seed(stmt, "medicines", medicineId);

            insertFlag(conn, UUID.randomUUID(), medicineId, "MAX_DAILY_DOSE");

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM medicine_max_daily_dose_missing_flags")) {
                rs.next();
                assertEquals(1, rs.getInt(1));
            }

            // Same (medicine_id, missing_reason) must be rejected by the unique constraint.
            assertThrows(SQLException.class, () ->
                    insertFlag(conn, UUID.randomUUID(), medicineId, "MAX_DAILY_DOSE"));

            // A different reason for the same medicine is allowed (distinct tracking).
            insertFlag(conn, UUID.randomUUID(), medicineId, "STRENGTH_VALUE_MG");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM medicine_max_daily_dose_missing_flags")) {
                rs.next();
                assertEquals(2, rs.getInt(1));
            }
        }
    }

    private void seed(Statement stmt, String table, UUID id) throws SQLException {
        try (PreparedStatement ps = stmt.getConnection().prepareStatement(
                "INSERT INTO " + table + " (id) VALUES (?)")) {
            ps.setBytes(1, uuidBytes(id));
            ps.executeUpdate();
        }
    }

    private void insertFlag(Connection conn, UUID id, UUID medicineId, String missingReason)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO medicine_max_daily_dose_missing_flags "
                        + "(id, medicine_id, active_ingredient, missing_reason, first_detected_at, last_detected_at) "
                        + "VALUES (?, ?, 'Paracetamol', ?, ?, ?)")) {
            ps.setBytes(1, uuidBytes(id));
            ps.setBytes(2, uuidBytes(medicineId));
            ps.setString(3, missingReason);
            ps.setTimestamp(4, Timestamp.from(Instant.now()));
            ps.setTimestamp(5, Timestamp.from(Instant.now()));
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
