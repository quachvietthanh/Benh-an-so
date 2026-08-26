package com.benhsoan.persistence.jpaRepository.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

/**
 * ApplyMedicalRecordTemplateService obtains this lock before evaluating and writing
 * the selected template. This test proves the MySQL/Flyway deployment provides the
 * serialization that the service relies on.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MedicalRecordTemplateApplyConcurrencyIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("medical_record_template_concurrency_test")
            .withUsername("template_test")
            .withPassword("template_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private JpaMedicalRecordRepository medicalRecordRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void concurrentTemplateApplyRequestsSerializeOnMedicalRecordPessimisticLock() throws Exception {
        var recordId = medicalRecordRepository.findAll().getFirst().getId();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CountDownLatch firstLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        CountDownLatch secondLockAcquired = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> transaction.executeWithoutResult(status -> {
                medicalRecordRepository.findByIdForUpdate(recordId).orElseThrow();
                firstLockAcquired.countDown();
                await(releaseFirstTransaction);
            }));
            assertTrue(firstLockAcquired.await(5, TimeUnit.SECONDS));

            Future<?> second = executor.submit(() -> transaction.executeWithoutResult(status -> {
                medicalRecordRepository.findByIdForUpdate(recordId).orElseThrow();
                secondLockAcquired.countDown();
            }));

            assertFalse(secondLockAcquired.await(300, TimeUnit.MILLISECONDS),
                    "the second apply must wait until the first transaction releases the record lock");
            releaseFirstTransaction.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertTrue(secondLockAcquired.await(1, TimeUnit.SECONDS));
        } finally {
            releaseFirstTransaction.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentUpdateAndTemplateApplySerializeOnPessimisticLock() throws Exception {
        var recordId = medicalRecordRepository.findAll().getFirst().getId();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CountDownLatch updateLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseUpdateTransaction = new CountDownLatch(1);
        CountDownLatch applyLockAcquired = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> updateTx = executor.submit(() -> transaction.executeWithoutResult(status -> {
                var record = medicalRecordRepository.findByIdForUpdate(recordId).orElseThrow();
                record.setChiefComplaint("Concurrent update complaint");
                medicalRecordRepository.save(record);
                updateLockAcquired.countDown();
                await(releaseUpdateTransaction);
            }));
            assertTrue(updateLockAcquired.await(5, TimeUnit.SECONDS));

            Future<?> applyTx = executor.submit(() -> transaction.executeWithoutResult(status -> {
                medicalRecordRepository.findByIdForUpdate(recordId).orElseThrow();
                applyLockAcquired.countDown();
            }));

            assertFalse(applyLockAcquired.await(300, TimeUnit.MILLISECONDS),
                    "Apply must wait until the update transaction releases its pessimistic lock");
            releaseUpdateTransaction.countDown();
            updateTx.get(5, TimeUnit.SECONDS);
            applyTx.get(5, TimeUnit.SECONDS);
            assertTrue(applyLockAcquired.await(1, TimeUnit.SECONDS));
        } finally {
            releaseUpdateTransaction.countDown();
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
