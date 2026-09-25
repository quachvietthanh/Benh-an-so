package com.benhsoan.application.ucservice.medicine;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.MedicineMaxDailyDoseMissingData;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.medicine.UpdateMedicineCommand;
import com.benhsoan.port.dto.result.MedicineResult;
import com.benhsoan.port.inbound.medicine.UpdateMedicineUseCase;
import com.benhsoan.port.outbound.repository.medicine.MedicineMaxDailyDoseMissingDataRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMedicineService implements UpdateMedicineUseCase {

    private final MedicineRepository medicineRepository;

    private final MedicineMaxDailyDoseMissingDataRepository medicineMaxDailyDoseMissingDataRepository;

    private final MedicineManagementAuthorizer authorizer;

    private final MedicineResultMapper resultMapper;

    private final ClockPort clockPort;

    private final AdminOperationAuditService adminOperationAuditService;

    private final CurrentUserPort currentUserPort;

    @Override
    public MedicineResult update(UpdateMedicineCommand command) {
        requireCommand(command);
        authorizer.requirePharmacist();

        Medicine medicine = medicineRepository.findById(command.medicineId())
                .orElseThrow(() -> new ValidationException(
                        "Medicine not found: " + command.medicineId()
                ));

        var before = AdminOperationAuditService.fields(
                "medicineName", medicine.getMedicineName(),
                "activeIngredient", medicine.getActiveIngredient(),
                "strength", medicine.getStrength(),
                "dosageForm", medicine.getDosageForm(),
                "unit", medicine.getUnit(),
                "defaultRoute", medicine.getDefaultRoute(),
                "minStockThreshold", medicine.getMinStockThreshold(),
                "controlled", medicine.isControlled(),
                "strengthValueMg", medicine.getStrengthValueMg(),
                "maxDailyDoseMg", medicine.getMaxDailyDoseMg());

        medicine.updateInformation(
                command.medicineName(),
                command.activeIngredient(),
                command.strength(),
                command.dosageForm(),
                command.unit(),
                command.defaultRoute(),
                command.minStockThreshold(),
                command.controlled(),
                command.strengthValueMg(),
                command.maxDailyDoseMg(),
                clockPort.now()
        );
        validateUniqueness(medicine);

        Medicine saved = medicineRepository.save(medicine);

        clearResolvedMissingMaxDailyDoseData(saved);

        adminOperationAuditService.record(
                currentUserPort.getCurrentUserId(),
                ActionType.UPDATE,
                ResourceType.MEDICINE,
                saved.getId(),
                before,
                AdminOperationAuditService.fields(
                        "medicineName", saved.getMedicineName(),
                        "activeIngredient", saved.getActiveIngredient(),
                        "strength", saved.getStrength(),
                        "dosageForm", saved.getDosageForm(),
                        "unit", saved.getUnit(),
                        "defaultRoute", saved.getDefaultRoute(),
                        "minStockThreshold", saved.getMinStockThreshold(),
                        "controlled", saved.isControlled(),
                        "strengthValueMg", saved.getStrengthValueMg(),
                        "maxDailyDoseMg", saved.getMaxDailyDoseMg()),
                clockPort.now()
        );

        return resultMapper.toResult(saved);
    }

    private void validateUniqueness(Medicine medicine) {
        if (medicineRepository.existsByMedicineNameAndActiveIngredient(
                normalize(medicine.getMedicineName()),
                normalize(medicine.getActiveIngredient()),
                medicine.getId()
        )) {
            throw new ValidationException(
                    "Medicine name and active ingredient already exist."
            );
        }
    }

    private void clearResolvedMissingMaxDailyDoseData(Medicine saved) {
        if (saved.getMaxDailyDoseMg() != null) {
            medicineMaxDailyDoseMissingDataRepository.clear(
                    saved.getId(),
                    MedicineMaxDailyDoseMissingData.REASON_MAX_DAILY_DOSE
            );
        }
        if (saved.getStrengthValueMg() != null) {
            medicineMaxDailyDoseMissingDataRepository.clear(
                    saved.getId(),
                    MedicineMaxDailyDoseMissingData.REASON_STRENGTH_VALUE
            );
        }
    }

    private static void requireCommand(UpdateMedicineCommand command) {
        if (command == null) {
            throw new ValidationException("Update medicine command is required.");
        }
        if (command.medicineId() == null) {
            throw new ValidationException("Medicine id is required.");
        }
    }

    private static String normalize(String value) {
        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
