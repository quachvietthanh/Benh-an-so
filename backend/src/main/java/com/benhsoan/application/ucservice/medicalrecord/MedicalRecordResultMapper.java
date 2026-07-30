package com.benhsoan.application.ucservice.medicalrecord;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.MedicalRecordAmendment;
import com.benhsoan.port.dto.result.MedicalRecordAccessLogResult;
import com.benhsoan.port.dto.result.MedicalRecordAmendmentResult;
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
}
