package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.result.CurrentShiftSummaryResult;
import com.benhsoan.port.inbound.billing.GetCurrentShiftSummaryUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCurrentShiftSummaryService implements GetCurrentShiftSummaryUseCase {

    private static final List<PaymentStatus> ELIGIBLE_STATUSES = List.of(
            PaymentStatus.RECORDED,
            PaymentStatus.SUCCESS,
            PaymentStatus.REFUNDED
    );

    private final PaymentRepository paymentRepository;
    private final CurrentUserPort currentUserPort;
    private final UserRepository userRepository;
    private final ClockPort clockPort;

    @Override
    public CurrentShiftSummaryResult getCurrentSummary() {
        UUID cashierId = currentUserPort.getCurrentUserId();
        String cashierName = userRepository.findById(cashierId)
                .map(User::getFullName)
                .orElse(cashierId.toString());

        List<Payment> unsettled = paymentRepository.findUnsettledByCashier(cashierId, ELIGIBLE_STATUSES);

        if (unsettled.isEmpty()) {
            return new CurrentShiftSummaryResult(
                    cashierId,
                    cashierName,
                    null,
                    clockPort.now(),
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    List.of()
            );
        }

        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal transfer = BigDecimal.ZERO;
        BigDecimal card = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (Payment payment : unsettled) {
            BigDecimal amount = payment.getAmountPaid();
            boolean isRefunded = payment.getStatus() == PaymentStatus.REFUNDED;
            BigDecimal effectiveAmount = isRefunded ? amount.negate() : amount;

            total = total.add(effectiveAmount);
            if (payment.getPaymentMethod() == PaymentMethod.CASH) {
                cash = cash.add(effectiveAmount);
            } else if (payment.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
                transfer = transfer.add(effectiveAmount);
            } else if (payment.getPaymentMethod() == PaymentMethod.CARD) {
                card = card.add(effectiveAmount);
            } else {
                other = other.add(effectiveAmount);
            }
        }

        Instant startTime = unsettled.get(0).getPaidAt();
        Instant endTime = clockPort.now();
        List<UUID> paymentIds = unsettled.stream().map(Payment::getId).toList();

        return new CurrentShiftSummaryResult(
                cashierId,
                cashierName,
                startTime,
                endTime,
                unsettled.size(),
                cash,
                transfer,
                card,
                other,
                total,
                paymentIds
        );
    }
}
