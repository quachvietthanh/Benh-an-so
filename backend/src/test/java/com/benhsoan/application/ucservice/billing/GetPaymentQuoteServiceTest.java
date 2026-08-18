package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.command.billing.GetPaymentQuoteCommand;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

class GetPaymentQuoteServiceTest {

    @Test
    void returnsCurrentClinicalServiceBreakdownAndTotal() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        ClinicalServiceFeeCalculator calculator = mock(ClinicalServiceFeeCalculator.class);
        ClockPort clockPort = mock(ClockPort.class);
        UUID visitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-18T08:00:00Z");
        List<ClinicalServiceCharge> charges = List.of(
                new ClinicalServiceCharge(itemId, "Blood test", new BigDecimal("95000"))
        );
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(mock(Visit.class)));
        when(clockPort.now()).thenReturn(now);
        when(calculator.calculate(visitId, now)).thenReturn(charges);
        when(calculator.total(charges)).thenReturn(new BigDecimal("95000"));

        var result = new GetPaymentQuoteService(visitRepository, calculator, clockPort).quote(
                new GetPaymentQuoteCommand(
                        visitId,
                        new BigDecimal("100000"),
                        new BigDecimal("150000")
                )
        );

        assertEquals(new BigDecimal("95000"), result.serviceFee());
        assertEquals(new BigDecimal("345000"), result.totalAmount());
        assertEquals(itemId, result.serviceFees().getFirst().clinicalOrderItemId());
        assertEquals("Blood test", result.serviceFees().getFirst().serviceName());
    }
}
