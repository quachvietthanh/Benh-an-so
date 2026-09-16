package com.benhsoan.application.ucservice.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.prescription.PrescriptionDispenseItem;
import com.benhsoan.domain.prescription.exception.PrescriptionNotFoundException;
import com.benhsoan.port.dto.result.PrescriptionDispenseHistoryResult;
import com.benhsoan.port.inbound.prescription.GetPrescriptionDispenseHistoryUseCase;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionDispenseItemRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPrescriptionDispenseHistoryService implements GetPrescriptionDispenseHistoryUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDispenseItemRepository dispenseItemRepository;

    @Override
    public List<PrescriptionDispenseHistoryResult> getHistory(UUID prescriptionId) {
        if (prescriptionId == null) {
            return List.of();
        }
        prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));

        return dispenseItemRepository.findByPrescriptionId(prescriptionId).stream()
                .map(this::toResult)
                .toList();
    }

    private PrescriptionDispenseHistoryResult toResult(PrescriptionDispenseItem item) {
        return new PrescriptionDispenseHistoryResult(
                item.getId(),
                item.getPrescriptionId(),
                item.getPrescriptionItemId(),
                item.getMedicineId(),
                item.getMedicineBatchId(),
                item.getDispensedQuantity(),
                item.getDispensedBy(),
                item.getDispensedAt());
    }
}
