package com.benhsoan.application.ucservice.medicalrecord;

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
public class MedicalRecordAccessAuditService {

    private static final String HISTORY_VIEW_DETAIL = "Patient medical history viewed";

    private final MedicalRecordAccessLogRepository medicalRecordAccessLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordHistoryView(UUID patientId, UUID accessedBy, Instant accessedAt) {
        medicalRecordAccessLogRepository.save(MedicalRecordAccessLog.createHistoryView(
                patientId,
                accessedBy,
                HISTORY_VIEW_DETAIL,
                accessedAt
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRecordView(UUID patientId, UUID visitId, UUID medicalRecordId, UUID accessedBy, Instant accessedAt) {
        recordRecordAccess(patientId, visitId, medicalRecordId, accessedBy, MedicalRecordAccessAction.VIEW, "Medical record viewed", accessedAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRecordAccess(UUID patientId, UUID visitId, UUID medicalRecordId, UUID accessedBy, MedicalRecordAccessAction action, String detail, Instant accessedAt) {
        medicalRecordAccessLogRepository.save(MedicalRecordAccessLog.createRecordAccess(
                patientId, visitId, medicalRecordId, accessedBy, action, detail, accessedAt
        ));
    }

    @Transactional
    public void recordRecordAccessInCurrentTransaction(
            UUID patientId,
            UUID visitId,
            UUID medicalRecordId,
            UUID accessedBy,
            MedicalRecordAccessAction action,
            String detail,
            Instant accessedAt
    ) {
        medicalRecordAccessLogRepository.save(MedicalRecordAccessLog.createRecordAccess(
                patientId, visitId, medicalRecordId, accessedBy, action, detail, accessedAt
        ));
    }
}
