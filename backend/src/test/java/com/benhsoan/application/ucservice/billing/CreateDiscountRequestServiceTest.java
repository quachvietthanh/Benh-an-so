package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.billing.DiscountRequest;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;
import com.benhsoan.domain.billing.exception.DiscountAlreadyExistsException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.billing.CreateDiscountRequestCommand;
import com.benhsoan.port.dto.result.DiscountRequestResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class CreateDiscountRequestServiceTest {

    @Test
    void createsPercentageDiscountRequestSuccessfully() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        DiscountRequestRepository discountRequestRepository = mock(DiscountRequestRepository.class);
        ClinicalServiceFeeCalculator feeCalculator = mock(ClinicalServiceFeeCalculator.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

        UUID visitId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T10:00:00Z");

        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(clockPort.now()).thenReturn(now);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit(visitId)));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(discountRequestRepository.existsByVisitIdAndStatus(visitId, DiscountRequestStatus.PENDING)).thenReturn(false);
        when(discountRequestRepository.save(any(DiscountRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateDiscountRequestService service = new CreateDiscountRequestService(
                visitRepository,
                paymentRepository,
                discountRequestRepository,
                feeCalculator,
                currentUserPort,
                clockPort,
                auditLogRepository,
                new DiscountRequestResultMapper()
        );

        CreateDiscountRequestCommand command = new CreateDiscountRequestCommand(
                visitId,
                DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                new BigDecimal("500000"),
                "Bệnh nhân có hoàn cảnh khó khăn"
        );

        DiscountRequestResult result = service.create(command);

        assertNotNull(result.id());
        assertEquals(visitId, result.visitId());
        assertEquals(DiscountType.PERCENTAGE, result.discountType());
        assertEquals(new BigDecimal("100000.00"), result.discountAmount());
        assertEquals(new BigDecimal("400000.00"), result.finalAmount());
        assertEquals(DiscountRequestStatus.PENDING, result.status());
        assertEquals(actorId, result.requestedBy());
        verify(discountRequestRepository).save(any(DiscountRequest.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void rejectsWhenPendingDiscountAlreadyExists() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        DiscountRequestRepository discountRequestRepository = mock(DiscountRequestRepository.class);
        ClinicalServiceFeeCalculator feeCalculator = mock(ClinicalServiceFeeCalculator.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);

        UUID visitId = UUID.randomUUID();
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit(visitId)));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(discountRequestRepository.existsByVisitIdAndStatus(visitId, DiscountRequestStatus.PENDING)).thenReturn(true);

        CreateDiscountRequestService service = new CreateDiscountRequestService(
                visitRepository,
                paymentRepository,
                discountRequestRepository,
                feeCalculator,
                currentUserPort,
                clockPort,
                mock(AuditLogRepository.class),
                new DiscountRequestResultMapper()
        );

        CreateDiscountRequestCommand command = new CreateDiscountRequestCommand(
                visitId,
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("50000"),
                new BigDecimal("200000"),
                "Bệnh nhân diện chính sách"
        );

        assertThrows(DiscountAlreadyExistsException.class, () -> service.create(command));
    }

    @Test
    void rejectsWhenVisitIsCancelled() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        UUID visitId = UUID.randomUUID();

        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        Visit cancelled = Visit.restore(
                visitId, "VIS-01", UUID.randomUUID(), UUID.randomUUID(), null, null,
                VisitType.WALK_IN, VisitStatus.CANCELLED, Instant.now(), null, null,
                "Checkup", null, UUID.randomUUID(), Instant.now(), null
        );
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(cancelled));

        CreateDiscountRequestService service = new CreateDiscountRequestService(
                visitRepository,
                mock(PaymentRepository.class),
                mock(DiscountRequestRepository.class),
                mock(ClinicalServiceFeeCalculator.class),
                currentUserPort,
                mock(ClockPort.class),
                mock(AuditLogRepository.class),
                new DiscountRequestResultMapper()
        );

        CreateDiscountRequestCommand command = new CreateDiscountRequestCommand(
                visitId,
                DiscountType.FULL_FREE,
                BigDecimal.ZERO,
                new BigDecimal("100000"),
                "Miễn phí"
        );

        assertThrows(ValidationException.class, () -> service.create(command));
    }

    @Test
    void rejectsWhenApprovedDiscountAlreadyExists() {
        VisitRepository visitRepository = mock(VisitRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        DiscountRequestRepository discountRequestRepository = mock(DiscountRequestRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);

        UUID visitId = UUID.randomUUID();
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit(visitId)));
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(discountRequestRepository.existsByVisitIdAndStatus(visitId, DiscountRequestStatus.PENDING)).thenReturn(false);
        when(discountRequestRepository.existsByVisitIdAndStatus(visitId, DiscountRequestStatus.APPROVED)).thenReturn(true);

        CreateDiscountRequestService service = new CreateDiscountRequestService(
                visitRepository,
                paymentRepository,
                discountRequestRepository,
                mock(ClinicalServiceFeeCalculator.class),
                currentUserPort,
                mock(ClockPort.class),
                mock(AuditLogRepository.class),
                new DiscountRequestResultMapper()
        );

        CreateDiscountRequestCommand command = new CreateDiscountRequestCommand(
                visitId,
                DiscountType.PERCENTAGE,
                new BigDecimal("15"),
                new BigDecimal("300000"),
                "Tạo thêm đề xuất khi đã có đề xuất duyệt"
        );

        assertThrows(DiscountAlreadyExistsException.class, () -> service.create(command));
    }

    @Test
    void rejectsDoctorRoleWithoutInvoiceCreatePermission() {
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(currentUserPort.hasPermission("INVOICE_CREATE")).thenReturn(false);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);

        CreateDiscountRequestService service = new CreateDiscountRequestService(
                mock(VisitRepository.class),
                mock(PaymentRepository.class),
                mock(DiscountRequestRepository.class),
                mock(ClinicalServiceFeeCalculator.class),
                currentUserPort,
                mock(ClockPort.class),
                mock(AuditLogRepository.class),
                new DiscountRequestResultMapper()
        );

        CreateDiscountRequestCommand command = new CreateDiscountRequestCommand(
                UUID.randomUUID(),
                DiscountType.PERCENTAGE,
                new BigDecimal("10"),
                new BigDecimal("100000"),
                "Bác sĩ tự tạo đề xuất"
        );

        assertThrows(AccessDeniedException.class, () -> service.create(command));
    }

    @Test
    void rejectsUnauthorizedUser() {
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);

        CreateDiscountRequestService service = new CreateDiscountRequestService(
                mock(VisitRepository.class),
                mock(PaymentRepository.class),
                mock(DiscountRequestRepository.class),
                mock(ClinicalServiceFeeCalculator.class),
                currentUserPort,
                mock(ClockPort.class),
                mock(AuditLogRepository.class),
                new DiscountRequestResultMapper()
        );

        CreateDiscountRequestCommand command = new CreateDiscountRequestCommand(
                UUID.randomUUID(),
                DiscountType.PERCENTAGE,
                new BigDecimal("10"),
                new BigDecimal("100000"),
                "Lý do"
        );

        assertThrows(AccessDeniedException.class, () -> service.create(command));
    }

    private static Visit activeVisit(UUID visitId) {
        return Visit.restore(
                visitId, "VIS-01", UUID.randomUUID(), UUID.randomUUID(), null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, Instant.now(), null, null,
                "Checkup", null, UUID.randomUUID(), Instant.now(), null
        );
    }
}
