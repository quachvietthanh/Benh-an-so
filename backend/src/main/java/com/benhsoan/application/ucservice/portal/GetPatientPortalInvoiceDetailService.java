package com.benhsoan.application.ucservice.portal;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceDetailResult;
import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceDetailResult.InvoiceLineItemView;
import com.benhsoan.port.inbound.portal.GetPatientPortalInvoiceDetailUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-007 CV-02: Returns detailed invoice data for the authenticated patient.
 * Strictly enforces QTN-23 via {@link PatientAccessGuard}, denying cross-patient access (TC-03),
 * and audits READ operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetPatientPortalInvoiceDetailService implements GetPatientPortalInvoiceDetailUseCase {

    private static final String ONLINE_PORTAL = "ONLINE_PORTAL";

    private final InvoiceRepository invoiceRepository;
    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PatientAccessGuard patientAccessGuard;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public PatientPortalInvoiceDetailResult getInvoiceDetail(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        Visit visit = visitRepository.findById(invoice.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(invoice.getVisitId()));

        // TC-03 / QTN-23: reject cross-patient access (403) and record ACCESS_DENIED audit log
        patientAccessGuard.requirePatientOwnership(
                visit.getPatientId(),
                ResourceType.INVOICE,
                invoiceId
        );

        String originalInvoiceCode = null;
        if (invoice.getOriginalInvoiceId() != null) {
            originalInvoiceCode = invoiceRepository.findById(invoice.getOriginalInvoiceId())
                    .map(Invoice::getInvoiceCode)
                    .orElse(null);
        }

        String creatorName = userRepository.findById(invoice.getCreatedBy())
                .map(User::getFullName)
                .orElse(null);

        String doctorName = userRepository.findById(visit.getDoctorId())
                .map(User::getFullName)
                .orElse(null);

        String specialtyName = specialtyRepository.findById(visit.getSpecialtyId())
                .map(Specialty::getName)
                .orElse(null);

        List<InvoiceLineItemView> items = invoice.getLines() == null ? List.of() : invoice.getLines().stream()
                .map(line -> new InvoiceLineItemView(
                        line.getId(),
                        line.getLineType() != null ? line.getLineType().name() : null,
                        line.getItemName(),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        line.getAmount()
                ))
                .toList();

        Instant viewedAt = clockPort.now();
        UUID currentUserId = currentUserPort.getCurrentUserId();

        // Audit READ operation
        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.READ,
                ResourceType.INVOICE,
                invoice.getId(),
                auditDetail(visit, invoice, viewedAt),
                null,
                viewedAt
        ));

        return new PatientPortalInvoiceDetailResult(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getType() != null ? invoice.getType().name() : null,
                invoice.getOriginalInvoiceId(),
                originalInvoiceCode,
                invoice.getAdjustmentReason(),
                invoice.getTotalAmount(),
                invoice.getCreatedAt(),
                creatorName,
                visit.getId(),
                visit.getVisitCode(),
                visit.getVisitAt(),
                doctorName,
                specialtyName,
                items
        );
    }

    private String auditDetail(Visit visit, Invoice invoice, Instant viewedAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("channel", ONLINE_PORTAL);
        detail.put("invoiceId", invoice.getId().toString());
        detail.put("invoiceCode", invoice.getInvoiceCode());
        detail.put("visitId", visit.getId().toString());
        detail.put("patientId", visit.getPatientId().toString());
        detail.put("viewedAt", viewedAt.toString());

        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize patient invoice detail audit.", exception);
        }
    }
}
