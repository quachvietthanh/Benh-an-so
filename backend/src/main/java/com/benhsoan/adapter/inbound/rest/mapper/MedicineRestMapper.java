package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.medicine.CreateMedicineRequest;
import com.benhsoan.adapter.inbound.rest.request.medicine.UpdateMedicineRequest;
import com.benhsoan.adapter.inbound.rest.response.medicine.MedicineResponse;
import com.benhsoan.port.dto.command.medicine.CreateMedicineCommand;
import com.benhsoan.port.dto.command.medicine.UpdateMedicineCommand;
import com.benhsoan.port.dto.result.MedicineResult;

@Component
public class MedicineRestMapper {

    public CreateMedicineCommand toCommand(CreateMedicineRequest request) {
        return new CreateMedicineCommand(
                request.medicineCode(),
                request.medicineName(),
                request.activeIngredient(),
                request.strength(),
                request.dosageForm(),
                request.unit(),
                request.defaultRoute(),
                request.minStockThreshold()
        );
    }

    public UpdateMedicineCommand toCommand(
            UUID medicineId,
            UpdateMedicineRequest request
    ) {
        return new UpdateMedicineCommand(
                medicineId,
                request.medicineName(),
                request.activeIngredient(),
                request.strength(),
                request.dosageForm(),
                request.unit(),
                request.defaultRoute(),
                request.minStockThreshold()
        );
    }

    public MedicineResponse toResponse(MedicineResult result) {
        return new MedicineResponse(
                result.id(),
                result.medicineCode(),
                result.medicineName(),
                result.activeIngredient(),
                result.strength(),
                result.dosageForm(),
                result.unit(),
                result.defaultRoute(),
                result.active(),
                result.createdAt(),
                result.updatedAt(),
                result.stockQuantity(),
                result.minStockThreshold()
        );
    }

    public Page<MedicineResponse> toResponse(Page<MedicineResult> resultPage) {
        return resultPage.map(this::toResponse);
    }
}
