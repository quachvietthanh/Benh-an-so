package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.prescription.CheckDrugInteractionRequest;
import com.benhsoan.adapter.inbound.rest.request.prescription.CreatePrescriptionItemRequest;
import com.benhsoan.adapter.inbound.rest.request.prescription.CreatePrescriptionRequest;
import com.benhsoan.adapter.inbound.rest.request.prescription.PrescriptionInteractionOverrideRequest;
import com.benhsoan.adapter.inbound.rest.response.prescription.DrugInteractionWarningResponse;
import com.benhsoan.adapter.inbound.rest.response.prescription.PrescriptionItemResponse;
import com.benhsoan.adapter.inbound.rest.response.prescription.PrescriptionResponse;
import com.benhsoan.adapter.inbound.rest.response.prescription.PrescriptionWarningResponse;
import com.benhsoan.port.dto.command.prescription.CheckDrugInteractionCommand;
import com.benhsoan.port.dto.command.prescription.CreatePrescriptionCommand;
import com.benhsoan.port.dto.command.prescription.CreatePrescriptionItemCommand;
import com.benhsoan.port.dto.command.prescription.PrescriptionInteractionOverrideCommand;
import com.benhsoan.port.dto.result.DrugInteractionWarningResult;
import com.benhsoan.port.dto.result.PrescriptionItemResult;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.dto.result.PrescriptionWarningResult;

@Component
public class PrescriptionRestMapper {

    public CreatePrescriptionCommand toCommand(
            CreatePrescriptionRequest request
    ) {
        List<PrescriptionInteractionOverrideCommand> interactionOverrides
                = request.interactionOverrides() == null
                        ? List.of()
                        : request.interactionOverrides()
                                .stream()
                                .map(this::toCommand)
                                .toList();

        return CreatePrescriptionCommand.builder()
                .medicalRecordId(request.medicalRecordId())
                .note(request.note())
                .items(request.items()
                        .stream()
                        .map(this::toCommand)
                        .toList())
                .interactionOverrides(interactionOverrides)
                .build();
    }

    public CheckDrugInteractionCommand toCommand(
            CheckDrugInteractionRequest request
    ) {
        return new CheckDrugInteractionCommand(request.drugIds());
    }

    public PrescriptionResponse toResponse(
            PrescriptionResult result
    ) {
        return PrescriptionResponse.builder()
                .id(result.id())
                .prescriptionCode(result.prescriptionCode())
                .medicalRecordId(result.medicalRecordId())
                .status(result.status())
                .note(result.note())
                .prescribedBy(result.prescribedBy())
                .prescribedAt(result.prescribedAt())
                .updatedBy(result.updatedBy())
                .updatedAt(result.updatedAt())
                .items(result.items()
                        .stream()
                        .map(this::toResponse)
                        .toList())
                .warnings(result.warnings()
                        .stream()
                        .map(this::toResponse)
                        .toList())
                .build();
    }

    public List<DrugInteractionWarningResponse> toResponse(
            List<DrugInteractionWarningResult> results
    ) {
        return results.stream().map(this::toResponse).toList();
    }

    private CreatePrescriptionItemCommand toCommand(
            CreatePrescriptionItemRequest request
    ) {
        return CreatePrescriptionItemCommand.builder()
                .medicineId(request.medicineId())
                .dosage(request.dosage())
                .frequency(request.frequency())
                .route(request.route())
                .durationDays(request.durationDays())
                .quantity(request.quantity())
                .instructions(request.instructions())
                .build();
    }

    private PrescriptionInteractionOverrideCommand toCommand(
            PrescriptionInteractionOverrideRequest request
    ) {
        return PrescriptionInteractionOverrideCommand.builder()
                .drugInteractionId(request.drugInteractionId())
                .overrideReason(request.overrideReason())
                .build();
    }

    private PrescriptionItemResponse toResponse(
            PrescriptionItemResult result
    ) {
        return PrescriptionItemResponse.builder()
                .id(result.id())
                .prescriptionId(result.prescriptionId())
                .medicineId(result.medicineId())
                .medicineName(result.medicineName())
                .activeIngredient(result.activeIngredient())
                .strength(result.strength())
                .unit(result.unit())
                .dosage(result.dosage())
                .frequency(result.frequency())
                .route(result.route())
                .durationDays(result.durationDays())
                .quantity(result.quantity())
                .instructions(result.instructions())
                .createdAt(result.createdAt())
                .updatedAt(result.updatedAt())
                .build();
    }

    private PrescriptionWarningResponse toResponse(
            PrescriptionWarningResult result
    ) {
        return PrescriptionWarningResponse.builder()
                .id(result.id())
                .drugInteractionId(result.drugInteractionId())
                .firstMedicineId(result.firstMedicineId())
                .secondMedicineId(result.secondMedicineId())
                .severity(result.severity())
                .warningMessage(result.warningMessage())
                .action(result.action())
                .overrideReason(result.overrideReason())
                .handledBy(result.handledBy())
                .handledAt(result.handledAt())
                .build();
    }

    private DrugInteractionWarningResponse toResponse(
            DrugInteractionWarningResult result
    ) {
        return new DrugInteractionWarningResponse(
                result.drugIdA(),
                result.drugIdB(),
                result.severity(),
                result.description(),
                result.clinicalRecommendation()
        );
    }
}
