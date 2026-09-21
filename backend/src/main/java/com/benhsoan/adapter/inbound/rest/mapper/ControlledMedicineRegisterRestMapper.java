package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.controlledmedicine.ControlledMedicineRegisterResponse;
import com.benhsoan.port.dto.result.ControlledMedicineRegisterResult;

@Component
public class ControlledMedicineRegisterRestMapper {

    public ControlledMedicineRegisterResponse toResponse(ControlledMedicineRegisterResult result) {
        return new ControlledMedicineRegisterResponse(
                result.id(),
                result.prescriptionId(),
                result.prescriptionItemId(),
                result.medicineId(),
                result.medicineName(),
                result.patientId(),
                result.patientCode(),
                result.patientName(),
                result.prescribedBy(),
                result.doctorName(),
                result.dispensedBy(),
                result.pharmacistName(),
                result.quantity(),
                result.dispensedAt()
        );
    }

    public Page<ControlledMedicineRegisterResponse> toResponse(
            Page<ControlledMedicineRegisterResult> resultPage
    ) {
        return resultPage.map(this::toResponse);
    }
}
