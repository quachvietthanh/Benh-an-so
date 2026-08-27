package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.patient.PatientMedicalHistoryDetailResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientMedicalHistoryDetailResponse.DiagnosisResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientMedicalHistoryDetailResponse.PrescriptionItemResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientMedicalHistorySummaryResponse;
import com.benhsoan.port.dto.result.patient.PatientMedicalHistoryDetailResult;
import com.benhsoan.port.dto.result.patient.PatientMedicalHistorySummaryResult;

@Component
public class PatientPortalMedicalHistoryRestMapper {

    public PatientMedicalHistorySummaryResponse toResponse(PatientMedicalHistorySummaryResult result) {
        return new PatientMedicalHistorySummaryResponse(
                result.visitId(),
                result.visitAt(),
                result.doctorName(),
                result.specialtyName(),
                result.diagnosisSummary(),
                result.prescriptionCount()
        );
    }

    public PatientMedicalHistoryDetailResponse toResponse(PatientMedicalHistoryDetailResult result) {
        return new PatientMedicalHistoryDetailResponse(
                result.visitId(),
                result.visitAt(),
                result.doctorName(),
                result.specialtyName(),
                toDiagnoses(result.diagnoses()),
                toItems(result.prescriptionItems()),
                result.doctorAdvice()
        );
    }

    private List<DiagnosisResponse> toDiagnoses(List<PatientMedicalHistoryDetailResult.DiagnosisItem> diagnoses) {
        return diagnoses.stream()
                .map(d -> new DiagnosisResponse(d.icd10Code(), d.name()))
                .toList();
    }

    private List<PrescriptionItemResponse> toItems(List<PatientMedicalHistoryDetailResult.PrescriptionItemView> items) {
        return items.stream()
                .map(i -> new PrescriptionItemResponse(
                        i.medicineName(),
                        i.quantity(),
                        i.dosage(),
                        i.instructions()))
                .toList();
    }
}
