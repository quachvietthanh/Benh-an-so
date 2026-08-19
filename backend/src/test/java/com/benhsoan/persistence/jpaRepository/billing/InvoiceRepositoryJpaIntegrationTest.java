package com.benhsoan.persistence.jpaRepository.billing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.entity.billing.InvoiceEntity;
import com.benhsoan.persistence.entity.patient.PatientEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientRepository;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;

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

    @Autowired
    private JpaPatientRepository patientRepository;

    @Autowired
    private JpaVisitRepository visitRepository;

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

    @Test
    void returnsUncancelledVisitsWithoutPaymentAsPayable() {
        UUID patientId = UUID.randomUUID();
        patientRepository.saveAndFlush(PatientEntity.builder()
                .id(patientId)
                .patientCode("BN-" + UUID.randomUUID().toString().substring(0, 8))
                .fullName("Test Patient")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.OTHER)
                .active(true)
                .createdAt(Instant.parse("2026-08-18T04:00:00Z"))
                .updatedAt(Instant.parse("2026-08-18T04:00:00Z"))
                .createdBy(UUID.randomUUID())
                .build());
        UUID waitingVisitId = UUID.randomUUID();
        visitRepository.saveAndFlush(visit(waitingVisitId, patientId, VisitStatus.WAITING));
        UUID cancelledVisitId = UUID.randomUUID();
        visitRepository.saveAndFlush(visit(cancelledVisitId, patientId, VisitStatus.CANCELLED));

        var payable = repository.findPayableEncounters(PageRequest.of(0, 20));

        assertTrue(payable.stream().anyMatch(item -> waitingVisitId.equals(item.getVisitId())));
        assertFalse(payable.stream().anyMatch(item -> cancelledVisitId.equals(item.getVisitId())));
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

    private VisitEntity visit(UUID visitId, UUID patientId, VisitStatus status) {
        Instant now = Instant.parse("2026-08-18T04:00:00Z");
        return VisitEntity.builder()
                .id(visitId)
                .visitCode("VIS-" + UUID.randomUUID().toString().substring(0, 8))
                .patientId(patientId)
                .doctorId(UUID.randomUUID())
                .visitType(VisitType.WALK_IN)
                .status(status)
                .visitAt(now)
                .reason("Test visit")
                .createdBy(UUID.randomUUID())
                .createdAt(now)
                .build();
    }
}
