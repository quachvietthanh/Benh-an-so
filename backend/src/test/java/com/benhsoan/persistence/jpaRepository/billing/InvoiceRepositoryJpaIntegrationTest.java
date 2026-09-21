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
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;
import com.benhsoan.persistence.entity.prescription.PrescriptionEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordRepository;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientRepository;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionRepository;
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

    @Autowired
    private JpaMedicalRecordRepository medicalRecordRepository;

    @Autowired
    private JpaPrescriptionRepository prescriptionRepository;

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
    void returnsCompletedVisitsWithoutPaymentAsPayable() {
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
        UUID completedVisitId = UUID.randomUUID();
        visitRepository.saveAndFlush(visit(completedVisitId, patientId, VisitStatus.COMPLETED));
        UUID waitingVisitId = UUID.randomUUID();
        visitRepository.saveAndFlush(visit(waitingVisitId, patientId, VisitStatus.WAITING));
        UUID cancelledVisitId = UUID.randomUUID();
        visitRepository.saveAndFlush(visit(cancelledVisitId, patientId, VisitStatus.CANCELLED));
        UUID earlyEndedVisitId = UUID.randomUUID();
        visitRepository.saveAndFlush(visit(earlyEndedVisitId, patientId, VisitStatus.EARLY_ENDED));

        var payable = repository.findPayableEncounters(PageRequest.of(0, 20));

        assertTrue(payable.stream().anyMatch(item -> completedVisitId.equals(item.getVisitId())));
        assertFalse(payable.stream().anyMatch(item -> waitingVisitId.equals(item.getVisitId())));
        assertFalse(payable.stream().anyMatch(item -> cancelledVisitId.equals(item.getVisitId())));
        assertFalse(payable.stream().anyMatch(item -> earlyEndedVisitId.equals(item.getVisitId())));
    }

    @Test
    void returnsPayableEncountersEvenWithPendingDispensePrescription() {
        UUID patientId = UUID.randomUUID();
        patientRepository.saveAndFlush(patient(patientId, "Le Thi Pending"));

        UUID visitId = UUID.randomUUID();
        visitRepository.saveAndFlush(visit(visitId, patientId, VisitStatus.COMPLETED));

        UUID medicalRecordId = UUID.randomUUID();
        medicalRecordRepository.saveAndFlush(MedicalRecordEntity.builder()
                .id(medicalRecordId)
                .visitId(visitId)
                .status(MedicalRecordStatus.SIGNED)
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.parse("2026-08-18T04:00:00Z"))
                .build());

        UUID prescriptionId = UUID.randomUUID();
        prescriptionRepository.saveAndFlush(PrescriptionEntity.builder()
                .id(prescriptionId)
                .prescriptionCode("RX-" + UUID.randomUUID().toString().substring(0, 8))
                .medicalRecordId(medicalRecordId)
                .status(PrescriptionStatus.PENDING_DISPENSE)
                .interconnectionStatus(InterconnectionStatus.NOT_SENT)
                .prescribedBy(UUID.randomUUID())
                .prescribedAt(Instant.parse("2026-08-18T04:00:00Z"))
                .build());

        var page = repository.findPayableEncounters(PageRequest.of(0, 20));

        var found = page.stream().filter(item -> visitId.equals(item.getVisitId())).findFirst();
        assertTrue(found.isPresent());
        assertTrue(Boolean.TRUE.equals(found.get().getHasPrescription()));
        assertTrue(Boolean.TRUE.equals(found.get().getHasPendingDispense()));
    }

    @Test
    void filtersPayableEncountersByDateAndSearchKeyword() {
        UUID patientId = UUID.randomUUID();
        patientRepository.saveAndFlush(PatientEntity.builder()
                .id(patientId)
                .patientCode("BN-UNIQUE01")
                .fullName("Nguyen Van Chon")
                .dateOfBirth(LocalDate.of(1992, 5, 5))
                .gender(Gender.MALE)
                .active(true)
                .createdAt(Instant.parse("2026-08-18T04:00:00Z"))
                .updatedAt(Instant.parse("2026-08-18T04:00:00Z"))
                .createdBy(UUID.randomUUID())
                .build());
        UUID visitId = UUID.randomUUID();
        visitRepository.saveAndFlush(visit(visitId, patientId, VisitStatus.COMPLETED));

        Instant from = Instant.parse("2026-08-18T00:00:00Z");
        Instant to = Instant.parse("2026-08-18T23:59:59Z");

        var matched = repository.findPayableEncounters(from, to, "Van Chon", PageRequest.of(0, 20));
        assertTrue(matched.stream().anyMatch(item -> visitId.equals(item.getVisitId())));

        var wrongDate = repository.findPayableEncounters(
                Instant.parse("2026-08-19T00:00:00Z"),
                Instant.parse("2026-08-19T23:59:59Z"),
                null,
                PageRequest.of(0, 20)
        );
        assertFalse(wrongDate.stream().anyMatch(item -> visitId.equals(item.getVisitId())));

        var wrongSearch = repository.findPayableEncounters(from, to, "KhongTonTai", PageRequest.of(0, 20));
        assertFalse(wrongSearch.stream().anyMatch(item -> visitId.equals(item.getVisitId())));
    }

    @Test
    void searchesInvoicesByPatientName() {
        UUID patientId = UUID.randomUUID();
        patientRepository.saveAndFlush(patient(patientId, "Nguyen Van A"));

        UUID visitId = UUID.randomUUID();
        visitRepository.saveAndFlush(visit(visitId, patientId, VisitStatus.COMPLETED));

        UUID invoiceId = UUID.randomUUID();
        repository.saveAndFlush(invoiceForVisit(invoiceId, "HD000050", visitId));

        var page = repository.search(null, null, null, "Nguyen", null, null, PageRequest.of(0, 20));

        assertTrue(page.getContent().stream().anyMatch(inv -> invoiceId.equals(inv.getId())));
    }

    @Test
    void searchesByPatientNameAndDateRange() {
        UUID patientId = UUID.randomUUID();
        patientRepository.saveAndFlush(patient(patientId, "Tran Thi B"));

        UUID visitId = UUID.randomUUID();
        visitRepository.saveAndFlush(visit(visitId, patientId, VisitStatus.COMPLETED));

        UUID invoiceId = UUID.randomUUID();
        repository.saveAndFlush(invoiceForVisit(invoiceId, "HD000051", visitId));

        var inRange = repository.search(
                null, null, null, "Tran",
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-31T00:00:00Z"),
                PageRequest.of(0, 20));
        assertTrue(inRange.getContent().stream().anyMatch(inv -> invoiceId.equals(inv.getId())));

        var outOfRange = repository.search(
                null, null, null, "Tran",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-30T00:00:00Z"),
                PageRequest.of(0, 20));
        assertFalse(outOfRange.getContent().stream().anyMatch(inv -> invoiceId.equals(inv.getId())));
    }

    @Test
    void returnsEmptyWhenPatientNameDoesNotMatch() {
        UUID patientId = UUID.randomUUID();
        patientRepository.saveAndFlush(patient(patientId, "Nguyen Van A"));

        UUID visitId = UUID.randomUUID();
        visitRepository.saveAndFlush(visit(visitId, patientId, VisitStatus.COMPLETED));
        repository.saveAndFlush(invoiceForVisit(UUID.randomUUID(), "HD000052", visitId));

        var page = repository.search(null, null, null, "Tran", null, null, PageRequest.of(0, 20));

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    void existingInvoiceCodeSearchStillWorks() {
        UUID patientId = UUID.randomUUID();
        patientRepository.saveAndFlush(patient(patientId, "Nguyen Van A"));

        UUID visitId = UUID.randomUUID();
        visitRepository.saveAndFlush(visit(visitId, patientId, VisitStatus.COMPLETED));

        UUID invoiceId = UUID.randomUUID();
        repository.saveAndFlush(invoiceForVisit(invoiceId, "HD000053", visitId));

        var page = repository.search("HD000053", null, null, null, null, null, PageRequest.of(0, 20));

        assertTrue(page.getContent().stream().anyMatch(inv -> invoiceId.equals(inv.getId())));
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
                .completedAt(status == VisitStatus.COMPLETED ? now : null)
                .reason("Test visit")
                .createdBy(UUID.randomUUID())
                .createdAt(now)
                .build();
    }

    private PatientEntity patient(UUID patientId, String fullName) {
        Instant now = Instant.parse("2026-08-18T04:00:00Z");
        return PatientEntity.builder()
                .id(patientId)
                .patientCode("BN-" + UUID.randomUUID().toString().substring(0, 8))
                .fullName(fullName)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.OTHER)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(UUID.randomUUID())
                .build();
    }

    private InvoiceEntity invoiceForVisit(UUID id, String code, UUID visitId) {
        return InvoiceEntity.builder()
                .id(id)
                .invoiceCode(code)
                .visitId(visitId)
                .paymentId(null)
                .type(InvoiceType.ORIGINAL)
                .originalInvoiceId(null)
                .adjustmentReason(null)
                .totalAmount(new BigDecimal("250000"))
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.parse("2026-08-18T04:00:00Z"))
                .build();
    }
}
