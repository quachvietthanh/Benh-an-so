package com.benhsoan.application.ucservice.controlledmedicine;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.controlledmedicine.ControlledMedicineRegister;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.controlledmedicine.ControlledMedicineRegisterRepository;

import lombok.RequiredArgsConstructor;

/**
 * Creates append-only records in the special controlled medicine register and
 * writes a matching audit entry for each record (CV-04 traceability).
 *
 * <p>Invoked from within the dispensing transaction so a register record can
 * never be created without a successful dispensing operation, nor can a
 * dispensing operation succeed without its register record (atomicity).</p>
 */
@Component
@RequiredArgsConstructor
public class ControlledMedicineRegisterRecorder {

    private final ControlledMedicineRegisterRepository registerRepository;
    private final AuditLogRepository auditLogRepository;

    public void record(
            UUID prescriptionId,
            UUID prescribedBy,
            UUID patientId,
            UUID dispensedBy,
            Instant dispensedAt,
            List<Entry> entries
    ) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        List<ControlledMedicineRegister> records = entries.stream()
                .map(entry -> ControlledMedicineRegister.create(
                        UUID.randomUUID(),
                        prescriptionId,
                        entry.prescriptionItemId(),
                        entry.medicineId(),
                        entry.medicineName(),
                        patientId,
                        prescribedBy,
                        dispensedBy,
                        entry.quantity(),
                        dispensedAt
                ))
                .toList();

        registerRepository.saveAll(records);

        for (ControlledMedicineRegister record : records) {
            auditLogRepository.save(AuditLog.create(
                    dispensedBy,
                    ActionType.CREATE,
                    ResourceType.CONTROLLED_MEDICINE_REGISTER,
                    record.getId(),
                    "{\"prescriptionId\":\"%s\",\"medicineId\":\"%s\",\"quantity\":%d}".formatted(
                            record.getPrescriptionId(),
                            record.getMedicineId(),
                            record.getQuantity()
                    ),
                    null,
                    dispensedAt
            ));
        }
    }

    public record Entry(
            UUID prescriptionItemId,
            UUID medicineId,
            String medicineName,
            int quantity
    ) {
    }
}
