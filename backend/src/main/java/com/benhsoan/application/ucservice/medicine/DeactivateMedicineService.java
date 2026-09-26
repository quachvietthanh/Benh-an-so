package com.benhsoan.application.ucservice.medicine;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.MedicineResult;
import com.benhsoan.port.inbound.medicine.DeactivateMedicineUseCase;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeactivateMedicineService implements DeactivateMedicineUseCase {

    private final MedicineRepository medicineRepository;

    private final MedicineManagementAuthorizer authorizer;

    private final MedicineResultMapper resultMapper;

    private final ClockPort clockPort;

    private final AdminOperationAuditService adminOperationAuditService;

    private final CurrentUserPort currentUserPort;

    @Override
    public MedicineResult deactivate(UUID medicineId) {
        requireMedicineId(medicineId);
        authorizer.requirePharmacist();

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new ValidationException(
                        "Medicine not found: " + medicineId
                ));

        if (medicine.isActive()) {
            medicine.deactivate(clockPort.now());
            medicine = medicineRepository.save(medicine);

            adminOperationAuditService.record(
                    currentUserPort.getCurrentUserId(),
                    ActionType.DEACTIVATE,
                    ResourceType.MEDICINE,
                    medicine.getId(),
                    AdminOperationAuditService.fields("medicineCode", medicine.getMedicineCode(), "active", true),
                    AdminOperationAuditService.fields("medicineCode", medicine.getMedicineCode(), "active", false),
                    clockPort.now()
            );
        }

        return resultMapper.toResult(medicine);
    }

    private static void requireMedicineId(UUID medicineId) {
        if (medicineId == null) {
            throw new ValidationException("Medicine id is required.");
        }
    }
}
