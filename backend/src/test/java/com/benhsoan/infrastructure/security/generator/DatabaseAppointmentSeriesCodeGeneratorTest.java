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
class DatabaseAppointmentSeriesCodeGeneratorTest {

    @Mock
    private AppointmentCodeSequenceRepository sequenceRepository;

    @Test
    void generatesFirstSeriesCodeWhenSequenceIsOne() {
        when(sequenceRepository.reserveNextValue("SER")).thenReturn(1L);

        assertEquals("SER000001", new DatabaseAppointmentSeriesCodeGenerator(sequenceRepository).generate());
    }

    @Test
    void generatesNextSeriesCodeIncrementally() {
        when(sequenceRepository.reserveNextValue("SER")).thenReturn(25L);

        assertEquals("SER000025", new DatabaseAppointmentSeriesCodeGenerator(sequenceRepository).generate());
    }

    @Test
    void generatesUniqueSeriesCodesUnderConcurrentExecution() throws Exception {
        AtomicLong counter = new AtomicLong(0);
        when(sequenceRepository.reserveNextValue(eq("SER")))
                .thenAnswer(inv -> counter.incrementAndGet());

        DatabaseAppointmentSeriesCodeGenerator generator = new DatabaseAppointmentSeriesCodeGenerator(sequenceRepository);

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
                assertTrue(generatedCodes.contains(String.format("SER%06d", i)));
            }
        }
    }

    private Callable<String> generateAfterStart(
            DatabaseAppointmentSeriesCodeGenerator generator,
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
