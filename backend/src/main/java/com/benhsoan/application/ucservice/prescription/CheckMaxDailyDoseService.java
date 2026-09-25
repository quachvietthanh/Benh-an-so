package com.benhsoan.application.ucservice.prescription;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.prescription.MaxDailyDoseCalculator;
import com.benhsoan.domain.prescription.MaxDailyDoseEvaluationResult;
import com.benhsoan.domain.prescription.MaxDailyDoseMissingData;
import com.benhsoan.domain.prescription.MaxDailyDoseWarning;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.CheckMaxDailyDoseCommand;
import com.benhsoan.port.dto.command.prescription.CheckMaxDailyDoseItemCommand;
import com.benhsoan.port.dto.result.MaxDailyDoseCheckResult;
import com.benhsoan.port.dto.result.MaxDailyDoseMissingDataResult;
import com.benhsoan.port.dto.result.MaxDailyDoseWarningResult;
import com.benhsoan.port.inbound.prescription.CheckMaxDailyDoseUseCase;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-05-CN-007: read-only, non-persistent pre-check of the total daily
 * active-ingredient dose. Reuses {@link MaxDailyDoseCalculator} as the single
 * source of truth so create/amend submission and this pre-check stay consistent.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckMaxDailyDoseService implements CheckMaxDailyDoseUseCase {

    private final MedicineRepository medicineRepository;
    private final PrescriptionClinicalContextValidator clinicalContextValidator;
    private final CurrentUserPort currentUserPort;

    @Override
    public MaxDailyDoseCheckResult check(CheckMaxDailyDoseCommand command) {
        requireCommand(command);

        // Same doctor/clinical-context authorization as the other safety pre-checks.
        UUID currentUserId = currentUserPort.getCurrentUserId();
        clinicalContextValidator.requireEditableRecordForDoctor(
                command.medicalRecordId(),
                currentUserId
        );

        List<CheckMaxDailyDoseItemCommand> items = distinctItems(command.items());
        if (items.isEmpty()) {
            return new MaxDailyDoseCheckResult(List.of(), List.of());
        }

        Map<UUID, Medicine> medicines = loadActiveMedicines(items);

        MaxDailyDoseEvaluationResult evaluation = MaxDailyDoseCalculator.evaluate(
                items.stream()
                        .map(item -> toDoseItem(item, medicines.get(item.medicineId())))
                        .toList()
        );

        return new MaxDailyDoseCheckResult(
                evaluation.warnings().stream().map(this::toWarningResult).toList(),
                evaluation.missingData().stream().map(this::toMissingDataResult).toList()
        );
    }

    private void requireCommand(CheckMaxDailyDoseCommand command) {
        if (command == null) {
            throw new ValidationException("Check max daily dose command is required.");
        }
        if (command.medicalRecordId() == null) {
            throw new ValidationException("Medical record id is required to check max daily dose.");
        }
    }

    private List<CheckMaxDailyDoseItemCommand> distinctItems(
            List<CheckMaxDailyDoseItemCommand> items
    ) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Map<UUID, CheckMaxDailyDoseItemCommand> byMedicineId = new LinkedHashMap<>();
        for (CheckMaxDailyDoseItemCommand item : items) {
            if (item == null || item.medicineId() == null) {
                throw new ValidationException("Medicine id is required.");
            }
            if (byMedicineId.put(item.medicineId(), item) != null) {
                throw new ValidationException("Duplicate medicine in max daily dose check: "
                        + item.medicineId());
            }
        }
        return List.copyOf(byMedicineId.values());
    }

    private Map<UUID, Medicine> loadActiveMedicines(List<CheckMaxDailyDoseItemCommand> items) {
        List<Medicine> found = medicineRepository.findAllById(
                items.stream().map(CheckMaxDailyDoseItemCommand::medicineId).toList());
        Map<UUID, Medicine> medicines = new LinkedHashMap<>();
        for (Medicine medicine : found) {
            medicines.put(medicine.getId(), medicine);
        }
        for (CheckMaxDailyDoseItemCommand item : items) {
            Medicine medicine = medicines.get(item.medicineId());
            if (medicine == null) {
                throw new ValidationException("Medicine not found: " + item.medicineId());
            }
            if (!medicine.isActive()) {
                throw new ValidationException("Inactive medicine cannot be checked: "
                        + medicine.getId());
            }
        }
        return medicines;
    }

    private MaxDailyDoseCalculator.Item toDoseItem(
            CheckMaxDailyDoseItemCommand item,
            Medicine medicine
    ) {
        return new MaxDailyDoseCalculator.Item(
                medicine.getActiveIngredient(),
                medicine.getStrengthValueMg(),
                item.singleDoseQuantity(),
                item.frequency() == null ? 0 : item.frequency(),
                medicine.getMaxDailyDoseMg()
        );
    }

    private MaxDailyDoseWarningResult toWarningResult(MaxDailyDoseWarning warning) {
        return new MaxDailyDoseWarningResult(
                warning.activeIngredient(),
                warning.totalDailyDoseMg(),
                warning.maxDailyDoseMg()
        );
    }

    private MaxDailyDoseMissingDataResult toMissingDataResult(MaxDailyDoseMissingData missingData) {
        return new MaxDailyDoseMissingDataResult(
                missingData.activeIngredient(),
                missingData.reason()
        );
    }
}
