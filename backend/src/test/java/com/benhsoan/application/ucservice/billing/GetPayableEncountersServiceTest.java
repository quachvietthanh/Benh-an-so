package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.port.dto.command.billing.PayableEncounterQuery;
import com.benhsoan.port.dto.result.PayableEncounterResult;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.billing.PayableEncounterSummary;
import com.benhsoan.port.outbound.time.ClockPort;

class GetPayableEncountersServiceTest {

    private static final ZoneId BILLING_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private InvoiceRepository invoiceRepository;
    private PayableEncounterResultMapper resultMapper;
    private ClinicalServiceFeeCalculator calculator;
    private ClockPort clockPort;
    private GetPayableEncountersService service;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        resultMapper = new PayableEncounterResultMapper();
        calculator = mock(ClinicalServiceFeeCalculator.class);
        clockPort = mock(ClockPort.class);
        service = new GetPayableEncountersService(invoiceRepository, resultMapper, calculator, clockPort);
    }

    @Test
    void returnsPayableEncountersWithCalculatedFeesAndDateFiltering() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Instant completedAt = Instant.parse("2026-09-21T02:30:00Z");
        Instant now = Instant.parse("2026-09-21T03:00:00Z");
        when(clockPort.now()).thenReturn(now);

        PayableEncounterSummary summary = new PayableEncounterSummary(
                visitId,
                "VIS-001",
                patientId,
                "PAT-001",
                "Nguyen Van A",
                "Sot cao",
                completedAt,
                true,
                false
        );

        LocalDate filterDate = LocalDate.of(2026, 9, 21);
        Instant expectedFrom = filterDate.atStartOfDay(BILLING_ZONE).toInstant();
        Instant expectedTo = filterDate.plusDays(1).atStartOfDay(BILLING_ZONE).toInstant();
        String search = "Nguyen";
        PageRequest pageable = PageRequest.of(0, 10);

        when(invoiceRepository.findPayableEncounters(expectedFrom, expectedTo, search, pageable))
                .thenReturn(new PageImpl<>(List.of(summary), pageable, 1));

        when(calculator.calculateBatch(List.of(visitId), now))
                .thenReturn(Map.of(visitId, new BigDecimal("50000")));

        Page<PayableEncounterResult> result = service.get(new PayableEncounterQuery(filterDate, search, pageable));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        PayableEncounterResult item = result.getContent().getFirst();
        assertEquals(visitId, item.visitId());
        assertEquals("VIS-001", item.visitCode());
        assertEquals("PAT-001", item.patientCode());
        assertEquals("Nguyen Van A", item.patientName());
        assertEquals(new BigDecimal("100000"), item.examFee());
        assertEquals(BigDecimal.ZERO, item.medicineFee());
        assertEquals(new BigDecimal("50000"), item.serviceFee());
        assertEquals(new BigDecimal("150000"), item.totalEstimatedAmount());
        assertEquals(true, item.hasPrescription());
        assertEquals(false, item.hasPendingDispense());
    }

    @Test
    void handlesClinicalFeeCalculationFailureGracefully() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Instant completedAt = Instant.parse("2026-09-21T02:30:00Z");
        Instant now = Instant.parse("2026-09-21T03:00:00Z");
        when(clockPort.now()).thenReturn(now);

        PayableEncounterSummary summary = new PayableEncounterSummary(
                visitId,
                "VIS-002",
                patientId,
                "PAT-002",
                "Tran Van B",
                "Dau dau",
                completedAt,
                true,
                true
        );

        PageRequest pageable = PageRequest.of(0, 10);
        when(invoiceRepository.findPayableEncounters(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(summary), pageable, 1));

        when(calculator.calculateBatch(List.of(visitId), now))
                .thenReturn(Map.of(visitId, BigDecimal.ZERO));

        Page<PayableEncounterResult> result = service.get(new PayableEncounterQuery(null, null, pageable));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        PayableEncounterResult item = result.getContent().getFirst();
        assertEquals(new BigDecimal("100000"), item.examFee());
        assertEquals(BigDecimal.ZERO, item.medicineFee());
        assertEquals(BigDecimal.ZERO, item.serviceFee());
        assertEquals(new BigDecimal("100000"), item.totalEstimatedAmount());
        assertEquals(true, item.hasPrescription());
        assertEquals(true, item.hasPendingDispense());
    }
}
