package com.benhsoan.application.ucservice.billing;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.exception.PaymentAlreadyExistsException;
import com.benhsoan.domain.billing.exception.PaymentNotAllowedException;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.billing.RecordPaymentCommand;
import com.benhsoan.port.dto.result.PaymentResult;
import com.benhsoan.port.inbound.billing.RecordPaymentUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RecordPaymentService implements RecordPaymentUseCase {

    private final VisitRepository visitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final PaymentResultMapper resultMapper;

    @Override
    public PaymentResult record(RecordPaymentCommand command) {
        ensureAuthorized();

        Visit visit = visitRepository.findByIdForUpdate(command.visitId())
                .orElseThrow(() -> new VisitNotFoundException(command.visitId()));

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            throw new PaymentNotAllowedException(
                    "Payment cannot be recorded for cancelled visits."
            );
        }

        if (paymentRepository.findByVisitId(visit.getId()).isPresent()) {
            throw new PaymentAlreadyExistsException(visit.getId());
        }

        ensureDispensingCompleted(visit.getId());

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        Payment payment = Payment.record(
                UUID.randomUUID(),
                visit.getId(),
                command.examFee(),
                command.medicineFee(),
                command.amountPaid(),
                command.paymentMethod(),
                actorId,
                now,
                visit.getStatus(),
                true
        );

        Payment saved;
        try {
            saved = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicatePaymentConflict(ex)) {
                throw new PaymentAlreadyExistsException(visit.getId());
            }
            throw ex;
        }

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.CREATE,
                ResourceType.PAYMENT,
                saved.getId(),
                """
                {
                "visitId":"%s",
                "examFee":"%s",
                "medicineFee":"%s",
                "totalAmount":"%s",
                "paymentMethod":"%s"
                }
                """.formatted(
                        saved.getVisitId(),
                        saved.getExamFee(),
                        saved.getMedicineFee(),
                        saved.getTotalAmount(),
                        saved.getPaymentMethod()
                ),
                null
        ));

        return resultMapper.toResult(saved);
    }

    private void ensureAuthorized() {
        if (!currentUserPort.hasRole("ADMIN")
                && !currentUserPort.hasRole("RECEPTIONIST")) {
            throw new AccessDeniedException("Only receptionists can record payments.");
        }
    }

    private void ensureDispensingCompleted(UUID visitId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findByVisitId(visitId)
                .orElse(null);

        if (medicalRecord == null) {
            return;
        }

        List<Prescription> prescriptions = prescriptionRepository
                .findByMedicalRecordId(medicalRecord.getId());

        boolean hasPendingDispense = prescriptions.stream()
                .anyMatch(prescription -> prescription.getStatus() == PrescriptionStatus.PENDING_DISPENSE);

        if (hasPendingDispense) {
            throw new PaymentNotAllowedException(
                    "Payment cannot be recorded before dispensing is completed."
            );
        }
    }

    private boolean isDuplicatePaymentConflict(DataIntegrityViolationException ex) {
        String message = extractMessage(ex).toLowerCase();
        return message.contains("uk_payments_visit")
                || message.contains("duplicate entry")
                && message.contains("visit_id");
    }

    private String extractMessage(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                builder.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        return builder.toString();
    }
}
