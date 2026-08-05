package com.benhsoan.persistence.adapterRepository.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionItemRepository;
import com.benhsoan.persistence.mapper.prescription.PrescriptionItemPersistenceMapper;
import com.benhsoan.persistence.mapper.prescription.PrescriptionPersistenceMapper;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        PrescriptionRepositoryAdapter.class,
        PrescriptionPersistenceMapper.class,
        PrescriptionItemPersistenceMapper.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PrescriptionRepositoryAdapterIntegrationTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-05T02:00:00Z");
    private static final Instant AMENDED_AT = Instant.parse("2026-08-05T03:00:00Z");

    @Autowired private PrescriptionRepositoryAdapter prescriptionRepository;
    @Autowired private JpaPrescriptionItemRepository prescriptionItemRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void replacesItemsInTheSameTransactionWithoutRetainingDeletedEntities() {
        UUID prescriptionId = UUID.randomUUID();
        UUID retainedItemId = UUID.randomUUID();
        UUID removedItemId = UUID.randomUUID();
        UUID addedItemId = UUID.randomUUID();
        UUID retainedMedicineId = UUID.randomUUID();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            prescriptionRepository.save(prescription(
                    prescriptionId,
                    "RX000001",
                    null,
                    List.of(
                            item(retainedItemId, prescriptionId, retainedMedicineId, "1 tablet", CREATED_AT, null),
                            item(removedItemId, prescriptionId, UUID.randomUUID(), "1 tablet", CREATED_AT, null)
                    )
            ));
            prescriptionRepository.save(prescription(
                    prescriptionId,
                    "RX000001",
                    AMENDED_AT,
                    List.of(
                            item(retainedItemId, prescriptionId, retainedMedicineId, "2 tablets", CREATED_AT, AMENDED_AT),
                            item(addedItemId, prescriptionId, UUID.randomUUID(), "1 tablet", AMENDED_AT, null)
                    )
            ));
        });

        Map<UUID, String> dosageByItemId = prescriptionItemRepository
                .findByPrescriptionIdOrderByCreatedAtAsc(prescriptionId)
                .stream()
                .collect(Collectors.toMap(item -> item.getId(), item -> item.getDosage()));

        assertEquals(2, dosageByItemId.size());
        assertEquals("2 tablets", dosageByItemId.get(retainedItemId));
        assertTrue(dosageByItemId.containsKey(addedItemId));
        assertFalse(dosageByItemId.containsKey(removedItemId));
    }

    private Prescription prescription(
            UUID id,
            String code,
            Instant updatedAt,
            List<PrescriptionItem> items
    ) {
        return Prescription.restore(
                id,
                code,
                UUID.randomUUID(),
                PrescriptionStatus.PENDING_DISPENSE,
                "Take after meals",
                UUID.randomUUID(),
                CREATED_AT,
                updatedAt == null ? null : UUID.randomUUID(),
                updatedAt,
                items
        );
    }

    private PrescriptionItem item(
            UUID itemId,
            UUID prescriptionId,
            UUID medicineId,
            String dosage,
            Instant createdAt,
            Instant updatedAt
    ) {
        return PrescriptionItem.restore(
                itemId,
                prescriptionId,
                medicineId,
                "Paracetamol",
                "Paracetamol",
                "500 mg",
                "tablet",
                dosage,
                "Twice daily",
                AdministrationRoute.ORAL,
                5,
                10,
                null,
                createdAt,
                updatedAt
        );
    }
}
