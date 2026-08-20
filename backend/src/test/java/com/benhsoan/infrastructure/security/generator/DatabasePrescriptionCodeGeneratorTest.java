package com.benhsoan.infrastructure.security.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.port.outbound.repository.prescription.PrescriptionCodeSequenceRepository;

@ExtendWith(MockitoExtension.class)
class DatabasePrescriptionCodeGeneratorTest {

    @Mock private PrescriptionCodeSequenceRepository sequenceRepository;

    @Test
    void generatesFirstPrescriptionCodeWhenNoPrescriptionExists() {
        when(sequenceRepository.reserveNextValue("RX")).thenReturn(1L);

        assertEquals("RX000001", new DatabasePrescriptionCodeGenerator(sequenceRepository).generate());
    }

    @Test
    void incrementsTheLatestPrescriptionCode() {
        when(sequenceRepository.reserveNextValue("RX")).thenReturn(10L);

        assertEquals("RX000010", new DatabasePrescriptionCodeGenerator(sequenceRepository).generate());
    }

    @Test
    void rejectsNonPositiveSequenceValue() {
        when(sequenceRepository.reserveNextValue("RX")).thenReturn(0L);

        assertThrows(
                IllegalStateException.class,
                () -> new DatabasePrescriptionCodeGenerator(sequenceRepository).generate()
        );
    }

    @Test
    void retainsTheRxNumericFormatForTheLargestSequenceValue() {
        when(sequenceRepository.reserveNextValue("RX")).thenReturn(Long.MAX_VALUE);

        String code = new DatabasePrescriptionCodeGenerator(sequenceRepository).generate();

        assertEquals("RX9223372036854775807", code);
        assertTrue(code.matches("RX\\d{6,}"));
        assertTrue(code.length() <= 30);
    }

    @Test
    void generatesDifferentCodesForConcurrentRequests() throws Exception {
        AtomicLong nextValue = new AtomicLong();
        when(sequenceRepository.reserveNextValue(anyString()))
                .thenAnswer(invocation -> nextValue.incrementAndGet());
        DatabasePrescriptionCodeGenerator generator = new DatabasePrescriptionCodeGenerator(
                sequenceRepository
        );
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<String>> codes = List.of(
                    executor.submit(generateAfterStart(generator, ready, start)),
                    executor.submit(generateAfterStart(generator, ready, start))
            );
            ready.await();
            start.countDown();

            assertEquals(
                    Set.of("RX000001", "RX000002"),
                    Set.of(codes.get(0).get(), codes.get(1).get())
            );
        }
    }

    private Callable<String> generateAfterStart(
            DatabasePrescriptionCodeGenerator generator,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return () -> {
            ready.countDown();
            start.await();
            return generator.generate();
        };
    }
}
