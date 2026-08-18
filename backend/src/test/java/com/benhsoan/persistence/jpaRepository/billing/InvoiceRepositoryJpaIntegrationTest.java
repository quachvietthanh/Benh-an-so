package com.benhsoan.persistence.jpaRepository.billing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.persistence.entity.billing.InvoiceEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class InvoiceRepositoryJpaIntegrationTest {

    @Autowired
    private JpaInvoiceRepository repository;

    @Test
    void detectsAdjustmentLinkedToOriginalInvoice() {
        UUID originalInvoiceId = UUID.randomUUID();
        repository.saveAndFlush(invoice(
                originalInvoiceId,
                "HD000030",
                InvoiceType.ORIGINAL,
                UUID.randomUUID(),
                null,
                null,
                new BigDecimal("250000")
        ));

        assertFalse(repository.existsByOriginalInvoiceId(originalInvoiceId));

        repository.saveAndFlush(invoice(
                UUID.randomUUID(),
                "HDDC000030",
                InvoiceType.ADJUSTMENT,
                null,
                originalInvoiceId,
                "Refund",
                new BigDecimal("-250000")
        ));

        assertTrue(repository.existsByOriginalInvoiceId(originalInvoiceId));
    }

    private InvoiceEntity invoice(
            UUID id,
            String code,
            InvoiceType type,
            UUID paymentId,
            UUID originalInvoiceId,
            String adjustmentReason,
            BigDecimal totalAmount
    ) {
        return InvoiceEntity.builder()
                .id(id)
                .invoiceCode(code)
                .visitId(UUID.randomUUID())
                .paymentId(paymentId)
                .type(type)
                .originalInvoiceId(originalInvoiceId)
                .adjustmentReason(adjustmentReason)
                .totalAmount(totalAmount)
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.parse("2026-08-18T04:00:00Z"))
                .build();
    }
}
