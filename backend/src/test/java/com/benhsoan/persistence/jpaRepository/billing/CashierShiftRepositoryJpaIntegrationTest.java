package com.benhsoan.persistence.jpaRepository.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.benhsoan.domain.billing.enums.CashierShiftStatus;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.persistence.entity.billing.CashierShiftEntity;
import com.benhsoan.persistence.entity.billing.PaymentEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("CashierShiftRepository and Payment JPA Integration Tests")
class CashierShiftRepositoryJpaIntegrationTest {

    @Autowired
    private JpaCashierShiftRepository cashierShiftRepository;

    @Autowired
    private JpaPaymentRepository paymentRepository;

    @Test
    void savesAndRetrievesCashierShift() {
        UUID shiftId = UUID.randomUUID();
        UUID cashierId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T10:00:00Z");

        CashierShiftEntity shift = CashierShiftEntity.builder()
                .id(shiftId)
                .shiftCode("CS2609210001")
                .cashierId(cashierId)
                .startTime(now.minusSeconds(28800))
                .endTime(now)
                .totalTransactions(5)
                .totalSystemAmount(new BigDecimal("2500000.00"))
                .systemCashAmount(new BigDecimal("1500000.00"))
                .systemTransferAmount(new BigDecimal("1000000.00"))
                .systemCardAmount(BigDecimal.ZERO)
                .systemOtherAmount(BigDecimal.ZERO)
                .actualCashAmount(new BigDecimal("1450000.00"))
                .differenceAmount(new BigDecimal("-50000.00"))
                .status(CashierShiftStatus.CONFIRMED)
                .notes("Lệch 50k tiền thối")
                .confirmedBy(managerId)
                .confirmedAt(now.plusSeconds(300))
                .confirmationNotes("Quản lý đã duyệt chênh lệch")
                .createdAt(now)
                .build();

        cashierShiftRepository.saveAndFlush(shift);

        Optional<CashierShiftEntity> foundById = cashierShiftRepository.findById(shiftId);
        assertTrue(foundById.isPresent());
        assertEquals("CS2609210001", foundById.get().getShiftCode());
        assertEquals(0, new BigDecimal("2500000.00").compareTo(foundById.get().getTotalSystemAmount()));
        assertEquals(0, new BigDecimal("1450000.00").compareTo(foundById.get().getActualCashAmount()));
        assertEquals(0, new BigDecimal("-50000.00").compareTo(foundById.get().getDifferenceAmount()));
        assertEquals(CashierShiftStatus.CONFIRMED, foundById.get().getStatus());
        assertEquals(managerId, foundById.get().getConfirmedBy());

        Optional<CashierShiftEntity> foundByCode = cashierShiftRepository.findByShiftCode("CS2609210001");
        assertTrue(foundByCode.isPresent());
        assertEquals(shiftId, foundByCode.get().getId());
    }

    @Test
    void searchesShiftsWithFiltersAndPagination() {
        UUID cashier1 = UUID.randomUUID();
        UUID cashier2 = UUID.randomUUID();
        Instant day1 = Instant.parse("2026-09-20T10:00:00Z");
        Instant day2 = Instant.parse("2026-09-21T10:00:00Z");

        CashierShiftEntity shift1 = createShift("CS2609200001", cashier1, CashierShiftStatus.CONFIRMED, day1);
        CashierShiftEntity shift2 = createShift("CS2609210001", cashier1, CashierShiftStatus.PENDING_CONFIRMATION, day2);
        CashierShiftEntity shift3 = createShift("CS2609210002", cashier2, CashierShiftStatus.CONFIRMED, day2);

        cashierShiftRepository.saveAllAndFlush(List.of(shift1, shift2, shift3));

        // Filter by cashierId
        Page<CashierShiftEntity> byCashier = cashierShiftRepository.search(
                cashier1,
                null,
                null,
                null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        assertEquals(2, byCashier.getTotalElements());

        // Filter by status
        Page<CashierShiftEntity> byStatus = cashierShiftRepository.search(
                null,
                CashierShiftStatus.PENDING_CONFIRMATION,
                null,
                null,
                PageRequest.of(0, 10)
        );
        assertEquals(1, byStatus.getTotalElements());
        assertEquals("CS2609210001", byStatus.getContent().get(0).getShiftCode());

        // Filter by date range
        Page<CashierShiftEntity> byDate = cashierShiftRepository.search(
                null,
                null,
                Instant.parse("2026-09-21T00:00:00Z"),
                Instant.parse("2026-09-21T23:59:59Z"),
                PageRequest.of(0, 10)
        );
        assertEquals(2, byDate.getTotalElements());
    }

    @Test
    void findsUnsettledPaymentsAndAssociatesWithShift() {
        UUID cashierId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T08:00:00Z");

        // Payment 1: unsettled cash
        PaymentEntity p1 = savePayment(cashierId, new BigDecimal("100000"), PaymentMethod.CASH, PaymentStatus.RECORDED, null, now.minusSeconds(3600));
        // Payment 2: unsettled transfer
        PaymentEntity p2 = savePayment(cashierId, new BigDecimal("200000"), PaymentMethod.BANK_TRANSFER, PaymentStatus.SUCCESS, null, now.minusSeconds(1800));
        // Payment 3: already settled
        UUID previousShiftId = UUID.randomUUID();
        savePayment(cashierId, new BigDecimal("300000"), PaymentMethod.CASH, PaymentStatus.RECORDED, previousShiftId, now.minusSeconds(7200));
        // Payment 4: cancelled
        savePayment(cashierId, new BigDecimal("150000"), PaymentMethod.CASH, PaymentStatus.CANCELLED, null, now.minusSeconds(500));

        List<PaymentStatus> validStatuses = List.of(PaymentStatus.RECORDED, PaymentStatus.SUCCESS, PaymentStatus.REFUNDED);

        // Find unsettled payments
        List<PaymentEntity> unsettled = paymentRepository.findUnsettledByCashier(cashierId, validStatuses);
        assertEquals(2, unsettled.size());
        assertTrue(unsettled.stream().anyMatch(p -> p.getId().equals(p1.getId())));
        assertTrue(unsettled.stream().anyMatch(p -> p.getId().equals(p2.getId())));

        // Settle payments with new shift
        UUID newShiftId = UUID.randomUUID();
        unsettled.forEach(p -> p.setCashierShiftId(newShiftId));
        paymentRepository.saveAllAndFlush(unsettled);

        // Verify no unsettled payments remain
        List<PaymentEntity> remainingUnsettled = paymentRepository.findUnsettledByCashier(cashierId, validStatuses);
        assertTrue(remainingUnsettled.isEmpty());

        // Verify findByCashierShiftId returns newly settled payments
        List<PaymentEntity> shiftPayments = paymentRepository.findByCashierShiftId(newShiftId);
        assertEquals(2, shiftPayments.size());
    }

    private CashierShiftEntity createShift(String code, UUID cashierId, CashierShiftStatus status, Instant createdAt) {
        return CashierShiftEntity.builder()
                .id(UUID.randomUUID())
                .shiftCode(code)
                .cashierId(cashierId)
                .startTime(createdAt.minusSeconds(28800))
                .endTime(createdAt)
                .totalTransactions(1)
                .totalSystemAmount(new BigDecimal("100000"))
                .systemCashAmount(new BigDecimal("100000"))
                .systemTransferAmount(BigDecimal.ZERO)
                .systemCardAmount(BigDecimal.ZERO)
                .systemOtherAmount(BigDecimal.ZERO)
                .actualCashAmount(new BigDecimal("100000"))
                .differenceAmount(BigDecimal.ZERO)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    private PaymentEntity savePayment(
            UUID cashierId,
            BigDecimal amount,
            PaymentMethod method,
            PaymentStatus status,
            UUID shiftId,
            Instant paidAt
    ) {
        return paymentRepository.saveAndFlush(PaymentEntity.builder()
                .id(UUID.randomUUID())
                .visitId(UUID.randomUUID())
                .examFee(amount)
                .medicineFee(BigDecimal.ZERO)
                .serviceFee(BigDecimal.ZERO)
                .totalAmount(amount)
                .amountPaid(amount)
                .paymentMethod(method)
                .status(status)
                .collectedBy(cashierId)
                .cashierShiftId(shiftId)
                .paidAt(paidAt)
                .createdAt(paidAt)
                .build());
    }
}
