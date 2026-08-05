package com.benhsoan.port.outbound.repository.patient;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.patient.GetPatientMedicalHistoryQuery;
import com.benhsoan.port.dto.result.MedicalHistoryItemResult;

public interface PatientMedicalHistoryQueryPort {

    Page<MedicalHistoryItemResult> findMedicalHistory(GetPatientMedicalHistoryQuery query);
}
