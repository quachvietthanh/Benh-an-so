package com.benhsoan.persistence.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;

/**
 * Verifies the V96 {@code clinic_configuration} ALTER statements (session
 * timeout/warning columns, defaults and CHECK constraints) against H2.
 *
 * <p>The permission-seeding section of V96 uses MySQL {@code UUID_TO_BIN(...)}
 * (same pattern as V40/V86) which H2 does not support, so it is intentionally
 * not executed here; it is covered by the MySQL Flyway path.</p>
 */
class SessionManagementMigrationH2Test {

    @Test
    void v96MigrationAltersClinicConfigurationOnH2() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:v96;DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", "")) {
            Statement stmt = conn.createStatement();

            stmt.execute("CREATE TABLE clinic_configuration (id INT NOT NULL, PRIMARY KEY (id))");
            stmt.execute("INSERT INTO clinic_configuration (id) VALUES (1)");

            String ddl = readResource("db/migration/V96__add_session_management.sql");
            String alterPortion = ddl.substring(0, ddl.indexOf("-- Session-management permissions"));
            RunScript.execute(conn, new StringReader(alterPortion));

            // Defaults apply to the existing singleton row.
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT session_timeout_minutes, session_warning_minutes FROM clinic_configuration WHERE id = 1")) {
                rs.next();
                assertEquals(30, rs.getInt(1));
                assertEquals(5, rs.getInt(2));
            }

            // CHECK constraints reject out-of-range values.
            assertThrows(SQLException.class, () -> stmt.execute(
                    "UPDATE clinic_configuration SET session_timeout_minutes = 0 WHERE id = 1"));
            assertThrows(SQLException.class, () -> stmt.execute(
                    "UPDATE clinic_configuration SET session_timeout_minutes = 2000 WHERE id = 1"));
            assertThrows(SQLException.class, () -> stmt.execute(
                    "UPDATE clinic_configuration SET session_warning_minutes = -1 WHERE id = 1"));
        }
    }

    private String readResource(String path) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "Migration resource not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}