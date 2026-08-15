package com.benhsoan.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonDatabaseBackupStorageAdapterIntegrationTest {

    private static final List<String> BACKUP_TABLES = List.of(
            "medicines",
            "medicine_batches",
            "prescriptions",
            "prescription_dispense_items",
            "invoices",
            "invoice_lines"
    );

    private JdbcTemplate jdbc;
    private JsonDatabaseBackupStorageAdapter adapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:backup-fk-" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        jdbc = new JdbcTemplate(dataSource);
        createSchema();

        adapter = new JsonDatabaseBackupStorageAdapter(
                jdbc,
                new ObjectMapper(),
                BACKUP_TABLES,
                tempDir
        );
    }

    private void createSchema() {
        jdbc.execute("""
                CREATE TABLE medicines (
                    id VARCHAR(36) PRIMARY KEY,
                    medicine_code VARCHAR(30) NOT NULL,
                    medicine_name VARCHAR(150) NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE medicine_batches (
                    id VARCHAR(36) PRIMARY KEY,
                    medicine_id VARCHAR(36) NOT NULL,
                    batch_number VARCHAR(50) NOT NULL,
                    FOREIGN KEY (medicine_id) REFERENCES medicines(id)
                )
                """);
        jdbc.execute("""
                CREATE TABLE prescriptions (
                    id VARCHAR(36) PRIMARY KEY,
                    prescription_code VARCHAR(30) NOT NULL,
                    medicine_id VARCHAR(36) NOT NULL,
                    FOREIGN KEY (medicine_id) REFERENCES medicines(id)
                )
                """);
        jdbc.execute("""
                CREATE TABLE prescription_dispense_items (
                    id VARCHAR(36) PRIMARY KEY,
                    prescription_id VARCHAR(36) NOT NULL,
                    medicine_batch_id VARCHAR(36) NOT NULL,
                    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
                    FOREIGN KEY (medicine_batch_id) REFERENCES medicine_batches(id)
                )
                """);
        jdbc.execute("""
                CREATE TABLE invoices (
                    id VARCHAR(36) PRIMARY KEY,
                    invoice_code VARCHAR(30) NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE invoice_lines (
                    id VARCHAR(36) PRIMARY KEY,
                    invoice_id VARCHAR(36) NOT NULL,
                    amount DECIMAL(15, 2) NOT NULL,
                    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
                )
                """);
    }
    @Test
    void foreignKeysAreEnforcedInTestSchema() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update("INSERT INTO medicine_batches (id, medicine_id, batch_number) VALUES (?, ?, ?)",
                        UUID.randomUUID().toString(), UUID.randomUUID().toString(), "ORPHAN-BATCH"));
    }

    @Test
    void restoresMultiTierDependencyChainWithoutForeignKeyViolations() {
        UUID medicineId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        UUID dispenseId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        seedChain(medicineId, batchId, prescriptionId, dispenseId, invoiceId, lineId);

        BackupSnapshot snapshot = adapter.exportSnapshot("BKP-FK-TEST");
        assertEquals("BKP-FK-TEST.json", snapshot.fileName());
        assertTrue(snapshot.content().length > 0);

        jdbc.update("UPDATE medicines SET medicine_name = ? WHERE id = ?", "CORRUPTED", medicineId.toString());
        jdbc.update("UPDATE invoice_lines SET amount = ? WHERE id = ?", new BigDecimal("999.00"), lineId.toString());

        adapter.restoreSnapshot("BKP-FK-TEST.json");

        assertEquals("Paracetamol 500 mg",
                jdbc.queryForObject("SELECT medicine_name FROM medicines WHERE id = ?", String.class, medicineId.toString()));
        assertEquals(0, new BigDecimal("100.00").compareTo(
                jdbc.queryForObject("SELECT amount FROM invoice_lines WHERE id = ?", BigDecimal.class, lineId.toString())));
        assertEquals("BATCH-001",
                jdbc.queryForObject("SELECT batch_number FROM medicine_batches WHERE id = ?", String.class, batchId.toString()));
        assertEquals("RX-001",
                jdbc.queryForObject("SELECT prescription_code FROM prescriptions WHERE id = ?", String.class, prescriptionId.toString()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM prescription_dispense_items", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM invoices", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM invoice_lines", Integer.class));
    }

    private void seedChain(UUID medicineId, UUID batchId, UUID prescriptionId, UUID dispenseId, UUID invoiceId, UUID lineId) {
        jdbc.update("INSERT INTO medicines (id, medicine_code, medicine_name) VALUES (?, ?, ?)",
                medicineId.toString(), "MED-001", "Paracetamol 500 mg");
        jdbc.update("INSERT INTO medicine_batches (id, medicine_id, batch_number) VALUES (?, ?, ?)",
                batchId.toString(), medicineId.toString(), "BATCH-001");
        jdbc.update("INSERT INTO prescriptions (id, prescription_code, medicine_id) VALUES (?, ?, ?)",
                prescriptionId.toString(), "RX-001", medicineId.toString());
        jdbc.update("INSERT INTO prescription_dispense_items (id, prescription_id, medicine_batch_id) VALUES (?, ?, ?)",
                dispenseId.toString(), prescriptionId.toString(), batchId.toString());
        jdbc.update("INSERT INTO invoices (id, invoice_code) VALUES (?, ?)",
                invoiceId.toString(), "INV-001");
        jdbc.update("INSERT INTO invoice_lines (id, invoice_id, amount) VALUES (?, ?, ?)",
                lineId.toString(), invoiceId.toString(), new BigDecimal("100.00"));
    }
}
