package com.benhsoan.persistence.jpaRepository.auth;

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
 * QTN-04 anti-race foundation. Both {@code PatientBookAppointmentService} (portal) and
 * {@code CreateAppointmentService} (reception) acquire a pessimistic write lock on the
 * doctor's {@code users} row before checking slot overlap and inserting, guaranteeing
 * exactly one booking wins for the same doctor/slot. This proves the MySQL/Flyway
 * deployment provides that serialization.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PatientAppointmentBookingConcurrencyIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("appointment_booking_concurrency_test")
            .withUsername("appointment_test")
            .withPassword("appointment_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private JpaUserRepository userRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void concurrentBookingsSerializeOnDoctorUserPessimisticLock() throws Exception {
        var doctorId = userRepository.findAll().getFirst().getId();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CountDownLatch firstLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        CountDownLatch secondLockAcquired = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> transaction.executeWithoutResult(status -> {
                userRepository.findByIdForUpdate(doctorId).orElseThrow();
                firstLockAcquired.countDown();
                await(releaseFirstTransaction);
            }));
            assertTrue(firstLockAcquired.await(5, TimeUnit.SECONDS));

            Future<?> second = executor.submit(() -> transaction.executeWithoutResult(status -> {
                userRepository.findByIdForUpdate(doctorId).orElseThrow();
                secondLockAcquired.countDown();
            }));

            assertFalse(secondLockAcquired.await(300, TimeUnit.MILLISECONDS),
                    "the second booking must wait until the first transaction releases the doctor lock");
            releaseFirstTransaction.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertTrue(secondLockAcquired.await(1, TimeUnit.SECONDS));
        } finally {
            releaseFirstTransaction.countDown();
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
