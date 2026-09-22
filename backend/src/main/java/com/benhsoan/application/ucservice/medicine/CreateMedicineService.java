package com.benhsoan.application.ucservice.medicine;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.medicine.CreateMedicineCommand;
import com.benhsoan.port.dto.result.MedicineResult;
import com.benhsoan.port.inbound.medicine.CreateMedicineUseCase;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateMedicineService implements CreateMedicineUseCase {

    private final MedicineRepository medicineRepository;

    private final MedicineManagementAuthorizer authorizer;

    private final MedicineResultMapper resultMapper;

    private final ClockPort clockPort;

    private final AdminOperationAuditService adminOperationAuditService;

    private final CurrentUserPort currentUserPort;

    @Override
    public MedicineResult create(CreateMedicineCommand command) {
        requireCommand(command);
        authorizer.requirePharmacist();

        Instant now = clockPort.now();
        Medicine medicine = Medicine.create(
                UUID.randomUUID(),
                command.medicineCode(),
                command.medicineName(),
                command.activeIngredient(),
                command.strength(),
                command.dosageForm(),
                command.unit(),
                command.defaultRoute(),
                command.minStockThreshold(),
                command.controlled(),
                now
        );
        validateUniqueness(medicine);

        Medicine saved = medicineRepository.save(medicine);
        adminOperationAuditService.record(
                currentUserPort.getCurrentUserId(),
                ActionType.CREATE,
                ResourceType.MEDICINE,
                saved.getId(),
                null,
                AdminOperationAuditService.fields(
                        "medicineCode", saved.getMedicineCode(),
                        "medicineName", saved.getMedicineName(),
                        "activeIngredient", saved.getActiveIngredient(),
                        "strength", saved.getStrength(),
                        "dosageForm", saved.getDosageForm(),
                        "unit", saved.getUnit(),
                        "defaultRoute", saved.getDefaultRoute(),
                        "minStockThreshold", saved.getMinStockThreshold(),
                        "controlled", saved.isControlled()),
                now
        );

        return resultMapper.toResult(saved);
    }

    private void validateUniqueness(Medicine medicine) {
        String medicineCode = normalize(medicine.getMedicineCode());
        if (medicineRepository.existsByMedicineCode(medicineCode)) {
            throw new ValidationException("Medicine code already exists.");
        }

        String medicineName = normalize(medicine.getMedicineName());
        String activeIngredient = normalize(medicine.getActiveIngredient());
        if (medicineRepository.existsByMedicineNameAndActiveIngredient(
                medicineName,
                activeIngredient,
                null
        )) {
            throw new ValidationException(
                    "Medicine name and active ingredient already exist."
            );
        }
    }

    private static void requireCommand(CreateMedicineCommand command) {
        if (command == null) {
            throw new ValidationException("Create medicine command is required.");
        }
    }

    private static String normalize(String value) {
        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
