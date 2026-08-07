package com.benhsoan.application.ucservice.medicine;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.medicine.UpdateMedicineCommand;
import com.benhsoan.port.dto.result.MedicineResult;
import com.benhsoan.port.inbound.medicine.UpdateMedicineUseCase;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMedicineService implements UpdateMedicineUseCase {

    private final MedicineRepository medicineRepository;

    private final MedicineManagementAuthorizer authorizer;

    private final MedicineResultMapper resultMapper;

    private final ClockPort clockPort;

    @Override
    public MedicineResult update(UpdateMedicineCommand command) {
        requireCommand(command);
        authorizer.requirePharmacist();

        Medicine medicine = medicineRepository.findById(command.medicineId())
                .orElseThrow(() -> new ValidationException(
                        "Medicine not found: " + command.medicineId()
                ));

        medicine.updateInformation(
                command.medicineName(),
                command.activeIngredient(),
                command.strength(),
                command.dosageForm(),
                command.unit(),
                command.defaultRoute(),
                clockPort.now()
        );
        validateUniqueness(medicine);

        return resultMapper.toResult(medicineRepository.save(medicine));
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
