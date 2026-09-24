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
 * Executes the actual V87 migration file against an in-memory H2 database to prove
 * the DDL is H2-compatible (the full Flyway chain cannot run on H2 in this
 * environment because of a pre-existing duplicate-V78 collision on origin/develop).
 */
class PatientPortalNotificationMigrationH2Test {

    @Test
    void v87MigrationRunsAndIsUsableOnH2() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:v87;DB_CLOSE_DELAY=-1;MODE=LEGACY", "sa", "")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE patients (id BINARY(16) NOT NULL, PRIMARY KEY (id))");
            stmt.execute("CREATE TABLE appointments (id BINARY(16) NOT NULL, PRIMARY KEY (id))");
            stmt.execute("CREATE TABLE appointment_reschedule_logs (id BINARY(16) NOT NULL, PRIMARY KEY (id))");
            stmt.execute("CREATE TABLE clinical_results (id BINARY(16) NOT NULL, PRIMARY KEY (id))");

            String ddl = readResource("db/migration/V87__create_patient_portal_notifications.sql");
            RunScript.execute(conn, new StringReader(ddl));

            UUID patientId = UUID.randomUUID();
            UUID appointmentId = UUID.randomUUID();
            seedParent(stmt, "patients", patientId);
            seedParent(stmt, "appointments", appointmentId);

            insert(conn, patientId, appointmentId, null, null);

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM patient_portal_notifications")) {
                rs.next();
                assertEquals(1, rs.getInt(1));
            }

            // Idempotency guard rail: same (patient_id, type, appointment_id) must be rejected.
            assertThrows(SQLException.class, () -> insert(conn, patientId, appointmentId, null, null));
        }
    }

    private void seedParent(Statement stmt, String table, UUID id) throws SQLException {
        try (PreparedStatement ps = stmt.getConnection().prepareStatement(
                "INSERT INTO " + table + " (id) VALUES (?)")) {
            ps.setBytes(1, uuidBytes(id));
            ps.executeUpdate();
        }
    }

    private void insert(Connection conn, UUID patientId, UUID appointmentId,
            UUID rescheduleLogId, UUID clinicalResultId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO patient_portal_notifications "
                        + "(id, patient_id, type, title, message, read_at, created_at, appointment_id, reschedule_log_id, clinical_result_id) "
                        + "VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, ?)")) {
            ps.setBytes(1, uuidBytes(UUID.randomUUID()));
            ps.setBytes(2, uuidBytes(patientId));
            ps.setString(3, "APPOINTMENT_REMINDER");
            ps.setString(4, "Nhắc lịch hẹn");
            ps.setString(5, "message");
            ps.setTimestamp(6, Timestamp.from(Instant.now()));
            ps.setBytes(7, uuidBytes(appointmentId));
            ps.setBytes(8, rescheduleLogId == null ? null : uuidBytes(rescheduleLogId));
            ps.setBytes(9, clinicalResultId == null ? null : uuidBytes(clinicalResultId));
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
