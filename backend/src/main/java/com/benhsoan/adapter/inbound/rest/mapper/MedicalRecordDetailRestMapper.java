package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordDetailResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordDetailResponse.PatientInfo;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordDetailResponse.VisitInfo;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordDiagnosisResponse;
import com.benhsoan.port.dto.result.MedicalRecordDetailResult;
import com.benhsoan.port.dto.result.MedicalRecordDiagnosisResult;

@Component
public class MedicalRecordDetailRestMapper {

    public MedicalRecordDetailResponse toResponse(MedicalRecordDetailResult result) {
        PatientInfo patient = new PatientInfo(
                result.patient().id(), result.patient().patientCode(), result.patient().fullName(),
                result.patient().dateOfBirth(), result.patient().gender(), result.patient().phone(),
                result.patient().identityNumber(), result.patient().insuranceNumber());

        VisitInfo visit = new VisitInfo(
                result.visit().id(), result.visit().visitCode(), result.visit().visitType(),
                result.visit().status(), result.visit().visitAt(), result.visit().startedAt(),
                result.visit().completedAt(), result.visit().reason(), result.visit().note(),
                result.visit().doctorId(), result.visit().doctorName());

        List<MedicalRecordDiagnosisResponse> diagnoses = result.diagnoses().stream()
                .map(this::toDiagnosisResponse)
                .toList();

        return new MedicalRecordDetailResponse(
                patient, visit,
                result.medicalRecordId(), result.chiefComplaint(), result.symptoms(),
                result.medicalHistory(), result.physicalExamination(), result.clinicalProgress(),
                result.treatmentPlan(), result.doctorInstructions(), result.conclusion(),
                result.status(), result.signatureData(), result.signedAt(), result.signedBy(),
                result.lockedAt(), result.lockedBy(),
                result.primaryIcdCode(), result.primaryIcdName(), result.secondaryIcdCodes(),
                diagnoses
        );
    }

    public List<MedicalRecordDetailResponse> toResponses(List<MedicalRecordDetailResult> results) {
        return results.stream().map(this::toResponse).toList();
    }

    private MedicalRecordDiagnosisResponse toDiagnosisResponse(MedicalRecordDiagnosisResult d) {
        return new MedicalRecordDiagnosisResponse(
                d.id(), d.medicalRecordId(), d.diagnosisCode(), d.diagnosisName(),
                d.diagnosisType(), d.note(), d.diagnosedBy(), d.diagnosedAt());
    }
}
