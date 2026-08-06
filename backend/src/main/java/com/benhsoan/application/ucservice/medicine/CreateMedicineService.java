package com.benhsoan.application.ucservice.medicine;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.medicine.CreateMedicineCommand;
import com.benhsoan.port.dto.result.MedicineResult;
import com.benhsoan.port.inbound.medicine.CreateMedicineUseCase;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
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
                now
        );
        validateUniqueness(medicine);

        return resultMapper.toResult(medicineRepository.save(medicine));
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
