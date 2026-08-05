package com.benhsoan.application.ucservice.clinical;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAccessLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicalResultAuditService {

    private final MedicalRecordAccessLogRepository repository;

    public void recordWrite(UUID clinicalResultId, UUID patientId, UUID visitId, UUID recordId, UUID actor,
            MedicalRecordAccessAction action, Instant at) {
        save(clinicalResultId, patientId, visitId, recordId, actor, action, at);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordView(UUID clinicalResultId, UUID patientId, UUID visitId, UUID recordId, UUID actor,
            MedicalRecordAccessAction action, Instant at) {
        save(clinicalResultId, patientId, visitId, recordId, actor, action, at);
    }

    private void save(UUID clinicalResultId, UUID patientId, UUID visitId, UUID recordId, UUID actor,
            MedicalRecordAccessAction action, Instant at) {
        repository.save(MedicalRecordAccessLog.createRecordAccess(
                patientId, visitId, recordId, actor, action,
                "Clinical result " + action.name().toLowerCase() + ": " + clinicalResultId, at));
    }
}
