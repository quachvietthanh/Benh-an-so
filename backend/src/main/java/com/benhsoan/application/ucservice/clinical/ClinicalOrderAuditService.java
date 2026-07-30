package com.benhsoan.application.ucservice.clinical;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.port.outbound.repository.logRepository.MedicalRecordAccessLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicalOrderAuditService {

    private final MedicalRecordAccessLogRepository medicalRecordAccessLogRepository;

    public void recordCreated(UUID patientId, UUID visitId, UUID medicalRecordId, UUID actorId, Instant createdAt) {
        medicalRecordAccessLogRepository.save(MedicalRecordAccessLog.createRecordAccess(
                patientId, visitId, medicalRecordId, actorId, MedicalRecordAccessAction.CREATE,
                "Clinical order created", createdAt
        ));
    }
}
