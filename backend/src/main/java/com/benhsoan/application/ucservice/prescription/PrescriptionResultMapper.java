package com.benhsoan.application.ucservice.prescription;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.PrescriptionWarningLog;
import com.benhsoan.port.dto.result.PrescriptionItemResult;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.dto.result.PrescriptionWarningResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PrescriptionResultMapper {

    private final PrescriptionDisplayContextResolver displayContextResolver;

    public PrescriptionResult toResult(
            Prescription prescription,
            List<PrescriptionWarningLog> warningLogs
    ) {
        List<PrescriptionWarningLog> safeWarningLogs = warningLogs == null
                ? List.of()
                : warningLogs;
        var displayContext = displayContextResolver.resolve(
                prescription.getMedicalRecordId(),
                prescription.getPrescribedBy()
        );

        return new PrescriptionResult(
                prescription.getId(),
                prescription.getPrescriptionCode(),
                prescription.getMedicalRecordId(),
                displayContext.visitId(),
                displayContext.visitCode(),
                displayContext.patientId(),
                displayContext.patientCode(),
                displayContext.patientName(),
                prescription.getStatus(),
                prescription.getNote(),
                prescription.getPrescribedBy(),
                displayContext.doctorName(),
                prescription.getPrescribedAt(),
                prescription.getUpdatedBy(),
                prescription.getUpdatedAt(),
                prescription.getItems()
                        .stream()
                        .map(this::toItemResult)
                        .toList(),
                safeWarningLogs
                        .stream()
                        .map(this::toWarningResult)
                        .toList()
        );
    }

    private PrescriptionItemResult toItemResult(
            PrescriptionItem item
    ) {
        return new PrescriptionItemResult(
                item.getId(),
                item.getPrescriptionId(),
                item.getMedicineId(),
                item.getMedicineName(),
                item.getActiveIngredient(),
                item.getStrength(),
                item.getUnit(),
                item.getDosage(),
                item.getFrequency(),
                item.getRoute(),
                item.getDurationDays(),
                item.getQuantity(),
                item.getInstructions(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private PrescriptionWarningResult toWarningResult(
            PrescriptionWarningLog warningLog
    ) {
        return new PrescriptionWarningResult(
                warningLog.getId(),
                warningLog.getRuleId(),
                warningLog.getFirstMedicineId(),
                warningLog.getSecondMedicineId(),
                warningLog.getSeverity(),
                warningLog.getWarningMessage(),
                warningLog.getAction(),
                warningLog.getOverrideReason(),
                warningLog.getHandledBy(),
                warningLog.getHandledAt()
        );
    }
}
