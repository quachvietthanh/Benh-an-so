package com.benhsoan.persistence.adapterRepository.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionRepository;
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
class PrescriptionConcurrencyIntegrationTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-05T02:00:00Z");

    @Autowired private PrescriptionRepositoryAdapter prescriptionRepository;
    @Autowired private JpaPrescriptionItemRepository prescriptionItemRepository;
    @Autowired private JpaPrescriptionRepository jpaPrescriptionRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearPrescriptionData() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            prescriptionItemRepository.deleteAll();
            jpaPrescriptionRepository.deleteAll();
        });
    }

    @Test
    @DisplayName("Pessimistic lock on findByIdForUpdate serializes concurrent cancellation and read")
    void concurrentCancelAndRead_serializesOnPessimisticLock() throws Exception {
        UUID prescriptionId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Prescription prescription = Prescription.restore(
                    prescriptionId, "RX-CONCURRENT-01", UUID.randomUUID(), PrescriptionStatus.PENDING_DISPENSE,
                    "Ghi chú", doctorId, CREATED_AT, null, null,
                    List.of(PrescriptionItem.restore(
                            UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Paracetamol", "Paracetamol",
                            "500 mg", "viên", "1 viên", 2, AdministrationRoute.ORAL, 2, 4, null, CREATED_AT, null
                    ))
            );
            prescriptionRepository.save(prescription);
        });

        CountDownLatch thread1Locked = new CountDownLatch(1);
        CountDownLatch thread2Started = new CountDownLatch(1);
        AtomicReference<PrescriptionStatus> thread2ObservedStatus = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> future1 = executor.submit(() -> {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    Prescription p = prescriptionRepository.findByIdForUpdate(prescriptionId).orElseThrow();
                    thread1Locked.countDown();
                    try {
                        thread2Started.await(5, TimeUnit.SECONDS);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    p.cancel("Hủy do đổi phác đồ", doctorId, CREATED_AT.plusSeconds(100));
                    prescriptionRepository.save(p);
                });
            });

            Future<?> future2 = executor.submit(() -> {
                try {
                    thread1Locked.await(5, TimeUnit.SECONDS);
                    thread2Started.countDown();
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        Prescription p = prescriptionRepository.findByIdForUpdate(prescriptionId).orElseThrow();
                        thread2ObservedStatus.set(p.getStatus());
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            future1.get(10, TimeUnit.SECONDS);
            future2.get(10, TimeUnit.SECONDS);

            assertEquals(PrescriptionStatus.CANCELLED, thread2ObservedStatus.get());

            Prescription finalPrescription = prescriptionRepository.findById(prescriptionId).orElseThrow();
            assertEquals(PrescriptionStatus.CANCELLED, finalPrescription.getStatus());
            assertEquals("Hủy do đổi phác đồ", finalPrescription.getCancelReason());
        } finally {
            executor.shutdownNow();
        }
    }
}
