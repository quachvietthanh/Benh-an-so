package com.benhsoan.application.ucservice.prescription;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.PrescriptionWarningLog;
import com.benhsoan.port.dto.result.DispenseAllocationResult;
import com.benhsoan.port.dto.result.DispenseItemSummaryResult;
import com.benhsoan.port.dto.result.PartialDispensePrescriptionResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PartialDispensePrescriptionResultMapper {

    private final PrescriptionResultMapper prescriptionResultMapper;

    public PartialDispensePrescriptionResult toResult(
            Prescription prescription,
            List<PrescriptionWarningLog> warningLogs,
            UUID dispensedBy,
            Instant dispensedAt,
            List<PrescriptionItem> items,
            Map<UUID, Medicine> medicines,
            List<DispenseAllocationResult> allocations
    ) {
        List<DispenseItemSummaryResult> itemSummaries = items.stream()
                .map(item -> toItemSummary(item, medicines.get(item.getMedicineId())))
                .toList();

        return new PartialDispensePrescriptionResult(
                prescriptionResultMapper.toResult(prescription, warningLogs),
                dispensedBy,
                dispensedAt,
                itemSummaries,
                List.copyOf(allocations));
    }

    private DispenseItemSummaryResult toItemSummary(PrescriptionItem item, Medicine medicine) {
        return new DispenseItemSummaryResult(
                item.getId(),
                item.getMedicineId(),
                medicine != null ? medicine.getMedicineCode() : null,
                item.getMedicineName(),
                item.getUnit(),
                item.getQuantity(),
                item.getDispensedQuantity(),
                item.getRemainingQuantity());
    }
}
