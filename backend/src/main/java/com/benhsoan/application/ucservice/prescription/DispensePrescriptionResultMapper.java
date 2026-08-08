package com.benhsoan.application.ucservice.prescription;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionWarningLog;
import com.benhsoan.port.dto.result.DispenseAllocationResult;
import com.benhsoan.port.dto.result.DispensePrescriptionResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class DispensePrescriptionResultMapper {

    private final PrescriptionResultMapper prescriptionResultMapper;

    DispensePrescriptionResult toResult(
            Prescription prescription,
            List<PrescriptionWarningLog> warningLogs,
            UUID dispensedBy,
            Instant dispensedAt,
            List<DispenseAllocationResult> allocations
    ) {
        return new DispensePrescriptionResult(
                prescriptionResultMapper.toResult(prescription, warningLogs),
                dispensedBy,
                dispensedAt,
                allocations.size(),
                allocations.stream()
                        .mapToInt(DispenseAllocationResult::dispensedQuantity)
                        .sum(),
                List.copyOf(allocations)
        );
    }
}
