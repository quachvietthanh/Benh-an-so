package com.benhsoan.persistence.jpaRepository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.persistence.entity.inventory.MedicineBatchEntity;
import com.benhsoan.persistence.jpaRepository.medicine.JpaMedicineRepository;

/**
 * Proves that MySQL InnoDB pessimistic write locking (SELECT ... FOR UPDATE)
 * serializes concurrent transactions on medicine_batches, preventing lost updates
 * and ensuring data consistency across adjustment and discard workflows (P2-2).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Inventory Adjustment & Discard MySQL Concurrency Integration Tests (P2-2)")
class InventoryAdjustmentConcurrencyIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("inventory_concurrency_test")
            .withUsername("inventory_test")
            .withPassword("inventory_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private JpaMedicineBatchRepository batchRepository;

    @Autowired
    private JpaMedicineRepository medicineRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Scenario 1: Concurrent adjustments serialize on pessimistic lock with zero lost updates")
    void concurrentAdjustmentsSerializeOnPessimisticLock() throws Exception {
        UUID medicineId = medicineRepository.findAll().getFirst().getId();
        UUID batchId = UUID.randomUUID();

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        // Seed test batch with initial quantity 100
        transaction.executeWithoutResult(status -> {
            MedicineBatchEntity entity = MedicineBatchEntity.builder()
                    .id(batchId)
                    .medicineId(medicineId)
                    .batchNumber("BATCH-CONC-01")
                    .expiryDate(LocalDate.now().plusYears(1))
                    .quantity(100)
                    .status(BatchStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();
            batchRepository.save(entity);
        });

        CountDownLatch firstLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        CountDownLatch secondLockAcquired = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Thread 1: Lock batch, reduce quantity 100 -> 80 (-20), hold lock
            Future<?> first = executor.submit(() -> transaction.executeWithoutResult(status -> {
                MedicineBatchEntity batch = batchRepository.findByIdForUpdate(batchId).orElseThrow();
                batch.setQuantity(80);
                batch.setUpdatedAt(Instant.now());
                batchRepository.save(batch);
                firstLockAcquired.countDown();
                await(releaseFirstTransaction);
            }));
            assertTrue(firstLockAcquired.await(5, TimeUnit.SECONDS));

            // Thread 2: Try to lock the same batch concurrently
            Future<?> second = executor.submit(() -> transaction.executeWithoutResult(status -> {
                MedicineBatchEntity batch = batchRepository.findByIdForUpdate(batchId).orElseThrow();
                secondLockAcquired.countDown();
                // Reads 80, reduces further 80 -> 60 (-20)
                batch.setQuantity(batch.getQuantity() - 20);
                batch.setUpdatedAt(Instant.now());
                batchRepository.save(batch);
            }));

            // Assert that second transaction is BLOCKED while first holds the pessimistic write lock
            assertFalse(secondLockAcquired.await(300, TimeUnit.MILLISECONDS),
                    "Second transaction must wait until first transaction commits its pessimistic lock");

            // Release first transaction
            releaseFirstTransaction.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertTrue(secondLockAcquired.await(1, TimeUnit.SECONDS));

            // Verify final state: quantity must be exactly 60 (no lost update)
            MedicineBatchEntity finalBatch = batchRepository.findById(batchId).orElseThrow();
            assertEquals(60, finalBatch.getQuantity());
            assertEquals(BatchStatus.ACTIVE, finalBatch.getStatus());
        } finally {
            releaseFirstTransaction.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Scenario 2: Concurrent adjustment and discard serialize safely without corrupting batch state")
    void concurrentAdjustmentAndDiscardSerializeSafely() throws Exception {
        UUID medicineId = medicineRepository.findAll().getFirst().getId();
        UUID batchId = UUID.randomUUID();

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        // Seed expired test batch with quantity 50
        transaction.executeWithoutResult(status -> {
            MedicineBatchEntity entity = MedicineBatchEntity.builder()
                    .id(batchId)
                    .medicineId(medicineId)
                    .batchNumber("BATCH-CONC-02")
                    .expiryDate(LocalDate.now().minusDays(10))
                    .quantity(50)
                    .status(BatchStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();
            batchRepository.save(entity);
        });

        CountDownLatch discardLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseDiscard = new CountDownLatch(1);
        CountDownLatch adjustLockAcquired = new CountDownLatch(1);
        AtomicBoolean adjustRejectedExpired = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Thread 1 (Discard): Sets quantity to 0, status to EXPIRED
            Future<?> discardTx = executor.submit(() -> transaction.executeWithoutResult(status -> {
                MedicineBatchEntity batch = batchRepository.findByIdForUpdate(batchId).orElseThrow();
                batch.setQuantity(0);
                batch.setStatus(BatchStatus.EXPIRED);
                batch.setUpdatedAt(Instant.now());
                batchRepository.save(batch);
                discardLockAcquired.countDown();
                await(releaseDiscard);
            }));
            assertTrue(discardLockAcquired.await(5, TimeUnit.SECONDS));

            // Thread 2 (Adjustment): Attempts to adjust the same batch
            Future<?> adjustTx = executor.submit(() -> transaction.executeWithoutResult(status -> {
                MedicineBatchEntity batch = batchRepository.findByIdForUpdate(batchId).orElseThrow();
                adjustLockAcquired.countDown();
                if (batch.getStatus() == BatchStatus.EXPIRED) {
                    // Domain rule: adjust cannot proceed on EXPIRED batch
                    adjustRejectedExpired.set(true);
                    return;
                }
                batch.setQuantity(40);
                batchRepository.save(batch);
            }));

            // Assert adjust thread is waiting for discard lock
            assertFalse(adjustLockAcquired.await(300, TimeUnit.MILLISECONDS),
                    "Adjustment must wait until discard releases pessimistic lock");

            releaseDiscard.countDown();
            discardTx.get(5, TimeUnit.SECONDS);
            adjustTx.get(5, TimeUnit.SECONDS);

            assertTrue(adjustLockAcquired.await(1, TimeUnit.SECONDS));
            assertTrue(adjustRejectedExpired.get(), "Adjustment should reject the batch once it sees EXPIRED status");

            MedicineBatchEntity finalBatch = batchRepository.findById(batchId).orElseThrow();
            assertEquals(0, finalBatch.getQuantity());
            assertEquals(BatchStatus.EXPIRED, finalBatch.getStatus());
        } finally {
            releaseDiscard.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Scenario 3: Concurrent discards execute only once and prevent duplicate stock deductions")
    void concurrentDiscardsExecuteOnlyOnce() throws Exception {
        UUID medicineId = medicineRepository.findAll().getFirst().getId();
        UUID batchId = UUID.randomUUID();

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        // Seed expired test batch with quantity 40
        transaction.executeWithoutResult(status -> {
            MedicineBatchEntity entity = MedicineBatchEntity.builder()
                    .id(batchId)
                    .medicineId(medicineId)
                    .batchNumber("BATCH-CONC-03")
                    .expiryDate(LocalDate.now().minusDays(5))
                    .quantity(40)
                    .status(BatchStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();
            batchRepository.save(entity);
        });

        CountDownLatch firstDiscardLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirstDiscard = new CountDownLatch(1);
        CountDownLatch secondDiscardLockAcquired = new CountDownLatch(1);
        AtomicBoolean secondDiscardSkippedAlreadyDiscarded = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Thread 1: Discards batch
            Future<?> firstDiscard = executor.submit(() -> transaction.executeWithoutResult(status -> {
                MedicineBatchEntity batch = batchRepository.findByIdForUpdate(batchId).orElseThrow();
                batch.setQuantity(0);
                batch.setStatus(BatchStatus.EXPIRED);
                batch.setUpdatedAt(Instant.now());
                batchRepository.save(batch);
                firstDiscardLockAcquired.countDown();
                await(releaseFirstDiscard);
            }));
            assertTrue(firstDiscardLockAcquired.await(5, TimeUnit.SECONDS));

            // Thread 2: Also attempts to discard the same batch
            Future<?> secondDiscard = executor.submit(() -> transaction.executeWithoutResult(status -> {
                MedicineBatchEntity batch = batchRepository.findByIdForUpdate(batchId).orElseThrow();
                secondDiscardLockAcquired.countDown();
                if (batch.getStatus() == BatchStatus.EXPIRED || batch.getQuantity() <= 0) {
                    // Domain rule: batch already discarded
                    secondDiscardSkippedAlreadyDiscarded.set(true);
                    return;
                }
                batch.setQuantity(0);
                batch.setStatus(BatchStatus.EXPIRED);
                batchRepository.save(batch);
            }));

            // Assert second discard is serialized behind the first
            assertFalse(secondDiscardLockAcquired.await(300, TimeUnit.MILLISECONDS),
                    "Second discard must wait until first discard finishes");

            releaseFirstDiscard.countDown();
            firstDiscard.get(5, TimeUnit.SECONDS);
            secondDiscard.get(5, TimeUnit.SECONDS);

            assertTrue(secondDiscardLockAcquired.await(1, TimeUnit.SECONDS));
            assertTrue(secondDiscardSkippedAlreadyDiscarded.get(),
                    "Second discard must observe the batch is already discarded and avoid duplicate processing");

            MedicineBatchEntity finalBatch = batchRepository.findById(batchId).orElseThrow();
            assertEquals(0, finalBatch.getQuantity());
            assertEquals(BatchStatus.EXPIRED, finalBatch.getStatus());
        } finally {
            releaseFirstDiscard.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for concurrent transaction release");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for concurrent transaction release", exception);
        }
    }
}
