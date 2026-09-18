package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.benhsoan.application.ucservice.inventory.EligibleStockSnapshotService;
import com.benhsoan.application.ucservice.inventory.LowStockAlertTransitionService;
import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.port.dto.command.prescription.DispenseItemCommand;
import com.benhsoan.port.dto.command.prescription.DispensePrescriptionItemsCommand;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:partial_dispense_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class PartialDispensePrescriptionServiceIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-07T02:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final LocalDate EXPIRY = LocalDate.of(2027, 1, 1);

    @Autowired private PartialDispensePrescriptionService service;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private MedicineRepository medicineRepository;
    @Autowired private MedicineBatchRepository medicineBatchRepository;

    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private EligibleStockSnapshotService eligibleStockSnapshotService;
    @MockitoBean private LowStockAlertTransitionService lowStockAlertTransitionService;
    @MockitoBean private PartialDispensePrescriptionResultMapper resultMapper;

    @BeforeEach
    void setUp() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(any(), any())).thenReturn(Map.of());
    }

    @Test
    void partialDispensePersistsPartiallyDispensedStatusAndReadsBack() {
        UUID medicineId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        medicineRepository.save(medicine(medicineId));
        medicineBatchRepository.save(batch(UUID.randomUUID(), medicineId, 12));
        PrescriptionItem item = item(prescriptionId, itemId, medicineId, 20, 0);
        prescriptionRepository.save(prescription(prescriptionId, PrescriptionStatus.PENDING_DISPENSE, item));

        service.dispense(new DispensePrescriptionItemsCommand(prescriptionId,
                List.of(new DispenseItemCommand(itemId, 12))));

        Prescription reloaded = prescriptionRepository.findById(prescriptionId).orElseThrow();
        assertEquals(PrescriptionStatus.PARTIALLY_DISPENSED, reloaded.getStatus());
        assertEquals(12, reloaded.getItems().get(0).getDispensedQuantity());
        assertEquals(8, reloaded.getItems().get(0).getRemainingQuantity());
    }

    @Test
    void completingRemainingQuantityPersistsDispensedStatusAndReadsBack() {
        UUID medicineId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        medicineRepository.save(medicine(medicineId));
        medicineBatchRepository.save(batch(UUID.randomUUID(), medicineId, 8));
        PrescriptionItem item = item(prescriptionId, itemId, medicineId, 20, 12);
        prescriptionRepository.save(prescription(prescriptionId, PrescriptionStatus.PARTIALLY_DISPENSED, item));

        service.dispense(new DispensePrescriptionItemsCommand(prescriptionId,
                List.of(new DispenseItemCommand(itemId, 8))));

        Prescription reloaded = prescriptionRepository.findById(prescriptionId).orElseThrow();
        assertEquals(PrescriptionStatus.DISPENSED, reloaded.getStatus());
        assertEquals(20, reloaded.getItems().get(0).getDispensedQuantity());
        assertEquals(0, reloaded.getItems().get(0).getRemainingQuantity());
    }

    private Medicine medicine(UUID medicineId) {
        return Medicine.restore(medicineId, code("MED", medicineId), "Paracetamol", "Paracetamol", "500 mg",
                DosageForm.TABLET, "vien", AdministrationRoute.ORAL, true, NOW, null, 100, 10);
    }

    private MedicineBatch batch(UUID batchId, UUID medicineId, int quantity) {
        return MedicineBatch.restore(batchId, medicineId, code("BATCH", batchId), EXPIRY, quantity,
                BatchStatus.ACTIVE, NOW, null);
    }

    private PrescriptionItem item(UUID prescriptionId, UUID itemId, UUID medicineId, int prescribed, int dispensed) {
        return PrescriptionItem.restore(
                itemId, prescriptionId, medicineId, "Paracetamol", "Paracetamol",
                "500 mg", "vien", "1 vien", 2, AdministrationRoute.ORAL, 5,
                prescribed, dispensed, null, NOW.minusSeconds(600), null);
    }

    private Prescription prescription(UUID prescriptionId, PrescriptionStatus status, PrescriptionItem item) {
        return Prescription.restore(
                prescriptionId, code("RX", prescriptionId), UUID.randomUUID(), status, "note",
                null, UUID.randomUUID(), NOW.minusSeconds(600), null, null,
                InterconnectionStatus.NOT_SENT, null, null, null, List.of(item));
    }

    private static String code(String prefix, UUID id) {
        return prefix + "-" + id.toString().substring(0, 8);
    }
}
