package com.benhsoan.port.inbound.patient;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.patient.GetPatientMedicalHistoryQuery;
import com.benhsoan.port.dto.result.MedicalHistoryItemResult;

public interface ViewPatientMedicalHistoryUseCase {

    Page<MedicalHistoryItemResult> viewMedicalHistory(GetPatientMedicalHistoryQuery query);
}
