package com.benhsoan.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.persistence.entity.inventory.MedicineBatchEntity;
import com.benhsoan.persistence.entity.medicine.MedicineEntity;
import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:backup-storage-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class JsonDatabaseBackupStorageAdapterIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @TempDir
    Path tempDir;

    @Test
    void exportsAndRestoresOperationalTables() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        JsonDatabaseBackupStorageAdapter adapter = new JsonDatabaseBackupStorageAdapter(
                jdbc,
                new ObjectMapper(),
                List.of("medicines", "medicine_batches"),
                tempDir
        );

        UUID medicineId = UUID.randomUUID();
        entityManager.persist(MedicineEntity.builder()
                .id(medicineId)
                .medicineCode("MED-PARA-500")
                .medicineName("Paracetamol 500 mg")
                .activeIngredient("Paracetamol")
                .strength("500 mg")
                .dosageForm(DosageForm.TABLET)
                .unit("vien")
                .defaultRoute(AdministrationRoute.ORAL)
                .active(true)
                .stockQuantity(100)
                .minStockThreshold(20)
                .createdAt(Instant.parse("2026-08-14T08:00:00Z"))
                .build());
        entityManager.persist(MedicineBatchEntity.builder()
                .id(UUID.randomUUID())
                .medicineId(medicineId)
                .batchNumber("BATCH-001")
                .expiryDate(LocalDate.of(2026, 12, 31))
                .quantity(40)
                .status(BatchStatus.ACTIVE)
                .createdAt(Instant.parse("2026-08-14T08:00:00Z"))
                .build());
        entityManager.flush();

        BackupSnapshot snapshot = adapter.exportSnapshot("BKP-TEST");
        assertTrue(snapshot.content().length > 0);
        assertEquals("BKP-TEST.json", snapshot.fileName());

        jdbc.update("DELETE FROM medicine_batches");
        jdbc.update("DELETE FROM medicines");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM medicines", Integer.class));

        adapter.restoreSnapshot("BKP-TEST.json");

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM medicines", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM medicine_batches", Integer.class));
        assertEquals("Paracetamol 500 mg",
                jdbc.queryForObject("SELECT medicine_name FROM medicines", String.class));
        assertEquals(40, jdbc.queryForObject("SELECT quantity FROM medicine_batches", Integer.class));
    }
}
