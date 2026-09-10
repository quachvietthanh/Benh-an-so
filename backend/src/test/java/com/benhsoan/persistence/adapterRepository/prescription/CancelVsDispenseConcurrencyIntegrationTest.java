package com.benhsoan.persistence.adapterRepository.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import com.benhsoan.domain.prescription.exception.PrescriptionAlreadyDispensedException;
import com.benhsoan.domain.prescription.exception.PrescriptionInvalidStatusException;
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
class CancelVsDispenseConcurrencyIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    @Autowired private PrescriptionRepositoryAdapter prescriptionRepository;
    @Autowired private JpaPrescriptionItemRepository prescriptionItemRepository;
    @Autowired private JpaPrescriptionRepository jpaPrescriptionRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearPrescriptions() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            prescriptionItemRepository.deleteAll();
            jpaPrescriptionRepository.deleteAll();
        });
    }

    @Test
    @DisplayName("When Cancel wins the lock, concurrent Dispense is rejected with PrescriptionInvalidStatusException")
    void cancelWinsLock_dispenseIsRejected() throws Exception {
        UUID prescriptionId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID pharmacistId = UUID.randomUUID();

        seedPendingPrescription(prescriptionId, doctorId);

        CountDownLatch cancelLocked = new CountDownLatch(1);
        CountDownLatch dispenseStarted = new CountDownLatch(1);
        AtomicReference<Exception> dispenseException = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> cancelFuture = executor.submit(() -> {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    Prescription prescription = prescriptionRepository.findByIdForUpdate(prescriptionId).orElseThrow();
                    cancelLocked.countDown();
                    try {
                        dispenseStarted.await(5, TimeUnit.SECONDS);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    prescription.cancel("Bệnh nhân hủy lượt khám", doctorId, NOW.plusSeconds(30));
                    prescriptionRepository.save(prescription);
                });
            });

            Future<?> dispenseFuture = executor.submit(() -> {
                try {
                    cancelLocked.await(5, TimeUnit.SECONDS);
                    dispenseStarted.countDown();
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        Prescription prescription = prescriptionRepository.findByIdForUpdate(prescriptionId).orElseThrow();
                        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
                            throw new PrescriptionInvalidStatusException("Cancelled prescriptions cannot be dispensed.");
                        }
                        prescription.markDispensed(pharmacistId, NOW.plusSeconds(60));
                        prescriptionRepository.save(prescription);
                    });
                } catch (Exception ex) {
                    dispenseException.set(ex);
                }
            });

            cancelFuture.get(10, TimeUnit.SECONDS);
            dispenseFuture.get(10, TimeUnit.SECONDS);

            assertNotNull(dispenseException.get());
            assertInstanceOf(PrescriptionInvalidStatusException.class, dispenseException.get());
            assertEquals("Cancelled prescriptions cannot be dispensed.", dispenseException.get().getMessage());

            Prescription finalPrescription = prescriptionRepository.findById(prescriptionId).orElseThrow();
            assertEquals(PrescriptionStatus.CANCELLED, finalPrescription.getStatus());
            assertEquals("Bệnh nhân hủy lượt khám", finalPrescription.getCancelReason());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("When Dispense wins the lock, concurrent Cancel is rejected with PrescriptionAlreadyDispensedException")
    void dispenseWinsLock_cancelIsRejected() throws Exception {
        UUID prescriptionId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID pharmacistId = UUID.randomUUID();

        seedPendingPrescription(prescriptionId, doctorId);

        CountDownLatch dispenseLocked = new CountDownLatch(1);
        CountDownLatch cancelStarted = new CountDownLatch(1);
        AtomicReference<Exception> cancelException = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> dispenseFuture = executor.submit(() -> {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    Prescription prescription = prescriptionRepository.findByIdForUpdate(prescriptionId).orElseThrow();
                    dispenseLocked.countDown();
                    try {
                        cancelStarted.await(5, TimeUnit.SECONDS);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    prescription.markDispensed(pharmacistId, NOW.plusSeconds(30));
                    prescriptionRepository.save(prescription);
                });
            });

            Future<?> cancelFuture = executor.submit(() -> {
                try {
                    dispenseLocked.await(5, TimeUnit.SECONDS);
                    cancelStarted.countDown();
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        Prescription prescription = prescriptionRepository.findByIdForUpdate(prescriptionId).orElseThrow();
                        prescription.cancel("Bác sĩ muốn hủy", doctorId, NOW.plusSeconds(60));
                        prescriptionRepository.save(prescription);
                    });
                } catch (Exception ex) {
                    cancelException.set(ex);
                }
            });

            dispenseFuture.get(10, TimeUnit.SECONDS);
            cancelFuture.get(10, TimeUnit.SECONDS);

            assertNotNull(cancelException.get());
            assertInstanceOf(PrescriptionAlreadyDispensedException.class, cancelException.get());

            Prescription finalPrescription = prescriptionRepository.findById(prescriptionId).orElseThrow();
            assertEquals(PrescriptionStatus.DISPENSED, finalPrescription.getStatus());
        } finally {
            executor.shutdownNow();
        }
    }

    private void seedPendingPrescription(UUID prescriptionId, UUID doctorId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Prescription prescription = Prescription.restore(
                    prescriptionId, "RX-RACE-001", UUID.randomUUID(), PrescriptionStatus.PENDING_DISPENSE,
                    "Take after meals", doctorId, NOW, null, null,
                    List.of(PrescriptionItem.restore(
                            UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Paracetamol", "Paracetamol",
                            "500 mg", "tablet", "1 tablet", 2, AdministrationRoute.ORAL, 2, 4, null, NOW, null
                    ))
            );
            prescriptionRepository.save(prescription);
        });
    }
}
