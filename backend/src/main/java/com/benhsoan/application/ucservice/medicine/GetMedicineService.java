package com.benhsoan.application.ucservice.medicine;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.MedicineResult;
import com.benhsoan.port.inbound.medicine.GetMedicineUseCase;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMedicineService implements GetMedicineUseCase {

    private final MedicineRepository medicineRepository;

    private final MedicineManagementAuthorizer authorizer;

    private final MedicineResultMapper resultMapper;

    @Override
    public MedicineResult getById(UUID medicineId) {
        if (medicineId == null) {
            throw new ValidationException("Medicine id is required.");
        }
        authorizer.requirePharmacist();

        return medicineRepository.findById(medicineId)
                .map(resultMapper::toResult)
                .orElseThrow(() -> new ValidationException(
                        "Medicine not found: " + medicineId
                ));
    }
}
