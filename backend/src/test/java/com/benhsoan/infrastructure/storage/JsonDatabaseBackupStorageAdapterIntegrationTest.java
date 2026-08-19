package com.benhsoan.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.benhsoan.domain.backup.exception.BackupExecutionException;
import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

@Testcontainers(disabledWithoutDocker = true)
class JsonDatabaseBackupStorageAdapterIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("backup_test")
            .withUsername("backup_test")
            .withPassword("backup_test");

    private static final BackupRestorePlan BACKUP_PLAN = new BackupRestorePlan(
            List.of("clinic_configuration", "rooms", "diagnosis_catalog", "clinical_service_catalog", "invoice_lines", "invoices",
                    "payments", "prescription_dispense_items", "prescriptions", "medicine_batches", "medicines",
                    "queue_items", "visits"),
            Set.of(),
            List.of(
                    dependency("medicine_batches", "medicine_id", "medicines"),
                    dependency("prescriptions", "medicine_id", "medicines"),
                    dependency("prescription_dispense_items", "prescription_id", "prescriptions"),
                    dependency("prescription_dispense_items", "medicine_batch_id", "medicine_batches"),
                    dependency("invoices", "payment_id", "payments"),
                    dependency("invoices", "original_invoice_id", "invoices"),
                    dependency("invoice_lines", "invoice_id", "invoices"),
                    dependency("queue_items", "visit_id", "visits"),
                    dependency("visits", "queue_item_id", "queue_items")
            ),
            Set.of(
                    new BackupRestorePlan.DeferredForeignKey("visits", "queue_item_id"),
                    new BackupRestorePlan.DeferredForeignKey("invoices", "original_invoice_id")
            )
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private JsonDatabaseBackupStorageAdapter adapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = mysqlDataSource();
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        recreateSchema();
        adapter = new JsonDatabaseBackupStorageAdapter(jdbc, objectMapper, BACKUP_PLAN, tempDir);
    }

    @Test
    void enforcesForeignKeysInActualMySqlSchema() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO medicine_batches (id, medicine_id, batch_number) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), "ORPHAN"));
    }

    @Test
    void restoresBillingAdjustmentQueueCycleAndCatalogs() {
        Fixture fixture = seedFixture();
        adapter.exportSnapshot("BKP-MYSQL-FK");

        jdbc.update("UPDATE clinic_configuration SET clinic_name = ? WHERE id = 1", "CORRUPTED");
        jdbc.update("UPDATE medicines SET medicine_name = ? WHERE id = ?", "CORRUPTED", fixture.medicineId());
        jdbc.update("UPDATE invoice_lines SET amount = ? WHERE id = ?", new BigDecimal("999.00"), fixture.lineId());
        jdbc.update("DELETE FROM diagnosis_catalog");
        jdbc.update("UPDATE visits SET queue_item_id = NULL WHERE id = ?", fixture.visitId());
        jdbc.update("UPDATE invoices SET original_invoice_id = NULL WHERE id = ?", fixture.adjustmentInvoiceId());

        transactions.executeWithoutResult(status -> adapter.restoreSnapshot("BKP-MYSQL-FK.json"));

        assertEquals("Clinic configuration", value("SELECT clinic_name FROM clinic_configuration WHERE id = 1"));
        assertEquals("Paracetamol 500 mg", value("SELECT medicine_name FROM medicines WHERE id = ?", fixture.medicineId()));
        assertEquals("D-001", value("SELECT diagnosis_code FROM diagnosis_catalog"));
        assertEquals(0, new BigDecimal("100.00").compareTo(jdbc.queryForObject(
                "SELECT amount FROM invoice_lines WHERE id = ?", BigDecimal.class, fixture.lineId())));
        assertEquals(fixture.queueItemId(), value("SELECT queue_item_id FROM visits WHERE id = ?", fixture.visitId()));
        assertEquals(fixture.invoiceId(), value("SELECT original_invoice_id FROM invoices WHERE id = ?", fixture.adjustmentInvoiceId()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payments", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM invoices", Integer.class));
    }

    @Test
    void rollsBackAllChangesWhenRestoreFailsMidway() throws Exception {
        Fixture fixture = seedFixture();
        BackupSnapshot snapshot = adapter.exportSnapshot("BKP-MYSQL-ROLLBACK");
        writeSnapshotWithInvalidInvoiceAmount(snapshot, fixture.lineId());

        jdbc.update("UPDATE medicines SET medicine_name = ? WHERE id = ?", "CURRENT-MEDICINE", fixture.medicineId());
        jdbc.update("UPDATE invoice_lines SET amount = ? WHERE id = ?", new BigDecimal("999.00"), fixture.lineId());

        assertThrows(BackupExecutionException.class, () -> transactions.executeWithoutResult(
                status -> adapter.restoreSnapshot("BKP-MYSQL-ROLLBACK.json")));

        assertEquals("CURRENT-MEDICINE", value("SELECT medicine_name FROM medicines WHERE id = ?", fixture.medicineId()));
        assertEquals(0, new BigDecimal("999.00").compareTo(jdbc.queryForObject(
                "SELECT amount FROM invoice_lines WHERE id = ?", BigDecimal.class, fixture.lineId())));
        assertEquals(fixture.queueItemId(), value("SELECT queue_item_id FROM visits WHERE id = ?", fixture.visitId()));
        assertEquals(fixture.invoiceId(), value("SELECT original_invoice_id FROM invoices WHERE id = ?", fixture.adjustmentInvoiceId()));
    }

    private void writeSnapshotWithInvalidInvoiceAmount(BackupSnapshot snapshot, String lineId) throws Exception {
        JsonDatabaseBackupStorageAdapter.BackupDocument document = objectMapper.readValue(
                snapshot.content(), JsonDatabaseBackupStorageAdapter.BackupDocument.class);
        List<JsonDatabaseBackupStorageAdapter.TableSnapshot> corrupted = document.data().stream()
                .map(table -> corruptInvoiceLine(table, lineId))
                .toList();
        Files.write(tempDir.resolve(snapshot.fileName()), objectMapper.writeValueAsBytes(
                new JsonDatabaseBackupStorageAdapter.BackupDocument(document.manifest(), corrupted)));
    }

    private JsonDatabaseBackupStorageAdapter.TableSnapshot corruptInvoiceLine(
            JsonDatabaseBackupStorageAdapter.TableSnapshot table, String lineId) {
        if (!"invoice_lines".equals(table.name())) {
            return table;
        }
        int id = indexOf(table, "id");
        int amount = indexOf(table, "amount");
        List<List<String>> rows = table.rows().stream().map(row -> {
            List<String> changed = new ArrayList<>(row);
            if (lineId.equals(row.get(id))) {
                changed.set(amount, "not-a-decimal");
            }
            return List.copyOf(changed);
        }).toList();
        return new JsonDatabaseBackupStorageAdapter.TableSnapshot(table.name(), table.columns(), rows);
    }

    private int indexOf(JsonDatabaseBackupStorageAdapter.TableSnapshot table, String column) {
        return java.util.stream.IntStream.range(0, table.columns().size())
                .filter(index -> column.equalsIgnoreCase(table.columns().get(index).name()))
                .findFirst().orElseThrow();
    }

    private Fixture seedFixture() {
        String medicineId = UUID.randomUUID().toString();
        String batchId = UUID.randomUUID().toString();
        String prescriptionId = UUID.randomUUID().toString();
        String dispenseId = UUID.randomUUID().toString();
        String paymentId = UUID.randomUUID().toString();
        String invoiceId = UUID.randomUUID().toString();
        String adjustmentInvoiceId = UUID.randomUUID().toString();
        String lineId = UUID.randomUUID().toString();
        String visitId = UUID.randomUUID().toString();
        String queueItemId = UUID.randomUUID().toString();

        jdbc.update("INSERT INTO rooms VALUES (?, ?)", UUID.randomUUID().toString(), "ROOM-001");
        jdbc.update("INSERT INTO diagnosis_catalog VALUES (?, ?)", UUID.randomUUID().toString(), "D-001");
        jdbc.update("INSERT INTO clinical_service_catalog VALUES (?, ?)", UUID.randomUUID().toString(), "S-001");
        jdbc.update("INSERT INTO medicines VALUES (?, ?, ?)", medicineId, "MED-001", "Paracetamol 500 mg");
        jdbc.update("INSERT INTO medicine_batches VALUES (?, ?, ?)", batchId, medicineId, "BATCH-001");
        jdbc.update("INSERT INTO prescriptions VALUES (?, ?, ?)", prescriptionId, medicineId, "RX-001");
        jdbc.update("INSERT INTO prescription_dispense_items VALUES (?, ?, ?)", dispenseId, prescriptionId, batchId);
        jdbc.update("INSERT INTO visits VALUES (?, ?)", visitId, null);
        jdbc.update("INSERT INTO queue_items VALUES (?, ?)", queueItemId, visitId);
        jdbc.update("UPDATE visits SET queue_item_id = ? WHERE id = ?", queueItemId, visitId);
        jdbc.update("INSERT INTO payments VALUES (?)", paymentId);
        jdbc.update("INSERT INTO invoices VALUES (?, ?, ?, ?, ?)", invoiceId, "INV-001", paymentId, null, "ORIGINAL");
        jdbc.update("INSERT INTO invoices VALUES (?, ?, ?, ?, ?)", adjustmentInvoiceId, "INV-002", null, invoiceId, "ADJUSTMENT");
        jdbc.update("INSERT INTO invoice_lines VALUES (?, ?, ?)", lineId, invoiceId, new BigDecimal("100.00"));
        return new Fixture(medicineId, invoiceId, adjustmentInvoiceId, lineId, visitId, queueItemId);
    }

    private String value(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, String.class, arguments);
    }

    private DriverManagerDataSource mysqlDataSource() {
        return new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private void recreateSchema() {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : BACKUP_PLAN.snapshotTables()) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        }
        jdbc.execute("DROP TABLE IF EXISTS flyway_schema_history");
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
        jdbc.execute("CREATE TABLE flyway_schema_history (installed_rank INT PRIMARY KEY, version VARCHAR(50), success BOOLEAN NOT NULL) ENGINE=InnoDB");
        jdbc.update("INSERT INTO flyway_schema_history VALUES (?, ?, ?)", 1, "test-schema-1", true);
        jdbc.execute("CREATE TABLE clinic_configuration (id TINYINT PRIMARY KEY, clinic_name VARCHAR(150) NOT NULL) ENGINE=InnoDB");
        jdbc.update("INSERT INTO clinic_configuration VALUES (?, ?)", 1, "Clinic configuration");
        jdbc.execute("CREATE TABLE rooms (id VARCHAR(36) PRIMARY KEY, room_code VARCHAR(30) NOT NULL) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE diagnosis_catalog (id VARCHAR(36) PRIMARY KEY, diagnosis_code VARCHAR(30) NOT NULL) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE clinical_service_catalog (id VARCHAR(36) PRIMARY KEY, service_code VARCHAR(30) NOT NULL) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE visits (id VARCHAR(36) PRIMARY KEY, queue_item_id VARCHAR(36)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE queue_items (id VARCHAR(36) PRIMARY KEY, visit_id VARCHAR(36) NOT NULL, FOREIGN KEY (visit_id) REFERENCES visits(id)) ENGINE=InnoDB");
        jdbc.execute("ALTER TABLE visits ADD FOREIGN KEY (queue_item_id) REFERENCES queue_items(id)");
        jdbc.execute("CREATE TABLE medicines (id VARCHAR(36) PRIMARY KEY, medicine_code VARCHAR(30) NOT NULL, medicine_name VARCHAR(150) NOT NULL) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE medicine_batches (id VARCHAR(36) PRIMARY KEY, medicine_id VARCHAR(36) NOT NULL, batch_number VARCHAR(50) NOT NULL, FOREIGN KEY (medicine_id) REFERENCES medicines(id)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE prescriptions (id VARCHAR(36) PRIMARY KEY, medicine_id VARCHAR(36) NOT NULL, prescription_code VARCHAR(30) NOT NULL, FOREIGN KEY (medicine_id) REFERENCES medicines(id)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE prescription_dispense_items (id VARCHAR(36) PRIMARY KEY, prescription_id VARCHAR(36) NOT NULL, medicine_batch_id VARCHAR(36) NOT NULL, FOREIGN KEY (prescription_id) REFERENCES prescriptions(id), FOREIGN KEY (medicine_batch_id) REFERENCES medicine_batches(id)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE payments (id VARCHAR(36) PRIMARY KEY) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE invoices (id VARCHAR(36) PRIMARY KEY, invoice_code VARCHAR(30) NOT NULL, payment_id VARCHAR(36), original_invoice_id VARCHAR(36), invoice_type VARCHAR(30) NOT NULL, FOREIGN KEY (payment_id) REFERENCES payments(id), FOREIGN KEY (original_invoice_id) REFERENCES invoices(id), CONSTRAINT chk_invoices_original_shape CHECK ((invoice_type = 'ORIGINAL' AND original_invoice_id IS NULL) OR (invoice_type = 'ADJUSTMENT' AND original_invoice_id IS NOT NULL))) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE invoice_lines (id VARCHAR(36) PRIMARY KEY, invoice_id VARCHAR(36) NOT NULL, amount DECIMAL(15, 2) NOT NULL, FOREIGN KEY (invoice_id) REFERENCES invoices(id)) ENGINE=InnoDB");
    }

    private static BackupRestorePlan.ForeignKeyDependency dependency(String child, String column, String parent) {
        return new BackupRestorePlan.ForeignKeyDependency(child, column, parent);
    }

    private record Fixture(String medicineId, String invoiceId, String adjustmentInvoiceId,
                           String lineId, String visitId, String queueItemId) {
    }
}
