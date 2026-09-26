package com.benhsoan.infrastructure.security.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
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

import com.benhsoan.port.outbound.repository.appointment.AppointmentCodeSequenceRepository;

@ExtendWith(MockitoExtension.class)
class DatabaseAppointmentCodeGeneratorTest {

    @Mock
    private AppointmentCodeSequenceRepository sequenceRepository;

    @Test
    void generatesFirstAppointmentCodeWhenSequenceIsOne() {
        when(sequenceRepository.reserveNextValue("APT")).thenReturn(1L);

        assertEquals("APT000001", new DatabaseAppointmentCodeGenerator(sequenceRepository).generate());
    }

    @Test
    void incrementsTheHighestNumericSequenceAcrossLegacyAndNewPrefixes() {
        when(sequenceRepository.reserveNextValue("APT")).thenReturn(10L);

        assertEquals("APT000010", new DatabaseAppointmentCodeGenerator(sequenceRepository).generate());
    }

    @Test
    void keepsWorkingWhenContinuingFromLegacyCodeSequence() {
        when(sequenceRepository.reserveNextValue("APT")).thenReturn(124L);

        assertEquals("APT000124", new DatabaseAppointmentCodeGenerator(sequenceRepository).generate());
    }

    @Test
    void generatesBatchCodesSequentiallyWhenStartingFromBeginning() {
        when(sequenceRepository.reserveNextValues("APT", 3)).thenReturn(3L);

        var generator = new DatabaseAppointmentCodeGenerator(sequenceRepository);
        var codes = generator.generateBatch(3);

        assertEquals(3, codes.size());
        assertEquals("APT000001", codes.get(0));
        assertEquals("APT000002", codes.get(1));
        assertEquals("APT000003", codes.get(2));
    }

    @Test
    void generatesBatchCodesSequentiallyFromExistingSequence() {
        when(sequenceRepository.reserveNextValues("APT", 4)).thenReturn(13L);

        var generator = new DatabaseAppointmentCodeGenerator(sequenceRepository);
        var codes = generator.generateBatch(4);

        assertEquals(4, codes.size());
        assertEquals("APT000010", codes.get(0));
        assertEquals("APT000011", codes.get(1));
        assertEquals("APT000012", codes.get(2));
        assertEquals("APT000013", codes.get(3));
    }

    @Test
    void generatesEmptyListWhenBatchCountIsZeroOrNegative() {
        var generator = new DatabaseAppointmentCodeGenerator(sequenceRepository);
        assertEquals(0, generator.generateBatch(0).size());
        assertEquals(0, generator.generateBatch(-1).size());
    }

    @Test
    void generatesUniqueCodesUnderConcurrentExecution() throws Exception {
        AtomicLong counter = new AtomicLong(0);
        when(sequenceRepository.reserveNextValue(eq("APT")))
                .thenAnswer(inv -> counter.incrementAndGet());

        DatabaseAppointmentCodeGenerator generator = new DatabaseAppointmentCodeGenerator(sequenceRepository);

        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            List<Future<String>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(generateAfterStart(generator, ready, start)));
            }

            ready.await();
            start.countDown();

            Set<String> generatedCodes = new java.util.HashSet<>();
            for (var future : futures) {
                generatedCodes.add(future.get());
            }

            assertEquals(threads, generatedCodes.size());
            for (int i = 1; i <= threads; i++) {
                assertTrue(generatedCodes.contains(String.format("APT%06d", i)));
            }
        }
    }

    private Callable<String> generateAfterStart(
            DatabaseAppointmentCodeGenerator generator,
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
