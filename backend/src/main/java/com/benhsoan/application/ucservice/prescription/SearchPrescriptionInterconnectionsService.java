package com.benhsoan.application.ucservice.prescription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.SearchPrescriptionInterconnectionsQuery;
import com.benhsoan.port.dto.result.PrescriptionInterconnectionListItemResult;
import com.benhsoan.port.inbound.prescription.SearchPrescriptionInterconnectionsUseCase;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchPrescriptionInterconnectionsService implements SearchPrescriptionInterconnectionsUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDisplayContextResolver displayContextResolver;
    private final CurrentUserPort currentUserPort;

    @Override
    public Page<PrescriptionInterconnectionListItemResult> search(
            SearchPrescriptionInterconnectionsQuery query
    ) {
        if (!currentUserPort.hasRole("ADMIN")) {
            throw new AccessDeniedException("Only administrators can search prescription interconnections.");
        }
        validate(query);
        var pageable = PageRequest.of(query.page(), query.size(),
                Sort.by(Sort.Direction.DESC, "lastInterconnectionAt"));
        return prescriptionRepository.findByInterconnectionStatus(
                        query.status(), query.from(), query.to(), pageable)
                .map(prescription -> {
                    var context = displayContextResolver.resolve(
                            prescription.getMedicalRecordId(), prescription.getPrescribedBy());
                    return new PrescriptionInterconnectionListItemResult(
                            prescription.getId(), prescription.getPrescriptionCode(),
                            context.patientId(), context.patientCode(), context.patientName(),
                            prescription.getPrescribedBy(), context.doctorName(), prescription.getStatus(),
                            prescription.getInterconnectionStatus(), prescription.getLastInterconnectionAt(),
                            prescription.getLastInterconnectionError(), prescription.getInterconnectionReceiptCode()
                    );
                });
    }

    private void validate(SearchPrescriptionInterconnectionsQuery query) {
        if (query.status() == null) {
            throw new ValidationException("Interconnection status is required.");
        }
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new ValidationException("from must be before or equal to to.");
        }
        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            throw new ValidationException("Page must be non-negative and size must be between 1 and 100.");
        }
    }
}
