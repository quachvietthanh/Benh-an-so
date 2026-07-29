package com.benhsoan.application.ucservice.queries.medicalrecord;

import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.patient.MedicalRecordAccessLog;
import com.benhsoan.port.outbound.repository.logRepository.MedicalRecordAccessLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalRecordAuditService {

    private final MedicalRecordAccessLogRepository accessLogRepository;

    /**
     * Asynchronously logs that a user viewed a patient's medical history.
     * Runs in a separate transaction to avoid blocking / rolling back
     * the main query on failure.
     */
    @Async
    @Transactional
    public void logViewHistory(
            UUID patientId,
            UUID performedByUserId
    ) {

        try {
            MedicalRecordAccessLog auditLog = MedicalRecordAccessLog
                    .createViewHistoryLog(
                            patientId,
                            performedByUserId,
                            null
                    );

            accessLogRepository.save(auditLog);

            log.debug(
                    "Audit log: user={} viewed medical history of patient={}",
                    performedByUserId, patientId
            );

        } catch (Exception e) {
            // Never let audit logging break the main flow
            log.error(
                    "Failed to write audit log for user={} patient={}: {}",
                    performedByUserId, patientId, e.getMessage()
            );
        }
    }

    /**
     * Asynchronously logs that access was denied for a medical history view.
     */
    @Async
    @Transactional
    public void logAccessDenied(
            UUID patientId,
            UUID performedByUserId,
            String reason
    ) {

        try {
            MedicalRecordAccessLog auditLog = MedicalRecordAccessLog
                    .createViewHistoryLog(
                            patientId,
                            performedByUserId,
                            null
                    );

            accessLogRepository.save(auditLog);

            log.warn(
                    "Access denied logged: user={} patient={} reason={}",
                    performedByUserId, patientId, reason
            );

        } catch (Exception e) {
            log.error(
                    "Failed to write access-denied audit log: {}",
                    e.getMessage()
            );
        }
    }
}
