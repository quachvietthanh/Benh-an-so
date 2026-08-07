package com.benhsoan.application.ucservice.medicine;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.MedicineResult;
import com.benhsoan.port.inbound.medicine.DeactivateMedicineUseCase;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
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
        }

        return resultMapper.toResult(medicine);
    }

    private static void requireMedicineId(UUID medicineId) {
        if (medicineId == null) {
            throw new ValidationException("Medicine id is required.");
        }
    }
}
