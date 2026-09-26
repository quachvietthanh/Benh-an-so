package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.billing.GetPaymentQuoteCommand;
import com.benhsoan.port.dto.result.PaymentQuoteResult;
import com.benhsoan.port.dto.result.PaymentServiceFeeQuoteResult;
import com.benhsoan.port.inbound.billing.GetPaymentQuoteUseCase;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPaymentQuoteService implements GetPaymentQuoteUseCase {

    private final VisitRepository visitRepository;
    private final ClinicalServiceFeeCalculator clinicalServiceFeeCalculator;
    private final ClockPort clockPort;

    @Override
    public PaymentQuoteResult quote(GetPaymentQuoteCommand command) {
        if (command == null || command.visitId() == null) {
            throw new ValidationException("Visit id is required.");
        }
        BigDecimal examFee = requireNonNegative(command.examFee(), "Exam fee is required.");
        BigDecimal medicineFee = requireNonNegative(command.medicineFee(), "Medicine fee is required.");
        visitRepository.findById(command.visitId())
                .orElseThrow(() -> new VisitNotFoundException(command.visitId()));

        Instant now = clockPort.now();
        List<ClinicalServiceCharge> charges = clinicalServiceFeeCalculator.calculate(command.visitId(), now);
        BigDecimal serviceFee = clinicalServiceFeeCalculator.total(charges);
        return new PaymentQuoteResult(
                command.visitId(),
                examFee,
                medicineFee,
                serviceFee,
                examFee.add(medicineFee).add(serviceFee),
                charges.stream()
                        .map(charge -> new PaymentServiceFeeQuoteResult(
                                charge.clinicalOrderItemId(),
                                charge.serviceName(),
                                charge.price()
                        ))
                        .toList(),
                now
        );
    }

    private BigDecimal requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(message);
        }
        return value;
    }
}
