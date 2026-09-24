package com.benhsoan.application.ucservice.inventory;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.inventory.procurement.exception.ProcurementPlanNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.ProcurementPlanResult;
import com.benhsoan.port.inbound.inventory.GetMedicationProcurementPlanUseCase;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementPlanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMedicationProcurementPlanService implements GetMedicationProcurementPlanUseCase {

    private final MedicationProcurementPlanRepository planRepository;
    private final MedicationProcurementResultMapper resultMapper;
    private final MedicationProcurementAuthorizer authorizer;

    @Override
    public ProcurementPlanResult getById(UUID id) {
        authorizer.requireReadPermission();

        if (id == null) {
            throw new ValidationException("Mã định danh phiếu dự trù không được để trống.");
        }
        return planRepository.findById(id)
                .map(resultMapper::toPlanResult)
                .orElseThrow(() -> new ProcurementPlanNotFoundException(id));
    }

    @Override
    public ProcurementPlanResult getByPlanCode(String planCode) {
        authorizer.requireReadPermission();

        if (planCode == null || planCode.isBlank()) {
            throw new ValidationException("Mã phiếu dự trù mua thuốc không được để trống.");
        }
        return planRepository.findByPlanCode(planCode)
                .map(resultMapper::toPlanResult)
                .orElseThrow(() -> new ProcurementPlanNotFoundException(planCode));
    }
}
