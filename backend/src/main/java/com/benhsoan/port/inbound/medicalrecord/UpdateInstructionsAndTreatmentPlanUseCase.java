package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.command.medicalrecord.UpdateInstructionsAndTreatmentPlanCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;

public interface UpdateInstructionsAndTreatmentPlanUseCase {
    MedicalRecordResult updateInstructionsAndTreatmentPlan(UUID medicalRecordId, UpdateInstructionsAndTreatmentPlanCommand command);
}
