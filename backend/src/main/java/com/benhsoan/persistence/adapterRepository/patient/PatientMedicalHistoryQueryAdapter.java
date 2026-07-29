package com.benhsoan.persistence.adapterRepository.patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.benhsoan.persistence.jpaRepository.patient.JpaPatientMedicalHistoryRepository;
import com.benhsoan.persistence.jpaRepository.patient.PatientMedicalHistoryProjection;
import com.benhsoan.port.dto.command.patient.GetPatientMedicalHistoryQuery;
import com.benhsoan.port.dto.result.MedicalHistoryItemResult;
import com.benhsoan.port.outbound.repository.queryRepository.patient.PatientMedicalHistoryQueryPort;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PatientMedicalHistoryQueryAdapter implements PatientMedicalHistoryQueryPort {

    private final JpaPatientMedicalHistoryRepository jpaRepository;

    @Override
    public Page<MedicalHistoryItemResult> findMedicalHistory(GetPatientMedicalHistoryQuery query) {
        return jpaRepository.findMedicalHistory(
                query.patientId(),
                query.from(),
                query.to(),
                PageRequest.of(query.page(), query.size())
        ).map(this::toResult);
    }

    private MedicalHistoryItemResult toResult(PatientMedicalHistoryProjection projection) {
        return new MedicalHistoryItemResult(
                projection.visitId(),
                projection.visitCode(),
                projection.visitType(),
                projection.visitStatus(),
                projection.visitAt(),
                projection.startedAt(),
                projection.completedAt(),
                projection.reason(),
                projection.note(),
                projection.doctorId(),
                projection.doctorName(),
                projection.medicalRecordId(),
                projection.medicalRecordStatus(),
                projection.chiefComplaint(),
                projection.conclusion()
        );
    }
}
