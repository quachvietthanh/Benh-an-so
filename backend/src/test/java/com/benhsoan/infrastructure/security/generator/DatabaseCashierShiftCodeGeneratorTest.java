package com.benhsoan.infrastructure.security.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.port.outbound.repository.billing.CashierShiftCodeSequenceRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseCashierShiftCodeGenerator Concurrency & Formatting Unit Tests")
class DatabaseCashierShiftCodeGeneratorTest {

    @Mock
    private CashierShiftCodeSequenceRepository sequenceRepository;

    @Test
    @DisplayName("Tạo mã ca đầu tiên CS000001 khi sequence trả về 1")
    void generatesFirstShiftCodeWhenSequenceReturnsOne() {
        when(sequenceRepository.reserveNextValue("CS")).thenReturn(1L);

        DatabaseCashierShiftCodeGenerator generator = new DatabaseCashierShiftCodeGenerator(sequenceRepository);
        assertEquals("CS000001", generator.generate());
    }

    @Test
    @DisplayName("Tăng mã ca đúng định dạng CS000010 khi sequence trả về 10")
    void incrementsShiftCodeCorrectly() {
        when(sequenceRepository.reserveNextValue("CS")).thenReturn(10L);

        DatabaseCashierShiftCodeGenerator generator = new DatabaseCashierShiftCodeGenerator(sequenceRepository);
        assertEquals("CS000010", generator.generate());
    }

    @Test
    @DisplayName("P2: Sinh các mã ca khác nhau và nhất quán khi có nhiều yêu cầu đồng thời")
    void generatesDistinctCodesForConcurrentRequests() throws Exception {
        AtomicLong nextValue = new AtomicLong();
        when(sequenceRepository.reserveNextValue(anyString()))
                .thenAnswer(invocation -> nextValue.incrementAndGet());

        DatabaseCashierShiftCodeGenerator generator = new DatabaseCashierShiftCodeGenerator(sequenceRepository);
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            List<Future<String>> codes = List.of(
                    executor.submit(generateAfterStart(generator, ready, start)),
                    executor.submit(generateAfterStart(generator, ready, start)),
                    executor.submit(generateAfterStart(generator, ready, start)),
                    executor.submit(generateAfterStart(generator, ready, start))
            );
            ready.await();
            start.countDown();

            Set<String> generatedSet = Set.of(
                    codes.get(0).get(),
                    codes.get(1).get(),
                    codes.get(2).get(),
                    codes.get(3).get()
            );

            assertEquals(4, generatedSet.size());
            assertTrue(generatedSet.contains("CS000001"));
            assertTrue(generatedSet.contains("CS000002"));
            assertTrue(generatedSet.contains("CS000003"));
            assertTrue(generatedSet.contains("CS000004"));
        }
    }

    private Callable<String> generateAfterStart(
            DatabaseCashierShiftCodeGenerator generator,
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
