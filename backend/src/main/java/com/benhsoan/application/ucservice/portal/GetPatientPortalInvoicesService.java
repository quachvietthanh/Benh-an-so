package com.benhsoan.application.ucservice.portal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceSummaryResult;
import com.benhsoan.port.inbound.portal.GetPatientPortalInvoicesUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-007 CV-02: Returns invoices for the currently authenticated patient,
 * ordered by createdAt DESC (TC-01, TC-04).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientPortalInvoicesService implements GetPatientPortalInvoicesUseCase {

    private final PatientRepository patientRepository;
    private final InvoiceRepository invoiceRepository;
    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public List<PatientPortalInvoiceSummaryResult> getInvoices(UUID visitId) {
        return getInvoices(visitId, null);
    }

    @Override
    public List<PatientPortalInvoiceSummaryResult> getInvoices(UUID visitId, Integer limit) {
        UUID userId = currentUserPort.getCurrentUserId();
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "No patient profile is linked to the authenticated user."));

        List<Invoice> invoices = invoiceRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId());
        if (invoices.isEmpty()) {
            return List.of();
        }

        if (visitId != null) {
            invoices = invoices.stream()
                    .filter(inv -> visitId.equals(inv.getVisitId()))
                    .toList();
        }

        if (invoices.isEmpty()) {
            return List.of();
        }

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : 50;
        invoices = invoices.stream()
                .limit(effectiveLimit)
                .toList();

        Set<UUID> relevantVisitIds = invoices.stream()
                .map(Invoice::getVisitId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Visit> patientVisits = visitRepository.findByPatientIdOrderByVisitAtDesc(patient.getId());
        Map<UUID, Visit> visitsById = patientVisits.stream()
                .filter(v -> relevantVisitIds.contains(v.getId()))
                .collect(Collectors.toMap(Visit::getId, v -> v, (a, b) -> a));

        List<UUID> doctorIds = visitsById.values().stream()
                .map(Visit::getDoctorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, String> doctorNames = doctorIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(doctorIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));

        List<UUID> specialtyIds = visitsById.values().stream()
                .map(Visit::getSpecialtyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, String> specialtyNames = specialtyIds.isEmpty()
                ? Map.of()
                : specialtyRepository.findAllById(specialtyIds).stream()
                        .collect(Collectors.toMap(Specialty::getId, Specialty::getName, (a, b) -> a));

        return invoices.stream()
                .map(inv -> {
                    Visit visit = visitsById.get(inv.getVisitId());
                    String visitCode = visit != null ? visit.getVisitCode() : null;
                    var visitAt = visit != null ? visit.getVisitAt() : null;
                    String doctorName = (visit != null && visit.getDoctorId() != null)
                            ? doctorNames.get(visit.getDoctorId())
                            : null;
                    String specialtyName = (visit != null && visit.getSpecialtyId() != null)
                            ? specialtyNames.get(visit.getSpecialtyId())
                            : null;
                    int itemCount = inv.getLines() != null ? inv.getLines().size() : 0;

                    return new PatientPortalInvoiceSummaryResult(
                            inv.getId(),
                            inv.getInvoiceCode(),
                            inv.getType() != null ? inv.getType().name() : null,
                            inv.getTotalAmount(),
                            inv.getCreatedAt(),
                            inv.getVisitId(),
                            visitCode,
                            visitAt,
                            doctorName,
                            specialtyName,
                            itemCount
                    );
                })
                .toList();
    }
}
