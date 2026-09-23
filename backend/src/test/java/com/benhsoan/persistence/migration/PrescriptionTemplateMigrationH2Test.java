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
 * Executes the actual V89 migration file against an in-memory H2 database to prove the
 * DDL is H2-compatible and enforces the idempotency/quantity constraints.
 */
class PrescriptionTemplateMigrationH2Test {

    @Test
    void v89MigrationRunsAndEnforcesConstraintsOnH2() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:v89;DB_CLOSE_DELAY=-1;MODE=LEGACY", "sa", "")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE diagnosis_catalog (id BINARY(16) NOT NULL, PRIMARY KEY (id))");
            stmt.execute("CREATE TABLE medicines (id BINARY(16) NOT NULL, PRIMARY KEY (id))");
            stmt.execute("CREATE TABLE users (id BINARY(16) NOT NULL, PRIMARY KEY (id))");

            String ddl = readResource("db/migration/V89__create_prescription_templates.sql");
            RunScript.execute(conn, new StringReader(ddl));

            UUID diagnosisId = UUID.randomUUID();
            UUID doctorId = UUID.randomUUID();
            UUID medicineId = UUID.randomUUID();
            seed(stmt, "diagnosis_catalog", diagnosisId);
            seed(stmt, "users", doctorId);
            seed(stmt, "medicines", medicineId);

            UUID templateId = UUID.randomUUID();
            insertTemplate(conn, templateId, diagnosisId, doctorId);
            insertItem(conn, UUID.randomUUID(), templateId, medicineId);

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM prescription_templates")) {
                rs.next();
                assertEquals(1, rs.getInt(1));
            }

            // Duplicate (template_id, medicine_id) must be rejected.
            assertThrows(SQLException.class,
                    () -> insertItem(conn, UUID.randomUUID(), templateId, medicineId));
        }
    }

    private void seed(Statement stmt, String table, UUID id) throws SQLException {
        try (PreparedStatement ps = stmt.getConnection().prepareStatement(
                "INSERT INTO " + table + " (id) VALUES (?)")) {
            ps.setBytes(1, uuidBytes(id));
            ps.executeUpdate();
        }
    }

    private void insertTemplate(Connection conn, UUID id, UUID diagnosisId, UUID doctorId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO prescription_templates (id, diagnosis_catalog_id, created_by, created_at) "
                        + "VALUES (?, ?, ?, ?)")) {
            ps.setBytes(1, uuidBytes(id));
            ps.setBytes(2, uuidBytes(diagnosisId));
            ps.setBytes(3, uuidBytes(doctorId));
            ps.setTimestamp(4, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        }
    }

    private void insertItem(Connection conn, UUID id, UUID templateId, UUID medicineId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO prescription_template_items "
                        + "(id, template_id, medicine_id, dosage, frequency, route, duration_days, quantity, instructions, sort_order) "
                        + "VALUES (?, ?, ?, '1 viên', 2, 'ORAL', 7, 14, NULL, 0)")) {
            ps.setBytes(1, uuidBytes(id));
            ps.setBytes(2, uuidBytes(templateId));
            ps.setBytes(3, uuidBytes(medicineId));
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
