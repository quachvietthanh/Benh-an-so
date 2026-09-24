package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.medicalrecord.ArchivedMedicalRecordResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.BatchArchiveMedicalRecordResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.EligibleForArchiveMedicalRecordResponse;
import com.benhsoan.port.dto.result.ArchiveEligibleMedicalRecordResult;
import com.benhsoan.port.dto.result.ArchivedMedicalRecordResult;
import com.benhsoan.port.dto.result.BatchArchiveMedicalRecordResult;

@Component
public class ArchiveMedicalRecordRestMapper {

    public EligibleForArchiveMedicalRecordResponse toEligibleResponse(ArchiveEligibleMedicalRecordResult result) {
        if (result == null) {
            return null;
        }
        return new EligibleForArchiveMedicalRecordResponse(
                result.getMedicalRecordId(),
                result.getVisitId(),
                result.getVisitCode(),
                result.getPatientId(),
                result.getPatientCode(),
                result.getPatientFullName(),
                result.getDoctorId(),
                result.getDoctorFullName(),
                result.getSpecialtyName(),
                result.getStatus(),
                result.getCompletedAt(),
                result.getSignedAt()
        );
    }

    public BatchArchiveMedicalRecordResponse toBatchResponse(BatchArchiveMedicalRecordResult result) {
        if (result == null) {
            return null;
        }
        return new BatchArchiveMedicalRecordResponse(
                result.getTotalRequested(),
                result.getArchivedCount(),
                result.getSkippedCount(),
                result.getArchivedMedicalRecordIds(),
                result.getSkippedMedicalRecordIds()
        );
    }

    public ArchivedMedicalRecordResponse toArchivedResponse(ArchivedMedicalRecordResult result) {
        if (result == null) {
            return null;
        }
        return new ArchivedMedicalRecordResponse(
                result.getMedicalRecordId(),
                result.getVisitId(),
                result.getVisitCode(),
                result.getPatientId(),
                result.getPatientCode(),
                result.getPatientFullName(),
                result.getPatientPhone(),
                result.getDoctorId(),
                result.getDoctorFullName(),
                result.getConclusion(),
                result.getRevisitDate(),
                result.getStatus(),
                result.getCompletedAt(),
                result.getSignedAt(),
                result.getArchivedAt(),
                result.getArchivedBy()
        );
    }
}
