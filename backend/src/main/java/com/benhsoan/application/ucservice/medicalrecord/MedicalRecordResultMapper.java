package com.benhsoan.application.ucservice.medicalrecord;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.MedicalRecordAmendment;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.MedicalRecordAccessLogResult;
import com.benhsoan.port.dto.result.MedicalRecordAmendmentResult;
import com.benhsoan.port.dto.result.MedicalRecordDetailResult;
import com.benhsoan.port.dto.result.MedicalRecordDetailResult.PatientInfo;
import com.benhsoan.port.dto.result.MedicalRecordDetailResult.VisitInfo;
import com.benhsoan.port.dto.result.MedicalRecordDiagnosisResult;
import com.benhsoan.port.dto.result.MedicalRecordResult;

@Component
public class MedicalRecordResultMapper {

    public MedicalRecordResult toResult(MedicalRecord record) {
        return new MedicalRecordResult(
                record.getId(), record.getVisitId(), record.getChiefComplaint(), record.getSymptoms(),
                record.getMedicalHistory(), record.getPhysicalExamination(), record.getClinicalProgress(),
                record.getTreatmentPlan(), record.getDoctorInstructions(), record.getConclusion(),
                record.getStatus(), record.getLockedAt(), record.getLockedBy(), record.getCreatedBy(),
                record.getCreatedAt(), record.getUpdatedBy(), record.getUpdatedAt()
        );
    }

    public MedicalRecordAmendmentResult toResult(MedicalRecordAmendment amendment) {
        return new MedicalRecordAmendmentResult(
                amendment.getId(), amendment.getMedicalRecordId(), amendment.getContent(),
                amendment.getReason(), amendment.getAmendedBy(), amendment.getAmendedAt()
        );
    }

    public MedicalRecordAccessLogResult toResult(MedicalRecordAccessLog accessLog) {
        return new MedicalRecordAccessLogResult(
                accessLog.getId(), accessLog.getPatientId(), accessLog.getVisitId(),
                accessLog.getMedicalRecordId(), accessLog.getAccessedBy(), accessLog.getAction(),
                accessLog.getDetail(), accessLog.getAccessedAt()
        );
    }

    public MedicalRecordDetailResult toDetailResult(MedicalRecord record, Visit visit, Patient patient,
            User doctor, List<MedicalRecordDiagnosis> diagnoses) {
        List<MedicalRecordDiagnosisResult> diagnosisResults = diagnoses.stream()
                .map(d -> new MedicalRecordDiagnosisResult(
                        d.getId(), d.getMedicalRecordId(), d.getDiagnosisCode(), d.getDiagnosisName(),
                        d.getDiagnosisType(), d.getNote(), d.getDiagnosedBy(), d.getDiagnosedAt()))
                .toList();

        MedicalRecordDiagnosis primary = diagnoses.stream()
                .filter(d -> d.getDiagnosisType() == DiagnosisType.PRIMARY)
                .findFirst()
                .orElse(null);

        List<String> secondaryIcdCodes = diagnoses.stream()
                .filter(d -> d.getDiagnosisType() == DiagnosisType.SECONDARY)
                .map(MedicalRecordDiagnosis::getDiagnosisCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();

        PatientInfo patientInfo = new PatientInfo(
                patient.getId(), patient.getPatientCode(), patient.getFullName(), patient.getDateOfBirth(),
                patient.getGender(), patient.getPhone(), patient.getIdentityNumber(), patient.getInsuranceNumber());

        VisitInfo visitInfo = new VisitInfo(
                visit.getId(), visit.getVisitCode(), visit.getVisitType(), visit.getStatus(),
                visit.getVisitAt(), visit.getStartedAt(), visit.getCompletedAt(),
                visit.getReason(), visit.getNote(), visit.getDoctorId(), doctor.getFullName());

        return new MedicalRecordDetailResult(
                patientInfo, visitInfo,
                record.getId(), record.getChiefComplaint(), record.getSymptoms(),
                record.getMedicalHistory(), record.getPhysicalExamination(), record.getClinicalProgress(),
                record.getTreatmentPlan(), record.getDoctorInstructions(), record.getConclusion(),
                record.getStatus(), record.getLockedAt(), record.getLockedBy(),
                primary == null ? null : primary.getDiagnosisCode(),
                primary == null ? null : primary.getDiagnosisName(),
                secondaryIcdCodes,
                diagnosisResults
        );
    }
}
