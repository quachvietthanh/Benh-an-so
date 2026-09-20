package com.benhsoan.application.ucservice.prescription;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.exception.PrescriptionNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.DispenseSuggestionBatchResult;
import com.benhsoan.port.dto.result.DispenseSuggestionItemResult;
import com.benhsoan.port.dto.result.DispenseSuggestionResult;
import com.benhsoan.port.inbound.prescription.GetDispenseSuggestionUseCase;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDispenseSuggestionService implements GetDispenseSuggestionUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public DispenseSuggestionResult getSuggestion(UUID prescriptionId) {
        if (prescriptionId == null) {
            throw new ValidationException("Prescription id is required.");
        }
        if (!currentUserPort.hasRole("PHARMACIST") && !currentUserPort.hasRole("ADMIN")) {
            throw new AccessDeniedException("Only pharmacists can view dispensing suggestions.");
        }

        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));

        if (prescription.getStatus() != PrescriptionStatus.PENDING_DISPENSE
                && prescription.getStatus() != PrescriptionStatus.PARTIALLY_DISPENSED) {
            throw new ValidationException(
                    "Only pending or partially dispensed prescriptions have a dispensing suggestion.");
        }

        Instant now = clockPort.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

        List<DispenseSuggestionItemResult> items = new ArrayList<>();
        for (PrescriptionItem item : prescription.getItems()) {
            int remaining = item.getRemainingQuantity();
            if (remaining <= 0) {
                continue;
            }

            List<MedicineBatch> batches = medicineBatchRepository
                    .findAvailableByMedicineId(item.getMedicineId(), today);

            List<DispenseSuggestionBatchResult> batchResults = new ArrayList<>();
            int stillToAllocate = remaining;
            for (MedicineBatch batch : batches) {
                if (stillToAllocate <= 0) {
                    break;
                }
                int suggested = Math.min(stillToAllocate, batch.getQuantity());
                batchResults.add(new DispenseSuggestionBatchResult(
                        batch.getId(),
                        batch.getBatchNumber(),
                        batch.getExpiryDate(),
                        batch.getQuantity(),
                        suggested
                ));
                stillToAllocate -= suggested;
            }

            items.add(new DispenseSuggestionItemResult(
                    item.getId(),
                    item.getMedicineId(),
                    item.getMedicineName(),
                    item.getQuantity(),
                    remaining,
                    List.copyOf(batchResults)
            ));
        }

        return new DispenseSuggestionResult(prescriptionId, List.copyOf(items));
    }
}
