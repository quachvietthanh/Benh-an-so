package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.benhsoan.port.dto.command.billing.PaymentMethodItemCommand;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.exception.PaymentAlreadyExistsException;
import com.benhsoan.domain.billing.exception.PaymentAmountMismatchException;
import com.benhsoan.domain.billing.exception.PaymentNotAllowedException;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.billing.RecordPaymentCommand;
import com.benhsoan.port.dto.result.PaymentResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentServiceFeeRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.servicecatalog.ServicePriceRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class RecordPaymentServiceTest {

    @Test
    void recordsPaymentAndWritesAuditLog() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentServiceFeeRepository paymentServiceFeeRepository = mock(PaymentServiceFeeRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        ClinicalServiceFeeCalculator feeCalculator = mock(ClinicalServiceFeeCalculator.class);
        com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository discountRequestRepository =
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class);
        RecordPaymentService service = new RecordPaymentService(
                visitRepository,
                medicalRecordRepository,
                prescriptionRepository,
                paymentRepository,
                currentUserPort,
                clockPort,
                auditLogRepository,
                new PaymentResultMapper(),
                feeCalculator,
                paymentServiceFeeRepository,
                discountRequestRepository
        );

        UUID visitId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-12T01:00:00Z");
        Visit visit = completedVisit(visitId);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(clockPort.now()).thenReturn(now);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(feeCalculator.calculate(visitId, now)).thenReturn(List.of(
                new ClinicalServiceCharge(UUID.randomUUID(), "Blood test", new BigDecimal("95000"))
        ));
        when(feeCalculator.total(any())).thenReturn(new BigDecimal("95000"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResult result = service.record(new RecordPaymentCommand(
                visitId,
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                new BigDecimal("345000"),
                PaymentMethod.CASH
        ));

        assertEquals(visitId, result.visitId());
        assertEquals(new BigDecimal("95000"), result.serviceFee());
        assertEquals(new BigDecimal("345000"), result.totalAmount());
        assertEquals(actorId, result.collectedBy());
        verify(paymentRepository).save(any(Payment.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.benhsoan.domain.billing.PaymentServiceFee>> snapshotCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(paymentServiceFeeRepository).saveAll(snapshotCaptor.capture());
        assertEquals(new BigDecimal("95000"), snapshotCaptor.getValue().getFirst().getAmount());
        verify(auditLogRepository).save(any());
    }

    @Test
    void recordsPaymentWithMultipleMethodsAndWritesAuditLog() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentServiceFeeRepository paymentServiceFeeRepository = mock(PaymentServiceFeeRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        ClinicalServiceFeeCalculator feeCalculator = mock(ClinicalServiceFeeCalculator.class);
        RecordPaymentService service = new RecordPaymentService(
                visitRepository,
                medicalRecordRepository,
                prescriptionRepository,
                paymentRepository,
                currentUserPort,
                clockPort,
                auditLogRepository,
                new PaymentResultMapper(),
                feeCalculator,
                paymentServiceFeeRepository
        );

        UUID visitId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-12T01:00:00Z");
        Visit visit = completedVisit(visitId);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(clockPort.now()).thenReturn(now);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(feeCalculator.calculate(visitId, now)).thenReturn(List.of(
                new ClinicalServiceCharge(UUID.randomUUID(), "Blood test", new BigDecimal("95000"))
        ));
        when(feeCalculator.total(any())).thenReturn(new BigDecimal("95000"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<com.benhsoan.port.dto.command.billing.PaymentMethodItemCommand> methodCommands = List.of(
                new com.benhsoan.port.dto.command.billing.PaymentMethodItemCommand(PaymentMethod.CASH, new BigDecimal("100000"), null),
                new com.benhsoan.port.dto.command.billing.PaymentMethodItemCommand(PaymentMethod.BANK_TRANSFER, new BigDecimal("245000"), "TXN998877")
        );

        PaymentResult result = service.record(new RecordPaymentCommand(
                visitId,
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                new BigDecimal("345000"),
                PaymentMethod.MULTIPLE,
                methodCommands
        ));

        assertEquals(visitId, result.visitId());
        assertEquals(PaymentMethod.MULTIPLE, result.paymentMethod());
        assertEquals(2, result.paymentMethods().size());
        assertEquals(new BigDecimal("100000"), result.paymentMethods().get(0).amount());
        assertEquals(new BigDecimal("245000"), result.paymentMethods().get(1).amount());
        assertEquals("TXN998877", result.paymentMethods().get(1).referenceNumber());
        verify(paymentRepository).save(any(Payment.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void rejectsUnknownVisit() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        UUID visitId = UUID.randomUUID();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.empty());

        RecordPaymentService service = service(
                visitRepository,
                mock(MedicalRecordRepository.class),
                mock(PrescriptionRepository.class),
                mock(PaymentRepository.class),
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class)
        );

        assertThrows(
                VisitNotFoundException.class,
                () -> service.record(command(visitId, "100000", "150000", "250000"))
        );
    }

    @Test
    void recordsPaymentWhenVisitIsWaiting() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UUID visitId = UUID.randomUUID();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(waitingVisit(visitId)));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecordPaymentService service = service(
                visitRepository,
                mock(MedicalRecordRepository.class),
                mock(PrescriptionRepository.class),
                paymentRepository,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class)
        );

        PaymentResult result = service.record(
                command(visitId, "100000", "0", "100000")
        );

        assertEquals(visitId, result.visitId());
        assertEquals(new BigDecimal("100000"), result.totalAmount());
    }

    @Test
    void rejectsPaymentWhenVisitIsCancelled() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        UUID visitId = UUID.randomUUID();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(cancelledVisit(visitId)));

        RecordPaymentService service = service(
                visitRepository,
                mock(MedicalRecordRepository.class),
                mock(PrescriptionRepository.class),
                mock(PaymentRepository.class),
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class)
        );

        assertThrows(
                PaymentNotAllowedException.class,
                () -> service.record(command(visitId, "100000", "0", "100000"))
        );
    }

    @Test
    void rejectsPaymentWhenDispensingIsNotCompleted() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UUID visitId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();

        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(completedVisit(visitId)));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(lockedMedicalRecord(medicalRecordId, visitId)));
        when(prescriptionRepository.findByMedicalRecordId(medicalRecordId)).thenReturn(
                List.of(pendingPrescription(medicalRecordId))
        );

        RecordPaymentService service = service(
                visitRepository,
                medicalRecordRepository,
                prescriptionRepository,
                paymentRepository,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class)
        );

        assertThrows(
                PaymentNotAllowedException.class,
                () -> service.record(command(visitId, "100000", "150000", "250000"))
        );
    }

    @Test
    void rejectsDuplicatePayment() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UUID visitId = UUID.randomUUID();

        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(completedVisit(visitId)));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.of(existingPayment(visitId)));

        RecordPaymentService service = service(
                visitRepository,
                mock(MedicalRecordRepository.class),
                mock(PrescriptionRepository.class),
                paymentRepository,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class)
        );

        assertThrows(
                PaymentAlreadyExistsException.class,
                () -> service.record(command(visitId, "100000", "150000", "250000"))
        );
    }

    @Test
    void mapsDuplicatePaymentPersistenceConflictToBusinessConflict() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UUID visitId = UUID.randomUUID();

        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(completedVisit(visitId)));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key uk_payments_visit"));

        RecordPaymentService service = service(
                visitRepository,
                mock(MedicalRecordRepository.class),
                mock(PrescriptionRepository.class),
                paymentRepository,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class)
        );

        assertThrows(
                PaymentAlreadyExistsException.class,
                () -> service.record(command(visitId, "100000", "150000", "250000"))
        );
    }

    @Test
    void rethrowsUnrelatedPaymentPersistenceIntegrityError() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UUID visitId = UUID.randomUUID();

        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(completedVisit(visitId)));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("fk_payments_collected_by"));

        RecordPaymentService service = service(
                visitRepository,
                mock(MedicalRecordRepository.class),
                mock(PrescriptionRepository.class),
                paymentRepository,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class)
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> service.record(command(visitId, "100000", "150000", "250000"))
        );
    }

    @Test
    void rejectsAmountMismatch() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UUID visitId = UUID.randomUUID();

        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(completedVisit(visitId)));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());

        RecordPaymentService service = service(
                visitRepository,
                mock(MedicalRecordRepository.class),
                mock(PrescriptionRepository.class),
                paymentRepository,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class)
        );

        assertThrows(
                PaymentAmountMismatchException.class,
                () -> service.record(command(visitId, "100000", "150000", "200000"))
        );
    }

    @Test
    void rejectsZeroTotalAmount() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UUID visitId = UUID.randomUUID();

        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(completedVisit(visitId)));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());

        RecordPaymentService service = service(
                visitRepository,
                mock(MedicalRecordRepository.class),
                mock(PrescriptionRepository.class),
                paymentRepository,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class)
        );

        assertThrows(
                com.benhsoan.domain.shared.exception.ValidationException.class,
                () -> service.record(command(visitId, "0", "0", "0"))
        );
    }

    @Test
    void rejectsUnauthorizedActor() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);

        RecordPaymentService service = service(
                visitRepository,
                mock(MedicalRecordRepository.class),
                mock(PrescriptionRepository.class),
                mock(PaymentRepository.class),
                currentUserPort,
                fixedClock(),
                mock(AuditLogRepository.class)
        );

        assertThrows(
                AccessDeniedException.class,
                () -> service.record(command(UUID.randomUUID(), "100000", "150000", "250000"))
        );
    }

    @Test
    void rejectsPaymentWhenDiscountApprovalIsPending() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository discountRequestRepository =
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class);
        UUID visitId = UUID.randomUUID();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(completedVisit(visitId)));
        when(discountRequestRepository.existsByVisitIdAndStatus(visitId, com.benhsoan.domain.billing.enums.DiscountRequestStatus.PENDING))
                .thenReturn(true);

        RecordPaymentService service = new RecordPaymentService(
                visitRepository,
                mock(MedicalRecordRepository.class),
                mock(PrescriptionRepository.class),
                mock(PaymentRepository.class),
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new PaymentResultMapper(),
                noServiceFees(),
                mock(PaymentServiceFeeRepository.class),
                discountRequestRepository
        );

        assertThrows(
                com.benhsoan.domain.billing.exception.PendingDiscountApprovalException.class,
                () -> service.record(command(visitId, "100000", "150000", "250000"))
        );
    }

    @Test
    void recordsPaymentWithApprovedDiscount() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository discountRequestRepository =
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class);
        UUID visitId = UUID.randomUUID();
        UUID discountRequestId = UUID.randomUUID();
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(completedVisit(visitId)));
        when(discountRequestRepository.existsByVisitIdAndStatus(visitId, com.benhsoan.domain.billing.enums.DiscountRequestStatus.PENDING))
                .thenReturn(false);

        com.benhsoan.domain.billing.DiscountRequest approvedRequest = com.benhsoan.domain.billing.DiscountRequest.restore(
                discountRequestId,
                visitId,
                com.benhsoan.domain.billing.enums.DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                new BigDecimal("250000"),
                new BigDecimal("50000"),
                new BigDecimal("200000"),
                "Uu dai",
                com.benhsoan.domain.billing.enums.DiscountRequestStatus.APPROVED,
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                null,
                null,
                null,
                null
        );
        when(discountRequestRepository.findByVisitIdAndStatus(visitId, com.benhsoan.domain.billing.enums.DiscountRequestStatus.APPROVED))
                .thenReturn(Optional.of(approvedRequest));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordPaymentService service = new RecordPaymentService(
                visitRepository,
                mock(MedicalRecordRepository.class),
                mock(PrescriptionRepository.class),
                paymentRepository,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new PaymentResultMapper(),
                noServiceFees(),
                mock(PaymentServiceFeeRepository.class),
                discountRequestRepository
        );

        PaymentResult result = service.record(command(visitId, "100000", "150000", "200000"));

        assertEquals(new BigDecimal("250000"), result.totalAmount());
        assertEquals(new BigDecimal("50000"), result.discountAmount());
        assertEquals(new BigDecimal("200000"), result.amountPaid());
        assertEquals(discountRequestId, result.discountRequestId());
    }

    private static RecordPaymentService service(
            VisitRepository visitRepository,
            MedicalRecordRepository medicalRecordRepository,
            PrescriptionRepository prescriptionRepository,
            PaymentRepository paymentRepository,
            CurrentUserPort currentUserPort,
            ClockPort clockPort,
            AuditLogRepository auditLogRepository
    ) {
        return new RecordPaymentService(
                visitRepository,
                medicalRecordRepository,
                prescriptionRepository,
                paymentRepository,
                currentUserPort,
                clockPort,
                auditLogRepository,
                new PaymentResultMapper(),
                noServiceFees(),
                mock(PaymentServiceFeeRepository.class),
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class)
        );
    }

    private static ClinicalServiceFeeCalculator noServiceFees() {
        return new ClinicalServiceFeeCalculator(
                mock(ClinicalOrderItemRepository.class),
                mock(ServicePriceRepository.class)
        );
    }

    private static CurrentUserPort authorizedCurrentUser() {
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        return currentUserPort;
    }

    private static ClockPort fixedClock() {
        ClockPort clockPort = mock(ClockPort.class);
        when(clockPort.now()).thenReturn(Instant.parse("2026-08-12T01:00:00Z"));
        return clockPort;
    }

    private static RecordPaymentCommand command(
            UUID visitId,
            String examFee,
            String medicineFee,
            String amountPaid
    ) {
        return new RecordPaymentCommand(
                visitId,
                new BigDecimal(examFee),
                new BigDecimal(medicineFee),
                new BigDecimal(amountPaid),
                PaymentMethod.CASH
        );
    }

    private static Visit completedVisit(UUID visitId) {
        return Visit.restore(
                visitId,
                "VIS-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                VisitType.WALK_IN,
                VisitStatus.COMPLETED,
                Instant.parse("2026-08-12T00:00:00Z"),
                Instant.parse("2026-08-12T00:10:00Z"),
                Instant.parse("2026-08-12T00:30:00Z"),
                "Checkup",
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-12T00:00:00Z"),
                Instant.parse("2026-08-12T00:30:00Z")
        );
    }

    private static Visit waitingVisit(UUID visitId) {
        return Visit.restore(
                visitId,
                "VIS-002",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                VisitType.WALK_IN,
                VisitStatus.WAITING,
                Instant.parse("2026-08-12T00:00:00Z"),
                null,
                null,
                "Checkup",
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-12T00:00:00Z"),
                null
        );
    }

    private static Visit cancelledVisit(UUID visitId) {
        return Visit.restore(
                visitId,
                "VIS-003",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                VisitType.WALK_IN,
                VisitStatus.CANCELLED,
                Instant.parse("2026-08-12T00:00:00Z"),
                null,
                null,
                "Checkup",
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-12T00:00:00Z"),
                null
        );
    }

    private static MedicalRecord lockedMedicalRecord(UUID medicalRecordId, UUID visitId) {
        return MedicalRecord.restore(
                medicalRecordId,
                visitId,
                "Complaint",
                "Symptoms",
                null,
                null,
                null,
                null,
                null,
                null,
                MedicalRecordStatus.LOCKED,
                Instant.parse("2026-08-12T00:30:00Z"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-12T00:05:00Z"),
                null,
                null
        );
    }

    private static Prescription pendingPrescription(UUID medicalRecordId) {
        UUID prescriptionId = UUID.randomUUID();
        PrescriptionItem item = PrescriptionItem.create(
                UUID.randomUUID(),
                prescriptionId,
                UUID.randomUUID(),
                "Paracetamol",
                "Paracetamol",
                "500 mg",
                "vien",
                "1 vien",
                2,
                com.benhsoan.domain.medicine.enums.AdministrationRoute.ORAL,
                5,
                10,
                null,
                Instant.parse("2026-08-12T00:15:00Z")
        );
        return Prescription.restore(
                prescriptionId,
                "RX-001",
                medicalRecordId,
                PrescriptionStatus.PENDING_DISPENSE,
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-12T00:15:00Z"),
                null,
                null,
                List.of(item)
        );
    }

    private static Payment existingPayment(UUID visitId) {
        return Payment.restore(
                UUID.randomUUID(),
                visitId,
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                new BigDecimal("250000"),
                new BigDecimal("250000"),
                PaymentMethod.CASH,
                com.benhsoan.domain.billing.enums.PaymentStatus.RECORDED,
                UUID.randomUUID(),
                Instant.parse("2026-08-12T00:45:00Z"),
                Instant.parse("2026-08-12T00:45:00Z")
        );
    }

    @Test
    void serializesAuditLogCorrectlyWithSpecialChars() throws Exception {
        VisitRepository visitRepository = mock(VisitRepository.class);
        MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentServiceFeeRepository paymentServiceFeeRepository = mock(PaymentServiceFeeRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        ClinicalServiceFeeCalculator feeCalculator = mock(ClinicalServiceFeeCalculator.class);
        BillingAccessDeniedAuditWriter auditWriter = mock(BillingAccessDeniedAuditWriter.class);

        RecordPaymentService service = new RecordPaymentService(
                visitRepository,
                medicalRecordRepository,
                prescriptionRepository,
                paymentRepository,
                currentUserPort,
                clockPort,
                auditLogRepository,
                new PaymentResultMapper(),
                feeCalculator,
                paymentServiceFeeRepository,
                new ObjectMapper(),
                auditWriter
        );

        UUID visitId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-12T01:00:00Z");

        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(clockPort.now()).thenReturn(now);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(waitingVisit(visitId)));
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(feeCalculator.calculate(any(), any())).thenReturn(List.of());
        when(feeCalculator.total(any())).thenReturn(BigDecimal.ZERO);

        RecordPaymentCommand command = RecordPaymentCommand.builder()
                .visitId(visitId)
                .examFee(new BigDecimal("100000"))
                .medicineFee(new BigDecimal("150000"))
                .amountPaid(new BigDecimal("250000"))
                .paymentMethods(List.of(
                        new PaymentMethodItemCommand(PaymentMethod.CASH, new BigDecimal("100000"), null),
                        new PaymentMethodItemCommand(PaymentMethod.BANK_TRANSFER, new BigDecimal("150000"), "BIDV\"SPECIAL\\REF#01")
                ))
                .build();

        PaymentResult result = service.record(command);
        assertEquals(new BigDecimal("250000"), result.totalAmount());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());

        AuditLog savedAudit = auditCaptor.getValue();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(savedAudit.getDetail());

        assertEquals(visitId.toString(), jsonNode.get("visitId").asText());
        JsonNode methodsNode = jsonNode.get("paymentMethods");
        assertEquals(2, methodsNode.size());
        assertEquals("BIDV\"SPECIAL\\REF#01", methodsNode.get(1).get("referenceNumber").asText());
    }

    @Test
    void ensureAuthorizedWritesAccessDeniedAuditLogAndThrows() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentServiceFeeRepository paymentServiceFeeRepository = mock(PaymentServiceFeeRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        ClinicalServiceFeeCalculator feeCalculator = mock(ClinicalServiceFeeCalculator.class);
        BillingAccessDeniedAuditWriter auditWriter = mock(BillingAccessDeniedAuditWriter.class);

        RecordPaymentService service = new RecordPaymentService(
                visitRepository,
                medicalRecordRepository,
                prescriptionRepository,
                paymentRepository,
                currentUserPort,
                clockPort,
                auditLogRepository,
                new PaymentResultMapper(),
                feeCalculator,
                paymentServiceFeeRepository,
                new ObjectMapper(),
                auditWriter
        );

        UUID visitId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-12T01:00:00Z");

        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(clockPort.now()).thenReturn(now);

        RecordPaymentCommand command = RecordPaymentCommand.builder()
                .visitId(visitId)
                .examFee(new BigDecimal("100000"))
                .medicineFee(new BigDecimal("150000"))
                .amountPaid(new BigDecimal("250000"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        assertThrows(AccessDeniedException.class, () -> service.record(command));
        verify(auditWriter).writePaymentDenied(actorId, visitId, now, "Only receptionists can record payments.");
    }
}
